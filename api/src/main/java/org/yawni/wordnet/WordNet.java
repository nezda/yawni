/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.yawni.wordnet;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.ImmutableMap;
import static com.google.common.collect.Iterables.transform;
import static com.google.common.collect.Iterables.concat;
import com.google.common.collect.Maps;
import com.google.common.io.Closeables;

import java.io.BufferedInputStream;
import org.yawni.util.cache.Cache;
import static org.yawni.util.MergedIterable.merge;
import static org.yawni.util.Utils.uniq;
import org.yawni.util.CharSequences;
import org.yawni.util.LightImmutableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.yawni.util.EnumAliases;
import org.yawni.util.StringTokenizer;
import org.yawni.util.cache.BloomFilter;
import org.yawni.util.cache.Caches;
import org.yawni.wordnet.WordSense.AdjPosition;

/**
 * An implementation of {@code WordNetInterface} that retrieves objects from the text files in the WordNet distribution
 * directory (typically <tt><em>$WNHOME</em>/dict/</tt>), or from a properly organized jar file containing it;
 * typical users will use {@link WordNet#getInstance()} to get the canonical instance of this
 * class.
 *
 * <p> A {@code WordNet} has an <em>entity cache</em>.  The entity cache is used to resolve multiple
 * temporally contiguous lookups of the same entity to the same object -- for example, successive
 * calls to {@link #lookupWord(CharSequence, POS)} with the same parameters would return the same value
 * ({@code ==} as well as {@code equals}), as would traversal of two {@link Relation}s
 * that shared the same target.  Under memory pressure, it is possible for
 * two different ({@code !=}, but still {@code equals}) objects to represent the same entity,
 * if their retrieval is separated by other database operations.
 *
 * @see WordNetInterface
 * @see Cache
 */
public final class WordNet implements WordNetInterface {
  private static final Logger log = LoggerFactory.getLogger(WordNet.class);

  private final FileManagerInterface fileManager;
  final Morphy morphy;

  //
  // Constructors
  //

  /**
   * Construct a {@link WordNetInterface} that retrieves file data from {@code fileManager}.
   */
  private WordNet(final FileManagerInterface fileManager) {
    this.fileManager = fileManager;
    this.morphy = new Morphy(this);
  }

  /**
   * Construct a dictionary backed by a set of files contained in the default
   * WordNet search directory.
   * See {@link FileManager} for a description of the location of the default
   * WordNet search directory ({@code $WNSEARCHDIR}).
   */
  WordNet() {
    this(new FileManager());
  }

//  /**
//   * Construct a dictionary backed by a set of files contained in
//   * {@code search directory}.
//   */
//  WordNet(final String searchDirectory) {
//    this(new FileManager(searchDirectory));
//  }

  // thread-safe singleton trick from:
  // http://tech.puredanger.com/2007/06/15/double-checked-locking/
  private static final class InstanceHolder {
    /** singleton reference */
    static final WordNet instance = new WordNet();
  } // end class InstanceHolder

  /**
   * Factory method to get <em>the</em> dictionary backed by a set of files contained
   * in the default WordNet search directory.
   * See {@link FileManager} for a description of the location of the default
   * WordNet search directory ({@code $WNSEARCHDIR}).
   */
  public static WordNet getInstance() {
    return InstanceHolder.instance;
  }

//  /**
//   * Factory method to get <em>the</em> dictionary backed by a set of files contained
//   * in {@code searchDirectory}.
//   */
//  //FIXME ignores passed in searchDirectory reference
//  public static WordNet getInstance(final String searchDirectory) {
//    return InstanceHolder.instance;
//  }

//  /**
//   * Factory method to get <em>the</em> {@link WordNetInterface} that retrieves file data from
//   * {@code fileManager}.  A client can use this to create a
//   * {@link WordNetInterface} backed by a {@link RemoteFileManager}.
//   * @see RemoteFileManager
//   */
//  //FIXME ignores passed in fileManager reference
//  public static WordNet getInstance(final FileManagerInterface fileManager) {
//    return InstanceHolder.instance;
//  }

  //
  // Entity lookup caching
  //
  static final int DEFAULT_CACHE_CAPACITY = 10000;//100000;
  private final Cache<DatabaseKey, Object> synsetCache = Caches.withCapacity(DEFAULT_CACHE_CAPACITY);
  // single cache which uses 2 kinds kinds of keys (keeps utilization high)
  // - POSOffsetDatabaseKey (getIndexWordAt direct-hit cache) and StringPOSDatabaseKey (lookupWord query cache)
  private final Cache<DatabaseKey, Object> indexWordCache = Caches.withCapacity(DEFAULT_CACHE_CAPACITY);

  // generic custom hashing interface
  interface DatabaseKey {
    @Override
    int hashCode();
    @Override
    boolean equals(Object that);
  } // end interface DatabaseKey

  static class POSOffsetDatabaseKey implements DatabaseKey {
    private final int offset;
    private final byte posOrdinal;
    POSOffsetDatabaseKey(final POS pos, final int offset) {
      this.offset = offset;
      this.posOrdinal = pos.getByteOrdinal();
    }
    @Override
    public boolean equals(final Object object) {
      // if indexWordCache is shared by getIndexWordAt() and lookupWord()
      // collisions can happen
      //assert object instanceof POSOffsetDatabaseKey : object;
      if (object instanceof POSOffsetDatabaseKey) {
        final POSOffsetDatabaseKey that = (POSOffsetDatabaseKey)object;
        return that.posOrdinal == this.posOrdinal && that.offset == this.offset;
      }
      return false;
    }
    @Override
    public int hashCode() {
      return (offset * 10) + posOrdinal;
    }
    @Override
    public String toString() {
      return "[POSOffsetDatabaseKey offset: "+offset+" "+POS.fromOrdinal(posOrdinal)+"]";
    }
  } // end class POSOffsetDatabaseKey

