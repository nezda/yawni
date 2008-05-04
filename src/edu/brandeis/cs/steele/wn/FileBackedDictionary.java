/*
 * Copyright 1998 by Oliver Steele.  You can use this software freely so long as you preserve
 * the copyright notice and this restriction, and label your changes.
 */
package edu.brandeis.cs.steele.wn;

import edu.brandeis.cs.steele.util.*;
import java.io.*;
import java.util.*;
import java.util.logging.*;

/** A <code>DictionaryDatabase</code> that retrieves objects from the text files in the WordNet distribution
 * directory.
 *
 * A <code>FileBackedDictionary</code> has an <i>entity cache</i>.  The entity cache is used to resolve multiple
 * temporally contiguous lookups of the same entity to the same object -- for example, successive
 * calls to <code>lookupWord</code> with the same parameters would return the same value
 * (<code>==</code> as well as <code>equals</code>), as would traversal of two <code>Pointer</code>s
 * that shared the same target.  The current implementation uses an LRU cache, so it's possible for
 * two different objects to represent the same entity, if their retrieval is separated by other
 * database operations.  FIXME revisit this comment FIXME <i>The LRU cache will be replaced by a 
 * cache based on WeakHashMap, once JDK 1.2 becomes more widely available.</i>
 *
 * @see edu.brandeis.cs.steele.wn.DictionaryDatabase
 * @see edu.brandeis.cs.steele.util.Cache
 * @see edu.brandeis.cs.steele.util.LRUCache
 * @author Oliver Steele, steele@cs.brandeis.edu
 * @version 1.0
 */
public class FileBackedDictionary implements DictionaryDatabase {
  private static final Logger log = Logger.getLogger(FileBackedDictionary.class.getName());
  static {
    log.setLevel(Level.SEVERE);
    //log.setLevel(Level.FINER);
  }
  
  static {
    final Handler handler = new ConsoleHandler();
    handler.setLevel(Level.FINEST);
    handler.setFormatter(new InfoOnlyFormatter());
    log.addHandler(handler);
  }

  private final FileManagerInterface db;
  private final Morphy morphy;


  //
  // Constructors
  //

  /** Construct a {@link DictionaryDatabase} that retrieves file data from
   * <code>fileManager</code>.  A client can use this to create a
   * {@link DictionaryDatabase} backed by a {@link RemoteFileManager}.
   * @see RemoteFileManager
   */
  FileBackedDictionary(final FileManagerInterface fileManager) {
    this.db = fileManager;
    this.morphy = new Morphy(this);
  }

  /** Construct a dictionary backed by a set of files contained in the default
   * WordNet search directory.  
   * @see FileManager for a description of the location of the default
   * WordNet search directory (<code>$WNSEARCHDIR</code>). 
   */
  FileBackedDictionary() {
    this(new FileManager());
  }

  /** Construct a dictionary backed by a set of files contained in
   * <var>search directory</var>. 
   */
  FileBackedDictionary(final String searchDirectory) {
    this(new FileManager(searchDirectory));
  }

  static class InstanceHolder {
    /** singleton reference */
    static final FileBackedDictionary instance = new FileBackedDictionary();
  } // end class InstanceHolder

  /** Factory method to get <i>the</i> dictionary backed by a set of files contained
   * in the default WordNet search directory.
   * @see FileManager for a description of the location of the default
   * WordNet search directory (<code>$WNSEARCHDIR</code>). 
   */
  public static FileBackedDictionary getInstance() {
    return InstanceHolder.instance;
  }

  /** Factory method to get <i>the</i> dictionary backed by a set of files contained
   * in <var>searchDirectory</var>.
   */
  //FIXME ignores passed in searchDirectory reference
  public static FileBackedDictionary getInstance(final String searchDirectory) {
    return InstanceHolder.instance;
  }

  /** Factory method to get <i>the</i> {@link DictionaryDatabase} that retrieves file data from
   * <code>fileManager</code>.  A client can use this to create a
   * {@link DictionaryDatabase} backed by a {@link RemoteFileManager}.
   * @see RemoteFileManager
   */
  //FIXME ignores passed in fileManager reference
  public static FileBackedDictionary getInstance(final FileManagerInterface fileManager) {
    return InstanceHolder.instance;
  }