  static class StringPOSDatabaseKey implements DatabaseKey {
    private final CharSequence key;
    private final byte posOrdinal;
    StringPOSDatabaseKey(final CharSequence key, final POS pos) {
      this.key = key;
      this.posOrdinal = pos.getByteOrdinal();
    }
    @Override
    public boolean equals(final Object object) {
      // if indexWordCache is shared by getIndexWordAt() and lookupWord()
      // collisions can happen
      //assert object instanceof StringPOSDatabaseKey : object;
      if (object instanceof StringPOSDatabaseKey) {
        final StringPOSDatabaseKey that = (StringPOSDatabaseKey)object;
        return that.posOrdinal == this.posOrdinal && CharSequences.equals(that.key, this.key);
      }
      return false;
    }
    @Override
    public int hashCode() {
      return (31 * posOrdinal) + CharSequences.hashCode(key);
    }
    @Override
    public String toString() {
      return "[StringPOSDatabaseKey key: \""+key+"\" "+POS.fromOrdinal(posOrdinal)+"]";
    }
  } // end class StringPOSDatabaseKey

  //
  // File name computation
  //
  private static final Map<POS, String> POS_TO_FILENAME_ROOT = Maps.newEnumMap(ImmutableMap.of(
      POS.NOUN, "noun",
      POS.VERB, "verb",
      POS.ADJ, "adj",
      POS.SAT_ADJ, "adj",
      POS.ADV, "adv"
    ));

  private static String getDatabaseSuffixName(final POS pos) {
    final String toReturn = POS_TO_FILENAME_ROOT.get(pos);
    if (toReturn == null) {
      throw new IllegalArgumentException("no filename for pos "+pos);
    }
    //don't like the potential varargs cost
    //checkArgument(toReturn != null, "no fileName for pos %s", pos);
    return toReturn;
  }

  private static final Map<POS, String> DATA_FILE_NAMES;
  static {
    DATA_FILE_NAMES = Maps.newEnumMap(POS.class);
    for (final POS pos : POS.CATS) {
      DATA_FILE_NAMES.put(pos, "data." + getDatabaseSuffixName(pos));
    }
  }

  private static String getDataFilename(final POS pos) {
    final String toReturn = DATA_FILE_NAMES.get(pos);
    if (toReturn == null) {
      throw new IllegalArgumentException("no filename for pos "+pos);
    }
    return toReturn;
  }

  private static final Map<POS, String> INDEX_FILE_NAMES;
  static {
    INDEX_FILE_NAMES = Maps.newEnumMap(POS.class);
    for (final POS pos : POS.CATS) {
      INDEX_FILE_NAMES.put(pos, "index." + getDatabaseSuffixName(pos));
    }
  }

  private static String getIndexFileName(final POS pos) {
    final String toReturn = INDEX_FILE_NAMES.get(pos);
    if (toReturn == null) {
      throw new IllegalArgumentException("no filename for pos "+pos);
    }
    return toReturn;
  }

  private static final Map<POS, String> EXCEPTION_FILE_NAMES;
  static {
    EXCEPTION_FILE_NAMES = Maps.newEnumMap(POS.class);
    for (final POS pos : POS.CATS) {
      EXCEPTION_FILE_NAMES.put(pos, getDatabaseSuffixName(pos) + ".exc");
    }
  }

  private static String getExceptionsFilename(final POS pos) {
    final String toReturn = EXCEPTION_FILE_NAMES.get(pos);
    if (toReturn == null) {
      throw new IllegalArgumentException("no filename for pos "+pos);
    }
    return toReturn;
  }

  enum PlainTextResource {
    CNTLIST_DOT_REV("cntlist.rev", true, true),
    CORE_RANK("core-wordnet.ranked", false, false),
    // alt filenames:
    // "morphosemantic-links.xls.tsv.sensekeys.bidi"
    // "morphosemantic-links.xls.tsv.offsets.bidi"
    MORPHOSEMANTIC_RELATIONS("morphosemantic-links.xls.tsv.offsets.synsetIndexes.bidi", false, false),
    VERB_GROUP_RELATIONS("verb_groups.non_pairs.offsets", false, false),
    VERB_SENTENCES_INDEX("sentidx.vrb", true, true),
    VERB_SENTENCES("sents.vrb", true, true),
    GENERIC_VERB_FRAMES("frames.vrb", true, true);

    private final String fileName;
    private final boolean required;
    private final boolean fileNameWnRelative;
    // on 1st access, resolve Future<CharStream>
    // memoized Supplier<CharStream>

    PlainTextResource(final String fileName, final boolean required, final boolean fileNameWnRelative) {
      this.fileName = fileName;
      this.required = required;
      this.fileNameWnRelative = fileNameWnRelative;
      // detect & prevent accidental duplicates
      staticThis.ALIASES.registerAlias(this, fileName);
    }

    public String getFileName() {
      return fileName;
    }
    private static class staticThis {
      static EnumAliases<PlainTextResource> ALIASES = EnumAliases.make(PlainTextResource.class);
    }
  } // end enum PlainTextResource

  //
  // Entity retrieval
  //