  //
  // Entity lookup caching
  //
  final int DEFAULT_CACHE_CAPACITY = 10000;//100000;
  private Cache synsetCache = new LRUCache(DEFAULT_CACHE_CAPACITY);
  private Cache indexWordCache = new LRUCache(DEFAULT_CACHE_CAPACITY);
  
  static interface DatabaseKey {
    public int hashCode();
    public boolean equals(Object that);
  } // end interface DatabaseKey
  
  static class POSOffsetDatabaseKey implements DatabaseKey {
    private final int offset;
    private final byte posOrdinal;

    POSOffsetDatabaseKey(final POS pos, final int offset) {
      this.offset = offset;
      this.posOrdinal = (byte) pos.ordinal();
    }

    @Override public boolean equals(final Object object) {
      if(object instanceof POSOffsetDatabaseKey) {
        final POSOffsetDatabaseKey that = (POSOffsetDatabaseKey)object;
        return that.posOrdinal == this.posOrdinal && that.offset == this.offset;
      }
      return false;
    }

    @Override public int hashCode() {
      return ((int) offset * 10) + posOrdinal;
    }
  } // end class POSOffsetDatabaseKey
  
  static class StringPOSDatabaseKey implements DatabaseKey {
    private final String key;
    private final byte posOrdinal;

    StringPOSDatabaseKey(final String key, final POS pos) {
      this.key = key;
      this.posOrdinal = (byte)pos.ordinal();
    }

    @Override public boolean equals(final Object object) {
      if(object instanceof StringPOSDatabaseKey) {
        final StringPOSDatabaseKey that = (StringPOSDatabaseKey)object;
        return that.posOrdinal == this.posOrdinal && that.key.equals(this.key);
      }
      return false;
    }

    @Override public int hashCode() {
      return posOrdinal ^ key.hashCode();
    }
  } // end class StringPOSDatabaseKey

  //
  // File name computation
  //
  private static final Map<POS, String> POS_TO_FILENAME_ROOT;
  static {
    POS_TO_FILENAME_ROOT = new EnumMap<POS, String>(POS.class);
    POS_TO_FILENAME_ROOT.put(POS.NOUN, "noun");
    POS_TO_FILENAME_ROOT.put(POS.VERB, "verb");
    POS_TO_FILENAME_ROOT.put(POS.ADJ, "adj");
    POS_TO_FILENAME_ROOT.put(POS.ADV, "adv");
  }

  /** NOTE: Called at most once per POS */
  private static String getDatabaseSuffixName(final POS pos) {
    assert POS_TO_FILENAME_ROOT.containsKey(pos);
    return POS_TO_FILENAME_ROOT.get(pos);
  }

  private static final Map<POS, String> DATA_FILE_NAMES = new EnumMap<POS, String>(POS.class);
  
  private static String getDataFilename(final POS pos) {
    String toReturn = DATA_FILE_NAMES.get(pos);
    if(toReturn == null) {
      toReturn = "data." + getDatabaseSuffixName(pos);
      DATA_FILE_NAMES.put(pos, toReturn);
    }
    return toReturn;
  }
  
  private static final Map<POS, String> INDEX_FILE_NAMES = new EnumMap<POS, String>(POS.class);
  
  private static String getIndexFilename(final POS pos) {
    String toReturn = INDEX_FILE_NAMES.get(pos);
    if(toReturn == null) {
      toReturn = "index." + getDatabaseSuffixName(pos);
      INDEX_FILE_NAMES.put(pos, toReturn);
    }
    return toReturn;
  }

  private static final Map<POS, String> EXCEPTION_FILE_NAMES = new EnumMap<POS, String>(POS.class);
  
  private static String getExceptionsFilename(final POS pos) {
    String toReturn = EXCEPTION_FILE_NAMES.get(pos);
    if(toReturn == null) {
      toReturn = getDatabaseSuffixName(pos) + ".exc";
      EXCEPTION_FILE_NAMES.put(pos, toReturn);
    }
    return toReturn;
  }
  
  private static String getCntlistDotRevFilename() {
    return "cntlist.rev";
  }

  private static String getLexnamesFilename() {
    return "lexnames";
  }