  //FIXME cache's don't store null values!
  private static void cacheDebug(final Cache<DatabaseKey, Object> cache) {
    //System.err.println(cache.getClass().getSimpleName());
    //System.err.printf("getIndexWordAtCacheMiss: %d getIndexWordAtCacheHit: %d weirdGetIndexWordAtCacheMiss: %d\n",
    //    getIndexWordAtCacheMiss, getIndexWordAtCacheHit, weirdGetIndexWordAtCacheMiss );
    //System.err.printf("getSynsetAtCacheMiss: %d getSynsetAtCacheHit: %d weirdGetSynsetAtCacheMiss: %d\n",
    //    getSynsetAtCacheMiss, getSynsetAtCacheHit, weirdGetSynsetAtCacheMiss);
    //System.err.printf("lookupIndexWordCacheMiss: %d lookupIndexWordCacheHit: %d weirdLookupIndexWordCacheMiss: %d\n",
    //    lookupIndexWordCacheMiss, lookupIndexWordCacheHit, weirdLookupIndexWordCacheMiss);
  }

  static int getIndexWordAtCacheMiss = 0;
  static int getIndexWordAtCacheHit = 0;
  static int weirdGetIndexWordAtCacheMiss = 0;

  Word getIndexWordAt(final POS pos, final int offset) {
    final DatabaseKey cacheKey = new POSOffsetDatabaseKey(pos, offset);
    Word word = (Word) indexWordCache.get(cacheKey);
    if (word != null) {
      getIndexWordAtCacheHit++;
      cacheDebug(indexWordCache);
    } else {
      getIndexWordAtCacheMiss++;
      cacheDebug(indexWordCache);
      final String fileName = getIndexFileName(pos);
      final CharSequence line;
      try {
        line = fileManager.readLineAt(offset, fileName);
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
      if (line == null) {
        throw new IllegalStateException("line null for offset "+offset+" "+pos);
      }
      word = new Word(line, offset, this);
      indexWordCache.put(cacheKey, word);
    }
    return word;
  }

  static int getSynsetAtCacheMiss = 0;
  static int getSynsetAtCacheHit = 0;
  static int weirdGetSynsetAtCacheMiss = 0;

  String getSynsetLineAt(final POS pos, final int offset) {
    final String fileName = getDataFilename(pos);
    try {
      return fileManager.readLineAt(offset, fileName);
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  @Override
  public Optional<Synset> getSynsetAt(final POS pos, final int offset) {
    final DatabaseKey cacheKey = new POSOffsetDatabaseKey(pos, offset);
    Synset synset = (Synset) synsetCache.get(cacheKey);
    if (synset != null) {
      getSynsetAtCacheHit++;
      cacheDebug(synsetCache);
    } else {
      getSynsetAtCacheMiss++;
      cacheDebug(synsetCache);
      try {
        synset = new Synset(getSynsetLineAt(pos, offset), this);
        synsetCache.put(cacheKey, synset);
      } catch (IllegalArgumentException iae) {
        synset = null;
      }
    }
    return Optional.ofNullable(synset);
  }

  //
  // Lookup functions
  //

  static int lookupIndexWordCacheMiss = 0;
  static int lookupIndexWordCacheHit = 0;
  static int weirdLookupIndexWordCacheMiss = 0;

  private static final Map<POS, BloomFilter<CharSequence>> INDEX_DATA_FILTERS;
  private static final Map<POS, BloomFilter<CharSequence>> EXCEPTIONS_FILTERS;
  static {
    INDEX_DATA_FILTERS = Maps.newEnumMap(POS.class);
    EXCEPTIONS_FILTERS = Maps.newEnumMap(POS.class);

    for (final POS pos : POS.CATS) {
      // assume WN dict/ is in the classpath
      final String indexDataResourceName = "dict/" + pos.name() + ".bloom";
      final BloomFilter<CharSequence> indexDataFilter = getResource(indexDataResourceName);
      if (indexDataFilter != null) {
        INDEX_DATA_FILTERS.put(pos, indexDataFilter);
      }
      final String exceptionsResourceName = "dict/" + pos.name() + ".exc.bloom";
      final BloomFilter<CharSequence> exceptionsFilter = getResource(exceptionsResourceName);
      if (exceptionsFilter != null) {
        EXCEPTIONS_FILTERS.put(pos, exceptionsFilter);
      }
    }
  }

  // look in classpath for filters
  private static BloomFilter<CharSequence> getResource(final String resourceName) {
    try {
      final URL url = WordNet.class.getClassLoader().getResource(resourceName);
      if (url == null) {
        log.info("resourceName: {} not found!", resourceName);
        return null;
      }
      final URLConnection conn = url.openConnection();
      final InputStream input = conn.getInputStream();
      // fast CharStream created from InputStream (e.g., could be read from jar file)
      final ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(input));
      @SuppressWarnings("unchecked")
      final BloomFilter<CharSequence> filter = (BloomFilter<CharSequence>) ois.readObject();
      Closeables.closeQuietly(ois);
      return filter;
    } catch (Exception e) {
      log.info("caught", e);
      System.err.println("caught!"+e);
      return null;
    }
  }

  private boolean maybeDefined(final CharSequence lemma, final POS pos) {
    if (INDEX_DATA_FILTERS.isEmpty()) {
      return true;
    }
    return INDEX_DATA_FILTERS.get(pos).contains(lemma);
  }

  private boolean maybeException(final CharSequence lemma, final POS pos) {
    if (EXCEPTIONS_FILTERS.isEmpty()) {
      return true;
    }
    return EXCEPTIONS_FILTERS.get(pos).contains(lemma);
  }

  private static final Object NULL_INDEX_WORD = new Object();

  @Override
  public Word lookupWord(final CharSequence lemma, final POS pos) {
    checkValidPOS(pos, "by lookupWord(lemma, pos)");
    final DatabaseKey cacheKey = new StringPOSDatabaseKey(lemma, pos);
    Object indexWord = indexWordCache.get(cacheKey);
    if (indexWord != null && indexWord != NULL_INDEX_WORD) {
      lookupIndexWordCacheHit++;
      cacheDebug(indexWordCache);
    } else {
      indexWord = NULL_INDEX_WORD;
      // consult the Bloom filter
      if (maybeDefined(lemma, pos)) {
        lookupIndexWordCacheMiss++;
        cacheDebug(indexWordCache);
        final String fileName = getIndexFileName(pos);
        final int offset;
        try {
          offset = fileManager.getIndexedLinePointer(lemma, fileName);
        } catch (IOException ioe) {
          throw new RuntimeException(ioe);
        }
        if (offset >= 0) {
          indexWord = getIndexWordAt(pos, offset);
        }
        //else {
          // if here && ! INDEX_DATA_FILTERS.isEmpty()
          //   false positive
        //}
      }
      // best not to add negative results (indexWord == NULL_INDEX_WORD)
      // to the LRU cache - let Bloom filter / ! maybeDefined() handle this
      if (indexWord != NULL_INDEX_WORD) {
        indexWordCache.put(cacheKey, indexWord);
      }
    }
    return indexWord != NULL_INDEX_WORD ? (Word) indexWord : null;
  }

  @Override
  public List<String> lookupBaseForms(final String someString, final POS pos) {
    if (pos == POS.ALL) {
      return LightImmutableList.copyOf(uniq(merge(
        lookupBaseForms(someString, POS.NOUN),
        lookupBaseForms(someString, POS.VERB),
        lookupBaseForms(someString, POS.ADJ),
        lookupBaseForms(someString, POS.ADV))));
    } else {
      return morphy.morphstr(someString, pos);
    }
  }

  @Override
  public List<Synset> lookupSynsets(final String someString, final POS pos) {
    if (pos == POS.ALL) {
      return LightImmutableList.copyOf(uniq(merge(
        lookupSynsets(someString, POS.NOUN),
        lookupSynsets(someString, POS.VERB),
        lookupSynsets(someString, POS.ADJ),
        lookupSynsets(someString, POS.ADV))));
    } else {
      return doLookupSynsets(someString, pos);
    }
  }

  private List<Synset> doLookupSynsets(final String someString, final POS pos) {
    checkValidPOS(pos, "by doLookupSynsets()");
    final LightImmutableList<String> morphs = morphy.morphstr(someString, pos);
    if (morphs.isEmpty()) {
      return LightImmutableList.of();
    }
    // 0. if we have morphs, we will usually have syns
    // 1. get all the Words (usually 1, except for exceptional forms (e.g., 'geese'))
    // 2. merge all their Synsets
    final ArrayList<Synset> syns = new ArrayList<>();
    int morphNum = -1;
    for (final String lemma : morphs) {
      morphNum++;
      final Word word = this.lookupWord(lemma, pos);
      if (word == null) {
        // some morphstr() values will not be defined words (lemmas).
        continue;
      }
      syns.ensureCapacity(syns.size() + word.getSynsets().size());
      syns.addAll(word.getSynsets());
    }
    // sometimes all morphstr() values will be generated and undefined for this POS
    // FIXME annoying that morphy sometimes returns undefined variants
    if (! morphs.isEmpty() && syns.isEmpty()) {
      //log.log(Level.WARNING, "no syns for \""+someString+"\" morphs: "+morphs+" "+pos);
      return LightImmutableList.of();
    }
    // TODO dedup this ?
    return LightImmutableList.copyOf(syns);
  }

  @Override
  public List<WordSense> lookupWordSenses(final String someString, final POS pos) {
    if (pos == POS.ALL) {
      return LightImmutableList.copyOf(uniq(merge(
        lookupWordSenses(someString, POS.NOUN),
        lookupWordSenses(someString, POS.VERB),
        lookupWordSenses(someString, POS.ADJ),
        lookupWordSenses(someString, POS.ADV))));
    } else {
      return doLookupWordSenses(someString, pos);
    }
  }

  // FIXME refactor! this is copy paste from doLookupSynsets ; however, code is really short
  private List<WordSense> doLookupWordSenses(final String someString, final POS pos) {
    checkValidPOS(pos, "by doLookupWordSenses()");
    final LightImmutableList<String> morphs = morphy.morphstr(someString, pos);
    if (morphs.isEmpty()) {
      return LightImmutableList.of();
    }
    // 0. if we have morphs, we will usually have syns
    // 1. get all the Words (usually 1, except for exceptional forms (e.g., 'geese'))
    // 2. merge all their Synsets
    final ArrayList<WordSense> wordSenses = new ArrayList<>();
    int morphNum = -1;
    for (final String lemma : morphs) {
      morphNum++;
      final Word word = this.lookupWord(lemma, pos);
      if (word == null) {
        // some morphstr() values will not be defined words (lemmas).
        continue;
      }
      wordSenses.ensureCapacity(wordSenses.size() + word.getSynsets().size());
      wordSenses.addAll(word.getWordSenses());
    }
    // sometimes all morphstr() values will be generated and undefined for this POS
    // FIXME annoying that morphy sometimes returns undefined variants
    if (! morphs.isEmpty() && wordSenses.isEmpty()) {
      //log.log(Level.WARNING, "no syns for \""+someString+"\" morphs: "+morphs+" "+pos);
      return LightImmutableList.of();
    }
    // TODO dedup this ?
    return LightImmutableList.copyOf(wordSenses);
  }

  @Override
  public Iterable<Synset> synsets(final String query) {
    final EnumMap<Command, String> cmdToValue = Command.getCmdToValue(query);
    if (cmdToValue.containsKey(Command.OFFSET)) {
      // hack: if 9 digit offset in cmdToValue, inserts implied Command.POS
      Command.OFFSET.act(cmdToValue, this);
    }

    if (cmdToValue.size() == 1) {
      if (cmdToValue.containsKey(Command.POS)) {
        final POS pos = POS.valueOf(cmdToValue.get(Command.POS));
        return synsets(pos);
      } else if (cmdToValue.containsKey(Command.LEXNAME)) {
        final Lexname lexname = Lexname.lookupLexname(cmdToValue.get(Command.LEXNAME));
        return synsets(lexname);
      } else if (cmdToValue.containsKey(Command.WORD)) {
        final String someString = cmdToValue.get(Command.WORD);
        final POS pos = POS.ALL;
        return lookupSynsets(someString, pos);
      }
    } else if (cmdToValue.size() == 2) {
      if (cmdToValue.containsKey(Command.POS)) {
        final POS pos = POS.valueOf(cmdToValue.get(Command.POS));
        if (cmdToValue.containsKey(Command.OFFSET)) {
          final int offset = Integer.parseInt(cmdToValue.get(Command.OFFSET));
          final Optional<Synset> synset = getSynsetAt(pos, offset);
          return synset.map(LightImmutableList::of).orElse(LightImmutableList.of());
        } else if (cmdToValue.containsKey(Command.WORD)) {
          final String someString = cmdToValue.get(Command.WORD);
          return lookupSynsets(someString, pos);
        }
      }
    }

    // future: sequences, e.g., 04073208 (release) 05847753 (stemmer)
    // - harder to interpret
    // - results have to be accumulated
    // - could be ambiguous
    throw new IllegalArgumentException("unsatisfiable query "+query);
  }

  @Override
  public Iterable<WordSense> wordSenses(final String query) {
    final EnumMap<Command, String> cmdToValue = Command.getCmdToValue(query);
    if (cmdToValue.size() == 1) {
      if (cmdToValue.containsKey(Command.POS)) {
        final POS pos = POS.valueOf(cmdToValue.get(Command.POS));
        return wordSenses(pos);
      } else if (cmdToValue.containsKey(Command.ADJ_POSITION)) {
        final AdjPosition adjPosition = AdjPosition.fromValue(cmdToValue.get(Command.ADJ_POSITION));
        return wordSenses(adjPosition);
      } else if (cmdToValue.containsKey(Command.WORD)) {
        final String someString = cmdToValue.get(Command.WORD);
        final POS pos = POS.ALL;
        return lookupWordSenses(someString, pos);
      }
    } else if (cmdToValue.size() == 2) {
      if (cmdToValue.containsKey(Command.POS)) {
        final POS pos = POS.valueOf(cmdToValue.get(Command.POS));
        if (cmdToValue.containsKey(Command.OFFSET)) {
          final int offset = Integer.parseInt(cmdToValue.get(Command.OFFSET));
          final Optional<Synset> synset = getSynsetAt(pos, offset);
          return synset.map(syn -> ((Iterable<WordSense>)syn)).orElse(LightImmutableList.of());
        } else if (cmdToValue.containsKey(Command.WORD)) {
          final String someString = cmdToValue.get(Command.WORD);
          return lookupWordSenses(someString, pos);
        }
      }
    }
    throw new IllegalArgumentException("unsatisfiable query "+query);
  }

  private final Cache<DatabaseKey, LightImmutableList<String>> exceptionsCache = Caches.withCapacity(DEFAULT_CACHE_CAPACITY);

  /**
   * <em>looks up</em> word in the appropriate <em>exc</em>eptions file for the given {@code pos}.
   * The exception list files, <tt>pos</tt>.<em>exc</em> , are used to help the morphological
   * processor find base forms from irregular inflections.
   * <strong>NOTE: The first entry is the exceptional word itself (e.g., for "geese", it's "geese")</strong>.
   * Port of {@code morph.c exc_lookup()}
   * @see <a href="https://wordnet.princeton.edu/documentation/morphy7wn#sect3">
   *   https://wordnet.princeton.edu/documentation/morphy7wn#sect3</a>
   */
  LightImmutableList<String> getExceptions(final CharSequence someString, final POS pos) {
    checkValidPOS(pos, "by getExceptions()");
    if (! maybeException(someString, pos)) {
      return LightImmutableList.of();
    }
    final DatabaseKey cacheKey = new StringPOSDatabaseKey(someString, pos);
    final LightImmutableList<String> cached = exceptionsCache.get(cacheKey);
    if (cached != null) {
      return cached;
    }
    assert someString != null;
    // empty string is valid input
    //assert someString.length() > 0 : "someString: \""+someString+"\" "+pos+" cacheKey: "+cacheKey;
    assert pos != null;
    final String fileName = getExceptionsFilename(pos);
    try {
      final int offset = fileManager.getIndexedLinePointer(someString, fileName);
      if (offset >= 0) {
        final String line = fileManager.readLineAt(offset, fileName);
        final LightImmutableList<String> toReturn = LightImmutableList.copyOf(new StringTokenizer(line, " "));
        assert toReturn.size() >= 2;
        exceptionsCache.put(cacheKey, toReturn);
        return toReturn;
      } else {
        exceptionsCache.put(cacheKey, LightImmutableList.of());
      }
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
    return LightImmutableList.of();
  }

  /**
   * <em>looks up</em> <a href="https://wordnet.princeton.edu/documentation/senseidx5wn#sect3">senskey</a>
   * in the {@code cntlist.rev} file and returns the matching line (or
   * {@code null}).  Informationally equivalent to searching
   * {@code index.sense} (or {@code sense.idx} on older Windows
   * releases).  Differences are that {@code cntlist.rev} includes defunct
   * sense information (does no harm though because it isn't referenced in its
   * WordNet), doesn't include entries for items with zero counts, doesn't
   * include synset offset, and formats adjective sense keys correctly (including
   * {@link WordSense.AdjPosition} information).
   *
   * @see Word#getTaggedSenseCount()
   */
  String lookupCntlistDotRevLine(final CharSequence senseKey) {
    //TODO add caching
    try {
      final int offset = fileManager.getIndexedLinePointer(senseKey, PlainTextResource.CNTLIST_DOT_REV.getFileName());
      final String line;
      if (offset < 0) {
        line = null;
      } else {
        line = fileManager.readLineAt(offset, PlainTextResource.CNTLIST_DOT_REV.getFileName());
      }
      return line;
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  // throws IllegalStateException if data file is not found
  String lookupCoreRankLine(final CharSequence senseKey) {
    try {
      final int offset = fileManager.getIndexedLinePointer(senseKey, PlainTextResource.CORE_RANK.getFileName());
      final String line;
      if (offset < 0) {
        line = null;
      } else {
        line = fileManager.readLineAt(offset, PlainTextResource.CORE_RANK.getFileName());
      }
      return line;
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  // throws IllegalStateException if data file is not found
  Iterable<CharSequence> lookupMorphoSemanticRelationLines(final CharSequence senseKey) {
    try {
      return fileManager.getMatchingLines(senseKey, PlainTextResource.MORPHOSEMANTIC_RELATIONS.getFileName());
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  // throws IllegalStateException if data file is not found
  Iterable<CharSequence> lookupVerbGroupLines(final CharSequence senseKey) {
    try {
      return fileManager.getMatchingLines(senseKey, PlainTextResource.VERB_GROUP_RELATIONS.getFileName());
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  /** XXX DOCUMENT ME */
  String lookupGenericFrame(final int framenum) {
    assert framenum >= 1;
    try {
      String line = fileManager.readLineNumber(framenum - 1, PlainTextResource.GENERIC_VERB_FRAMES.getFileName());
      assert line != null : "framenum: "+framenum;
      // parse line. format example:
      //<number>
      //<framenum>[ ]+<frame string>

      //TODO make this a util method indexOfNonSpace(CharSequence, sidx)
      // skip leading digits, skip spaces, rest is frame text
      int idx = line.indexOf(' ');
      assert idx >= 0;
      for (int i = idx + 1, n = line.length(); i < n && line.charAt(i) == ' '; i++) {
        idx++;
      }
      assert line.charAt(idx) == ' ';
      assert line.charAt(idx + 1) != ' ';
      idx++;
      line = line.substring(idx);
      return line;
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  /**
   * @return verb sentence numbers as a comma separated list with no spaces
   *         (e.g., "15,16")
   */
  String lookupVerbSentencesNumbers(final CharSequence verbSenseKey) {
    try {
      String line = null;
      final int offset = fileManager.getIndexedLinePointer(verbSenseKey, PlainTextResource.VERB_SENTENCES_INDEX.getFileName());
      if (offset >= 0) {
        line = fileManager.readLineAt(offset, PlainTextResource.VERB_SENTENCES_INDEX.getFileName());
        assert line != null;
        // parse line. format example:
        //<number>
        //<framenum>[ ]+<frame string>

        // skip leading digits, skip spaces, rest is frame text
        int idx = line.indexOf(' ');
        assert idx >= 0;
        for (int i = idx + 1, n = line.length(); i < n && line.charAt(i) == ' '; i++) {
          idx++;
        }
        assert line.charAt(idx) == ' ';
        if (idx + 1 < line.length()) {
          assert line.charAt(idx + 1) != ' ' : "verbSenseKey: "+verbSenseKey;
          idx++;
          line = line.substring(idx);
        } else {
          // bug in sents.vrb (WordNet version 3.0)
          // which contains single invalid line:
          // "pet%2:35:00:: "
          line = null;
        }
      }
      return line;
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  /**
   * @return illustrative verb sentence for given number. Always contains "%s".
   */
  String lookupVerbSentence(final String verbSentenceNumber) {
    String line = null;
    try {
      final int offset = fileManager.getIndexedLinePointer(verbSentenceNumber, PlainTextResource.VERB_SENTENCES.getFileName());
      if (offset >= 0) {
        line = fileManager.readLineAt(offset, PlainTextResource.VERB_SENTENCES.getFileName());
        assert line != null;
        // parse line. format example:
        //<number>
        //<sentenceNumber>[ ]+<sentence string>

        // skip leading digits, skip spaces, rest is sentence text
        int idx = line.indexOf(' ');
        assert idx >= 0;
        for (int i = idx + 1, n = line.length(); i < n && line.charAt(i) == ' '; i++) {
          idx++;
        }
        assert line.charAt(idx) == ' ';
        assert line.charAt(idx + 1) != ' ';
        idx++;
        line = line.substring(idx);
        assert line.contains("%s");
      }
      return line;
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }

  private static void checkValidPOS(final POS pos, String msg) {
    Preconditions.checkArgument(POS.ALL != pos, "POS.ALL is not supported by %s", msg);
  }

  //
  // Iterators
  //

  private abstract class AbstractWordIterator extends AbstractIterator<Word> {
    private static final String TWO_SPACES = "  ";
    protected final POS pos;
    protected final String fileName;
    protected int nextOffset = 0;

    protected AbstractWordIterator(final POS pos) {
      this.pos = pos;
      this.fileName = getIndexFileName(pos);
    }

    protected void skipLicenseLines() throws IOException {
      if (nextOffset != 0) {
        return;
      }
      String line;
      int offset = -1;
      do {
        if (nextOffset < 0) {
          throw new NoSuchElementException();
        }
        line = fileManager.readLineAt(nextOffset, fileName);
        if (line == null) {
          break;
        }
        offset = nextOffset;
        nextOffset = fileManager.getNextLinePointer(nextOffset, fileName);
      } while (line.startsWith(TWO_SPACES)); // first few lines start with TWO_SPACES
      assert nextOffset != -1;
      nextOffset = offset;
    }
  } // end class AbstractWordIterator

  /**
   * @see WordNetInterface#words
   */
  private class WordIterator extends AbstractWordIterator {
    WordIterator(final POS pos) {
      super(pos);
    }
    @Override
    protected Word computeNext() {
      try {
        skipLicenseLines();
        final int offset = nextOffset;
        final String line = fileManager.readLineAt(nextOffset, fileName);
        nextOffset = fileManager.getNextLinePointer(nextOffset, fileName);
        if (line == null) {
          return endOfData();
        }
        return new Word(line, offset, WordNet.this);
      } catch (final IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  } // end class WordIterator

  @Override
  public Iterable<Word> words(final POS pos) {
    if (pos == POS.ALL) {
      return merge(
        words(POS.NOUN),
        words(POS.VERB),
        words(POS.ADJ),
        words(POS.ADV));
    } else {
      return () -> new WordIterator(pos);
    }
  }

  /**
   * @see WordNetInterface#searchBySubstring
   */
  private class SearchBySubstringIterator extends AbstractWordIterator {
    private final Matcher matcher;
    SearchBySubstringIterator(final POS pos, final CharSequence pattern) {
      super(pos);
      // searchNormalize lowercases and translates spaces to underscores
      // this can throw PatternSyntaxException; gigo
      this.matcher = Pattern.compile(Morphy.searchNormalize(pattern.toString())).matcher("");
    }
    @Override
    protected Word computeNext() {
      try {
        skipLicenseLines();
        final int offset = fileManager.getMatchingLinePointer(nextOffset, matcher, fileName);
        if (offset >= 0) {
          final Word value = getIndexWordAt(pos, offset);
          nextOffset = fileManager.getNextLinePointer(offset, fileName);
          return value;
        } else {
          return endOfData();
        }
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  } // end class SearchBySubstringIterator

  @Override
  public Iterable<Word> searchBySubstring(final CharSequence substring, final POS pos) {
    if (pos == POS.ALL) {
      return merge(
          searchBySubstring(substring, POS.NOUN),
          searchBySubstring(substring, POS.VERB),
          searchBySubstring(substring, POS.ADJ),
          searchBySubstring(substring, POS.ADV));
    } else {
      return () -> new SearchBySubstringIterator(pos, substring);
    }
  }

  /**
   * @see WordNetInterface#searchByPrefix
   */
  private class SearchByPrefixIterator extends AbstractIterator<Word> {
    private final POS pos;
    private final CharSequence prefix;
    private final String fileName;
    private int nextOffset;
    SearchByPrefixIterator(final POS pos, final CharSequence prefix) {
      this.pos = pos;
      this.prefix = Morphy.searchNormalize(prefix.toString());
      this.fileName = getIndexFileName(pos);
    }
    @Override
    protected Word computeNext() {
      try {
        final int offset = fileManager.getPrefixMatchLinePointer(nextOffset, prefix, fileName);
        if (offset >= 0) {
          final Word value = getIndexWordAt(pos, offset);
          // setup for next element
          nextOffset = fileManager.getNextLinePointer(offset, fileName);
          return value;
        } else {
          return endOfData();
        }
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  } // end class SearchByPrefixIterator

  @Override
  public Iterable<Word> searchByPrefix(final CharSequence prefix, final POS pos) {
    if (pos == POS.ALL) {
      return merge(
        searchByPrefix(prefix, POS.NOUN),
        searchByPrefix(prefix, POS.VERB),
        searchByPrefix(prefix, POS.ADJ),
        searchByPrefix(prefix, POS.ADV));
    } else {
      return () -> new SearchByPrefixIterator(pos, prefix);
    }
  }

  /**
   * @see WordNetInterface#searchGlossBySubstring
   */
  private class SearchGlossBySubstringIterator extends AbstractIterator<Synset> {
    private final Iterator<Synset> syns;
    private final Matcher matcher;
    SearchGlossBySubstringIterator(final POS pos, final CharSequence pattern) {
      this.syns = synsets(pos).iterator();
      // this can throw PatternSyntaxException; gigo
      this.matcher = Pattern.compile(pattern.toString()).matcher("");
    }
    @Override
    protected Synset computeNext() {
      while (syns.hasNext()) {
        final Synset syn = syns.next();
        if (matcher.reset(syn.getGloss()).find()) {
          return syn;
        }
      }
      return endOfData();
    }
  } // end class SearchGlossBySubstringIterator

  @Override
  public Iterable<Synset> searchGlossBySubstring(final CharSequence substring, final POS pos) {
    if (pos == POS.ALL) {
      return merge(
        searchGlossBySubstring(substring, POS.NOUN),
        searchGlossBySubstring(substring, POS.VERB),
        searchGlossBySubstring(substring, POS.ADJ),
        searchGlossBySubstring(substring, POS.ADV));
    } else {
      return () -> new SearchGlossBySubstringIterator(pos, substring);
    }
  }

  Iterable<Synset> synsets(final Lexname lexname) {
    return () -> new LexnameIterator(lexname);
  }

  private class LexnameIterator extends AbstractIterator<Synset> {
    private final Iterator<Synset> syns;
    private final Lexname lexname;
    LexnameIterator(final Lexname lexname) {
      this.syns = synsets(lexname.getPOS()).iterator();
      this.lexname = lexname;
    }
    @Override
    protected Synset computeNext() {
      while (syns.hasNext()) {
        final Synset syn = syns.next();
        if (lexname == syn.getLexname()) {
          return syn;
        }
      }
      return endOfData();
    }
  } // end class LexnameIterator

  Iterable<WordSense> wordSenses(final AdjPosition adjPosition) {
    return () -> new AdjPositionIterator(adjPosition);
  }

  private class AdjPositionIterator extends AbstractIterator<WordSense> {
    private final Iterator<WordSense> wordSenses;
    private final AdjPosition adjPosition;
    AdjPositionIterator(final AdjPosition adjPosition) {
      this.wordSenses = wordSenses(POS.ADJ).iterator();
      this.adjPosition = adjPosition;
    }
    @Override
    protected WordSense computeNext() {
      while (wordSenses.hasNext()) {
        final WordSense wordSense = wordSenses.next();
        if (adjPosition == wordSense.getAdjPosition()) {
          return wordSense;
        }
      }
      return endOfData();
    }
  } // end class AdjPositionIterator

  /**
   * @see WordNetInterface#synsets
   */
  private class POSSynsetsIterator extends AbstractIterator<Synset> {
    private final POS pos;
    private final String fileName;
    private int nextOffset;
    POSSynsetsIterator(final POS pos) {
      this.pos = pos;
      this.fileName = getDataFilename(pos);
    }
    @Override
    protected Synset computeNext() {
      try {
        String line;
        int offset;
        do {
          if (nextOffset < 0) {
            throw new NoSuchElementException();
          }
          line = fileManager.readLineAt(nextOffset, fileName);
          offset = nextOffset;
          if (line == null) {
            return endOfData();
          }
          nextOffset = fileManager.getNextLinePointer(nextOffset, fileName);
        } while (line.startsWith("  ")); // first few lines start with "  "
        int usedOffset = offset;
        return getSynsetAt(pos, offset).orElseThrow(
            () -> new NoSuchElementException(pos + " " + usedOffset));
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  } // end class POSSynsetsIterator

  @Override
  public Iterable<Synset> synsets(final POS pos) {
    if (pos == POS.ALL) {
      return merge(
        synsets(POS.NOUN),
        synsets(POS.VERB),
        synsets(POS.ADJ),
        synsets(POS.ADV));
    } else {
      return () -> new POSSynsetsIterator(pos);
    }
  }

  /**
   * @see WordNetInterface#wordSenses
   */
  private class POSWordSensesIterator extends AbstractIterator<WordSense> {
    private final Iterator<WordSense> wordSenses;
    POSWordSensesIterator(final POS pos) {
      // uses 2 level Iterator - first is Words, second is their WordSenses
      // Both levels have a variable number of members
      // Only second level's elements are emitted.
      this.wordSenses = concat(words(pos)).iterator();
    }
    @Override
    protected WordSense computeNext() {
      if (wordSenses.hasNext()) {
        return wordSenses.next();
      }
      return endOfData();
    }
  } // end class POSWordSensesIterator

  @Override
  public Iterable<WordSense> wordSenses(final POS pos) {
    if (pos == POS.ALL) {
      return merge(
        wordSenses(POS.NOUN),
        wordSenses(POS.VERB),
        wordSenses(POS.ADJ),
        wordSenses(POS.ADV));
    } else {
      return () -> new POSWordSensesIterator(pos);
    }
  }

  /**
   * @see WordNetInterface#relations
   */
  private class POSRelationsIterator extends AbstractIterator<Relation> {
    private final Iterator<Relation> relations;
    POSRelationsIterator(final POS pos, final RelationType relationType) {
      this.relations = concat(transform(synsets(pos), new SynsetToRelations(relationType))).iterator();
    }
    @Override
    protected Relation computeNext() {
      if (relations.hasNext()) {
        return relations.next();
      }
      return endOfData();
    }
  } // end class POSRelationsIterator

  private static class SynsetToRelations implements Function<Synset, List<Relation>> {
    private final RelationType relationType;
    SynsetToRelations(final RelationType relationType) {
      this.relationType = relationType;
    }
    @Override
    public List<Relation> apply(final Synset synset) {
      if (relationType == null) {
        return synset.getRelations();
      } else {
        return synset.getRelations(relationType);
      }
    }
  } // end class SynsetToRelations

  @Override
  public Iterable<Relation> relations(final POS pos) {
    return relations(null, pos);
  }

  @Override
  public Iterable<Relation> relations(final RelationType relationType, final POS pos) {
    if (pos == POS.ALL) {
      return merge(
        relations(relationType, POS.NOUN),
        relations(relationType, POS.VERB),
        relations(relationType, POS.ADJ),
        relations(relationType, POS.ADV));
    } else {
      return () -> new POSRelationsIterator(pos, relationType);
    }
  }

  /**
   * @see WordNetInterface#exceptions
   */
  private class POSExceptionsIterator extends AbstractIterator<List<String>> {
    private final String fileName;
    private int nextOffset;
    POSExceptionsIterator(final POS pos) {
      this.fileName = getExceptionsFilename(pos);
    }
    @Override
    protected List<String> computeNext() {
      try {
        final String line = fileManager.readLineAt(nextOffset, fileName);
        if (line == null) {
          return endOfData();
        }
        nextOffset = fileManager.getNextLinePointer(nextOffset, fileName);
        final LightImmutableList<String> toReturn = LightImmutableList.of(line.split(" "));
        assert toReturn.size() >= 2;
        return toReturn;
      } catch (IOException ioe) {
        throw new RuntimeException(ioe);
      }
    }
  } // end class POSExceptionsIterator

  @Override
  public Iterable<List<String>> exceptions(final POS pos) {
    if (pos == POS.ALL) {
      return concat(
        exceptions(POS.NOUN),
        exceptions(POS.VERB),
        exceptions(POS.ADJ),
        exceptions(POS.ADV));
    } else {
      return () -> new POSExceptionsIterator(pos);
    }
  }
}