  private static String getVerbSentencesIndexFilename() {
    return "sentidx.vrb";
  }

  private static String getVerbSentencesFilename() {
    return "sents.vrb";
  }

  private static String getGenericVerbFramesFilename() {
    return "frames.vrb";
  }

  //
  // Entity retrieval
  //
  
  //FIXME cache's don't store null values!
  private static void cacheDebug(final Cache cache) {
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
      ++getIndexWordAtCacheHit;
      cacheDebug(indexWordCache);
    } else {
      ++getIndexWordAtCacheMiss;
      cacheDebug(indexWordCache);
      final String filename = getIndexFilename(pos);
      final CharSequence line;
      try {
        line = db.readLineAt(filename, offset);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      word = new Word(line, offset);
      indexWordCache.put(cacheKey, word);
    }
    assert word != null : "pos: "+pos+" offset: "+offset;
    return word;
  }
  
  static int getSynsetAtCacheMiss = 0;
  static int getSynsetAtCacheHit = 0;
  static int weirdGetSynsetAtCacheMiss = 0;
  
  Synset getSynsetAt(final POS pos, final int offset, String line) {
    final DatabaseKey cacheKey = new POSOffsetDatabaseKey(pos, offset);
    Synset synset = (Synset) synsetCache.get(cacheKey);
    if (synset != null) {
      ++getSynsetAtCacheHit;
      cacheDebug(synsetCache);
    } else {
      ++getSynsetAtCacheMiss;
      cacheDebug(synsetCache);
      if (line == null) {
        final String filename = getDataFilename(pos);
        try {
          line = db.readLineAt(filename, offset);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
      synset = new Synset(line);
      synsetCache.put(cacheKey, synset);
    }
    assert synset != null : "pos: "+pos+" offset: "+offset+" line: "+line;
    return synset;
  }

  Synset getSynsetAt(final POS pos, final int offset) {
    return getSynsetAt(pos, offset, null);
  }


  //
  // Lookup functions
  //
  
  static int lookupIndexWordCacheMiss = 0;
  static int lookupIndexWordCacheHit = 0;
  static int weirdLookupIndexWordCacheMiss = 0;

  private static Object NULL_INDEX_WORD = new Object();

  /** {@inheritDoc} */
  public Word lookupWord(final POS pos, final String lemma) {
    final DatabaseKey cacheKey = new StringPOSDatabaseKey(lemma, pos);
    Object indexWord = indexWordCache.get(cacheKey);
    if (indexWord != null && indexWord != NULL_INDEX_WORD) {
      ++lookupIndexWordCacheHit;
      cacheDebug(indexWordCache);
    } else {
      ++lookupIndexWordCacheMiss;
      cacheDebug(indexWordCache);
      final String filename = getIndexFilename(pos);
      final int offset;
      try {
        offset = db.getIndexedLinePointer(filename, lemma.toLowerCase().replace(' ', '_'));
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      if (offset >= 0) {
        indexWord = getIndexWordAt(pos, offset);
      } else {
        indexWord = NULL_INDEX_WORD;
      }
      indexWordCache.put(cacheKey, indexWord);
    }
    return indexWord != NULL_INDEX_WORD ? (Word) indexWord : null;
  }
  
  /** LN Not used much - this might not even have a <i>unique</i> result ? */
  public String lookupBaseForm(final POS pos, final String derivation) {
    // TODO add caching!
    // FIXME in addition to exceptions file and Morhpy.morphstr() too
    // use getindex() too ?
    final String filename = getExceptionsFilename(pos);
    try {
      final int offset = db.getIndexedLinePointer(filename, derivation.toLowerCase());
      if (offset >= 0) {
        final String line = db.readLineAt(filename, offset);
        // FIXME there could be > 1 entry on this line of the exception file
        // technically, i think should return the last word:
        //   line.substring(line.lastIndexOf(' ') + 1)
        final int spaceIdx = line.indexOf(' ');
        return line.substring(spaceIdx + 1);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }
  
  /** {@inheritDoc} */
  public String[] lookupBaseForms(final POS pos, final String someString) {
    // TODO use getindex() too ?
    final List<String> morphs = morphy.morphstr(someString, pos);
    if(morphs.isEmpty()) {
      return NO_STRINGS;
    }
    final String[] toReturn = morphs.toArray(new String[morphs.size()]);
    return toReturn;
  }

  private static final Synset[] NO_SYNSETS = new Synset[0];

  /** {@inheritDoc} */
  public Synset[] lookupSynsets(final POS pos, final String someString) {
    // TODO use getindex() too ?
    final List<String> morphs = morphy.morphstr(someString, pos);
    if(morphs == null || morphs.isEmpty()) {
      return NO_SYNSETS;
    }
    // 0. if we have morphs, we will have syns
    // 1. get all the Words (usually 1)
    // 2. merge all their Synsets
    final ArrayList<Synset> syns = new ArrayList<Synset>();
    for(final String lemma : morphs) {
      final Word word = this.lookupWord(pos, lemma);
      if(word == null) {
        // LN little hacky - morphstr() bug that it returns a "lemma" for
        // an undefined word ?
        //assert morphs.size() == 1 : "morphs: "+morphs;
        if(morphs.size() != 1) {
          log.log(Level.WARNING, "morphs: "+morphs);
        }
        //break; // LN why did i break here ?
        continue;
      }
      syns.ensureCapacity(syns.size() + word.getSynsets().length);
      for(final Synset syn : word.getSynsets()) {
        syns.add(syn);
      }
    }
    // TODO dedup this ?
    return syns.toArray(new Synset[syns.size()]);
  }

  private final Cache exceptionsCache = new LRUCache(DEFAULT_CACHE_CAPACITY);
  //private final Cache exceptionsCache = new LRUCache(0);
  
  /** 
   * <i>looks up</i> word in the appropriate <i>exc</i>eptions file for the given <param>pos</param>.
   * The exception list files, <tt>pos</tt>.<i>exc</i> , are used to help the morphological
   * processor find base forms from irregular inflections.  <b>NOTE: Skip the
   * first entry (the exceptional word itself!)</b>
   * morph.c exc_lookup()
   */
  String[] getExceptions(final String someString, final POS pos) {
    final DatabaseKey cacheKey = new StringPOSDatabaseKey(someString, pos);
    final Object cached = exceptionsCache.get(cacheKey);
    if(cached != null) {
      return (String[]) cached;
    }
    assert someString != null;
    assert someString.length() > 0 : "someString: \""+someString+"\" "+pos;
    assert pos != null;
    final String filename = getExceptionsFilename(pos);
    try {
      final int offset = db.getIndexedLinePointer(filename, someString);
      if (offset >= 0) {
        final String line = db.readLineAt(filename, offset);
        final String[] toReturn = line.split(" ");
        assert toReturn.length >= 2;
        exceptionsCache.put(cacheKey, toReturn);
        return toReturn;
      } else {
        exceptionsCache.put(cacheKey, NO_STRINGS);
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return NO_STRINGS;
  }

  /** 
   * <i>looks up</i> <a href="http://wordnet.princeton.edu/man/senseidx.5WN.html#sect3">senskey</a> 
   * in the <code>cntlist.rev</code> file and returns the matching line (or
   * <code>null</code>).
   */
  String lookupCntlistDotRevLine(final String senseKey) {
    //TODO add caching
    final int offset;
    final String line;
    try {
      offset = db.getIndexedLinePointer(getCntlistDotRevFilename(), senseKey);
      if(offset < 0) {
        line = null;
      } else {
        line = db.readLineAt(getCntlistDotRevFilename(), offset);
      }
    } catch(IOException ioe) {
      throw new RuntimeException(ioe);
    }
    return line;
  }

  /** XXX DOCUMENT ME */
  String lookupLexCategory(final int lexnum) {
    assert lexnum >= 0;
    String line;
    try {
      line = db.readLineNumber(getLexnamesFilename(), lexnum);
      if(line != null) {
        // parse line. format example:
        //00	adj.all	3
        //<lexnum>\tlexname\t<pos ordinal>
        final int start = line.indexOf('\t');
        assert start != 0;
        int end = line.lastIndexOf('\t');
        assert start != end;
        line = line.substring(start + 1, end);
      } else if(lexnum < Lexnames.contents.length) {
        line = Lexnames.contents[lexnum][1];
      }
    } catch(IOException ioe) {
      throw new RuntimeException(ioe);
    }
    return line;
  }

  /** XXX DOCUMENT ME */
  String lookupGenericFrame(final int framenum) {
    assert framenum >= 1;
    String line = null;
    try {
      line = db.readLineNumber(getGenericVerbFramesFilename(), framenum - 1);
      assert line != null : "framenum: "+framenum;
      // parse line. format example:
      //<number>
      //<framenum>[ ]+<frame string>

      // skip leading digits, skip spaces, rest is frame text
      int idx = line.indexOf(" ");
      assert idx >= 0;
      for(int i = idx + 1, n = line.length(); i < n && line.charAt(i) == ' '; i++) {
        idx++;
      }
      assert line.charAt(idx) == ' ';
      assert line.charAt(idx + 1) != ' ';
      idx++;
      line = line.substring(idx);
    } catch(IOException ioe) {
      throw new RuntimeException(ioe);
    }
    return line;
  }

  /**
   * @return verb sentence numbers as a comma separated list with no spaces
   *         (e.g. "15,16")
   */
  String lookupVerbSentencesNumbers(final String verbSenseKey) {
    String line = null;
    try {
      final int offset = db.getIndexedLinePointer(getVerbSentencesIndexFilename(), verbSenseKey);
      if(offset >= 0) {
        line = db.readLineAt(getVerbSentencesIndexFilename(), offset);
        assert line != null;
        // parse line. format example:
        //<number>
        //<framenum>[ ]+<frame string>
        
        // skip leading digits, skip spaces, rest is frame text
        int idx = line.indexOf(" ");
        assert idx >= 0;
        for(int i = idx + 1, n = line.length(); i < n && line.charAt(i) == ' '; i++) {
          idx++;
        }
        assert line.charAt(idx) == ' ';
        if(idx + 1 < line.length()) {
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
    } catch(IOException ioe) {
      throw new RuntimeException(ioe);
    }
    return line;
  }

  /**
   * @return illustrative verb sentence for given number. Always contains "%s".
   */
  String lookupVerbSentence(final String verbSentenceNumber) {
    String line = null;
    try {
      final int offset = db.getIndexedLinePointer(getVerbSentencesFilename(), verbSentenceNumber);
      if(offset >= 0) {
        line = db.readLineAt(getVerbSentencesFilename(), offset);
        assert line != null;
        // parse line. format example:
        //<number>
        //<sentenceNumber>[ ]+<sentence string>
        
        // skip leading digits, skip spaces, rest is sentence text
        int idx = line.indexOf(" ");
        assert idx >= 0;
        for(int i = idx + 1, n = line.length(); i < n && line.charAt(i) == ' '; i++) {
          idx++;
        }
        assert line.charAt(idx) == ' ';
        assert line.charAt(idx + 1) != ' ';
        idx++;
        line = line.substring(idx);
        assert line.indexOf("%s") >= 0;
      }
    } catch(IOException ioe) {
      throw new RuntimeException(ioe);
    }
    return line;
  }

  private static final String[] NO_STRINGS = new String[0];
  
  //
  // Iterators
  //
  
  /** 
   * @see DictionaryDatabase#words 
   */
  // TODO don't do this throw NoSuchElementException iterator stuff
  private class WordIterator implements Iterator<Word> {
    private final POS pos;
    private final String filename;
    private int nextOffset = 0;
    private int offset = -1;

    WordIterator(final POS pos) {
      this.pos = pos;
      this.filename = getIndexFilename(pos);
    }
    public boolean hasNext() {
      // meant to be used with LookaheadIterator
      return true;
    }
    public Word next() {
      try {
        String line;
        do {
          if (nextOffset < 0) {
            throw new NoSuchElementException();
          }
          offset = nextOffset;
          line = db.readLineAt(filename, nextOffset);
          if (line == null) {
            throw new NoSuchElementException();
          }
          nextOffset = db.getNextLinePointer(filename, nextOffset);
        } while (line.startsWith("  ")); // first few lines start with "  "
        //FIXME something is wrong with this
        return new Word(line, offset);
      } catch (final IOException e) {
        throw new RuntimeException(e);
      }
    }
    public void remove() {
      throw new UnsupportedOperationException();
    }
  } // end class WordIterator

  /** {@inheritDoc} */
  public Iterable<Word> words(final POS pos) {
    return new Iterable<Word>() {
      public Iterator<Word> iterator() {
        return new LookaheadIterator<Word>(new WordIterator(pos));
      }
    };
  }
  
  /** 
   * @see DictionaryDatabase#searchWords 
   */
  // TODO don't do this throw NoSuchElementException iterator stuff
  private class SearchIterator implements Iterator<Word> {
    private final POS pos;
    private final String substring;
    private final String filename;
    private int nextOffset = 0;

    SearchIterator(final POS pos, final String substring) {
      this.pos = pos;
      this.substring = Morphy.searchNormalize(substring);
      this.filename = getIndexFilename(pos);
    }
    public boolean hasNext() {
      // meant to be used with LookaheadIterator
      return true;
    }
    public Word next() {
      try {
        final int offset = db.getMatchingLinePointer(filename, nextOffset, substring);
        if (offset >= 0) {
          final Word value = getIndexWordAt(pos, offset);
          nextOffset = db.getNextLinePointer(filename, offset);
          return value;
        } else {
          throw new NoSuchElementException();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    public void remove() {
      throw new UnsupportedOperationException();
    }
  } // end class SearchIterator

  /** {@inheritDoc} */
  public Iterable<Word> searchWords(final POS pos, final String substring) {
    return new Iterable<Word>() {
      public Iterator<Word> iterator() {
        return new LookaheadIterator<Word>(new SearchIterator(pos, substring));
      }
    };
  }
  
  /** 
   * @see DictionaryDatabase#searchIndexBeginning 
   */
  // TODO don't do this throw NoSuchElementException iterator stuff
  private class StartsWithSearchIterator implements Iterator<Word> {
    private final POS pos;
    private final String prefix;
    private final String filename;
    private int nextOffset = 0;
    StartsWithSearchIterator(final POS pos, final String prefix) {
      this.pos = pos;
      this.prefix = prefix;
      this.filename = getIndexFilename(pos);
    }
    public boolean hasNext() {
      // meant to be used with LookaheadIterator
      return true;
    }
    public Word next() {
      try {
        final int offset = db.getMatchingBeginningLinePointer(filename, nextOffset, prefix);
        if (offset >= 0) {
          final Word value = getIndexWordAt(pos, offset);
          nextOffset = db.getNextLinePointer(filename, offset);
          return value;
        } else {
          throw new NoSuchElementException();
        }
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    public void remove() {
      throw new UnsupportedOperationException();
    }
  } // end class StartsWithSearchIterator
  
  /** {@inheritDoc} */
  public Iterable<Word> searchIndexBeginning(final POS pos, final String prefix) {
    return new Iterable<Word>() {
      public Iterator<Word> iterator() {
        return new LookaheadIterator<Word>(new StartsWithSearchIterator(pos, prefix));
      }
    };
  }

  /** 
   * @see DictionaryDatabase#synsets 
   */
  // TODO don't do this throw NoSuchElementException iterator stuff
  private class POSSynsetsIterator implements Iterator<Synset> {
    private final POS pos;
    private final String filename;
    private int nextOffset = 0;
    POSSynsetsIterator(final POS pos) {
      this.pos = pos;
      this.filename = getDataFilename(pos);
    }
    public boolean hasNext() {
      // meant to be used with LookaheadIterator
      return true;
    }
    public Synset next() {
      try {
        String line;
        int offset;
        do {
          if (nextOffset < 0) {
            throw new NoSuchElementException();
          }
          line = db.readLineAt(filename, nextOffset);
          offset = nextOffset;
          if (line == null) {
            throw new NoSuchElementException();
          }
          nextOffset = db.getNextLinePointer(filename, nextOffset);
        } while (line.startsWith("  ")); // first few lines start with "  "
        return getSynsetAt(pos, offset, line);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
    public void remove() {
      throw new UnsupportedOperationException();
    }
  } // end class POSSynsetsIterator

  public Iterable<Synset> synsets(final POS pos) {
    return new Iterable<Synset> () {
      public Iterator<Synset> iterator() {
        return new LookaheadIterator<Synset>(new POSSynsetsIterator(pos));
      }
    };
  }
}

