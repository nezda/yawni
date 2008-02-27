/**
 * Java port of morph.c - WordNet search code morphology functions
 */
package edu.brandeis.cs.steele.wn;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import edu.brandeis.cs.steele.util.*;

class Morphy {
  private static final Logger log = Logger.getLogger(Morphy.class.getName());
  static {
    Level level = Level.SEVERE;
    final String altLevel = System.getProperty("edu.brandeis.cs.steele.wn.Morphy.level");
    if(altLevel != null) {
      level = Level.parse(altLevel);
    }
    log.setLevel(level);
  }

  static {
    final Handler handler = new ConsoleHandler();
    handler.setLevel(Level.FINEST);
    handler.setFormatter(new InfoOnlyFormatter());
    //final Handler[] handlers = log.getHandlers();
    //for(final Handler existingHandler : handlers) {
    //  log.removeHandler(existingHandler);
    //}
    log.addHandler(handler);
  }

  private static final String SUFX[] = { 
    // Noun suffixes
  //0    1      2      3      4       5       6      7
    "s", "ses", "xes", "zes", "ches", "shes", "men", "ies",
    // Verb suffixes
  //8    9      10    11    12    13    14     15
    "s", "ies", "es", "es", "ed", "ed", "ing", "ing",
    // Adjective suffixes
  //15    16     17    18
    "er", "est", "er", "est"
  };

  private static final String ADDR[] = { 
    // Noun endings
  //0   1    2    3    4     5     6      7
    "", "s", "x", "z", "ch", "sh", "man", "y",
    // Verb endings
  //8   9    10   11  12   13  14   15
    "", "y", "e", "", "e", "", "e", "",
    // Adjective endings
  //15  16  17   18
    "", "", "e", "e",
  };

  // from wn.h
  //
  // NOUN 1
  // VERB 2
  // ADJ 3
  // ADV 4
  // SATELLITE 5
  
  // offsets and counts into SUFX and ADDR (0 not used since NOUN == 1)
                                 //0  1  2  3  
  private static int offsets[] = { 0, 0, 8, 16 };
  private static int cnts[] =    { 0, 8, 8, 4 };

  private static final String PREPOSITIONS[] = {
    "to",
    "at",
    "of",
    "on",
    "off",
    "in",
    "out",
    "up",
    "down",
    "from",
    "with",
    "into",
    "for",
    "about",
    "between",
  };

  private final FileBackedDictionary dictionary;
  private Cache morphyCache; 

  Morphy(final FileBackedDictionary dictionary) {
    this.dictionary = dictionary;
    morphyCache = new LRUCache(dictionary.DEFAULT_CACHE_CAPACITY);
  }

  private String searchNormalize(String origstr) {
    if (log.isLoggable(Level.FINEST)) {
      log.log(Level.FINEST, "origstr: "+origstr);
    }

    //TODO optimize this
    final int underscore = origstr.indexOf('_');
    final int dash = origstr.indexOf('-');
    final int space = origstr.indexOf(' ');
    if (underscore >= 0 || dash >= 0 || space >= 0) {
      // fix edge underscores (e.g. "_slovaks_")
      origstr = origstr.replaceAll("^[_ -]+", "");
      origstr = origstr.replaceAll("[_ -]+$", "");
    }
    return origstr.toLowerCase().replaceAll("\\s+", "_");
  }
  
  /** 
   * Try to find baseform (lemma) of word or collocation in POS.
   * Unlike the original, returns <b>all</b> baseforms of origstr. 
   * Converts '_' to ' '.
   * morph.c morphstr() function.  
   *
   * <b>The original function returned nothing for words which were already
   * stemmed.</b>
   *
   * Alorithm:
   * - normalize search string to database format
   * - if search string in exception list, add distinct exceptional variants
   * - if pos != verb, add any distinct base forms
   * - if pos == verb and search string is multiword phrase with preposition,
   *     add distinct morphprep variant if there is one
   * - else if no variants found yet
   *     for each word in the collocation, build up search string with 
   *     that word's stem if it has one, or the original string splicing
   *     the provided token separators back in (e.g. "_", "-")
   *     - add any variants
   *
   * TODO simplify this code - is a brute force port from tricky C code.
   * Consider Java idioms like StringTokenizer/Scanner.
   */
  List<String> morphstr(final String origstr, POS pos) {
    if (pos == POS.SAT_ADJ) {
      pos = POS.ADJ;
    }

    final FileBackedDictionary.DatabaseKey cacheKey = new FileBackedDictionary.StringPOSDatabaseKey(origstr, pos);
    final Object cached = morphyCache.get(cacheKey);
    if (cached != null) {
      //FIXME doesn't cache null (combinations not in WordNet)
      return (List<String>)cached;
    }
    
    // Assume string hasn't had spaces substituted with '_'
    final String str = searchNormalize(origstr);
    if (str.length() == 0) {
      return Collections.emptyList();
    }
    int wordCount = countWords(str, '_');
    if (log.isLoggable(Level.FINEST)) {
      log.finest("origstr: "+origstr+" wordCount: "+wordCount);
    }
    //XXX what does 'svcnt' stand for? state variable...count...
    //XXX what does 'svprep' stand for? state variable...preposition...
    int svcnt = 0;
    int svprep = 0;

    boolean phase1Done = false;
    //TODO no need to allocate this if we don't use it
    final List<String> toReturn = new ArrayList<String>();

    // first try exception list
    String[] tmp = dictionary.getExceptions(str, pos);
    if (tmp != null && tmp.length != 0 && tmp[1].equalsIgnoreCase(str) == false) {
      // force next time to pass null
      svcnt = 1;
      // add variants from exception list
      // verb.exc line "saw see"
      //  e.g. input: "saw" output: "see", "saw"
      //ONLY root: toReturn.add(clean(tmp[1]));
      for(int i=tmp.length - 1; i>=0; --i) {
        toReturn.add(clean(tmp[i]));
      }
      phase1Done = true;
    }

    // Then try simply morph on original string
    if (phase1Done == false &&
        pos != POS.VERB && 
        null != (tmp = morphword(str, pos)) /*&& 
        tmp[0].equalsIgnoreCase(str) == false*/) {
      if (log.isLoggable(Level.FINER)) {
        log.finer("Morphy hit str: "+str+" tmp[0]: "+tmp[0]+
            " tmp.length: "+tmp.length+" tmp: "+Arrays.toString(tmp));
      }
      // use this knowledge and base forms to add all case variants
      // on morphs in tmp
      final IndexWord indexWord = is_defined(tmp[0], pos);
      addTrueCaseLemmas(indexWord, toReturn);
      
      phase1Done = true;
    }
    
    IndexWord indexWord = null;

    int prep;
    if (phase1Done == false && 
        pos == POS.VERB && wordCount > 1 && 
        (prep = hasprep(str, wordCount)) != 0) {
      // assume we have a verb followed by a preposition
      svprep = prep;
      final String tmp1 = morphprep(str);
      if (tmp1 != null) {
        if (log.isLoggable(Level.FINER)) {
          log.finer("origstr: "+origstr+" tmp1: "+tmp1);
        }
        toReturn.add(clean(tmp1));
      }
      phase1Done = true;
    } else if (phase1Done == false) {
      svcnt = wordCount = countWords(str, '-');
      if (log.isLoggable(Level.FINER)) {
        log.finer("origstr: \""+origstr+
            "\" str: \""+str+"\" wordCount: "+wordCount+" "+pos);
      }
      int st_idx = 0;
      String word = null;
      String searchstr = "";
      // LN loop over '-' and '_' chunked "tokens"
      while (--wordCount != 0) {
        final int end_idx1 = str.indexOf('_', st_idx);
        final int end_idx2 = str.indexOf('-', st_idx);
        int end_idx = -1;
        String append;
        if (end_idx1 > 0 && end_idx2 > 0) {
          // LN remainder contains dashes and underscores
          if (end_idx1 < end_idx2) {
            end_idx = end_idx1;
            append = "_";
          } else {
            end_idx = end_idx2;
            append = "-";
          }
        } else {
          if (end_idx1 > 0) {
            // was an underscore, keep it?
            end_idx = end_idx1;
            append = "_";
          } else {
            // was a space, so try a "-" in its place
            end_idx = end_idx2;
            append = "-";
          }
        }
        assert append != null;
        if (end_idx < 0) { 
          // XXX shouldn't do this
          assert str.equals("_");
          //assert false : "word: \""+word+"\" str: \""+str+"\" wordCount: "+wordCount+" end_idx: "+end_idx;
          phase1Done = true;
          break;           
        }
        word = str.substring(st_idx, end_idx);
        if (log.isLoggable(Level.FINER)) {
          log.finer("word: \""+word+"\" str: \""+str+"\" wordCount: "+wordCount);
        }

        tmp = morphword(word, pos);
        if (tmp != null) {
          if (tmp.length != 1) {
            if (log.isLoggable(Level.WARNING)) {
              log.warning("losing colloc word variant?: "+Arrays.toString(tmp));
            }
          }
          searchstr += tmp[0];
        } else {
          if (log.isLoggable(Level.FINER)) {
            log.log(Level.FINER, "word: \""+word+"\", "+pos+" returned null. searchstr: \""+searchstr+"\"");
          }
          assert word != null;
          searchstr += word;
        }
        assert append != null;
        searchstr += append;
        st_idx = end_idx + 1;
      } // end multi-word loop

      // if 'word' is null, there was only 1 word in origstr
      if (word == null) {
        // happens for all verbs ?
        //DEBUG System.err.println("word is null?: origstr: "+origstr);
        word = "";
      }

      if (log.isLoggable(Level.FINER)) {
        log.log(Level.FINER, "word: \""+word+"\" str.substring(st_idx): \""+
            str.substring(st_idx)+"\" st_idx: "+st_idx+" "+pos);
      }
      
      // assertions: 
      // if single-word, st_idx == 0, queryStr == original string and word == original string and searchStr == ""
      // else st_idx = s.length() and queryStr == "" and queryStr =="" and searchstr == (possibly) stemmed items collocation
      final String queryStr = word = str.substring(st_idx);

      // this will trivially return null if queryStr == ""
      final String[] morphWords = morphword(queryStr, pos);

      assert searchstr != null;
      
      if (morphWords != null) {
        if (morphWords.length != 1) {
          if (log.isLoggable(Level.WARNING)) {
            log.warning("losing variant morphWords[]?: "+Arrays.toString(morphWords));
          }
        }
        assert searchstr != null;
        if (morphWords.length > 0) {
          assert morphWords[0] != null;
          searchstr += morphWords[0];
        } else {
          if (log.isLoggable(Level.WARNING)) {
            log.warning("morphWords = []: searchstr: "+searchstr);
          }
        }
      } else {
        // morphWords is null
        assert searchstr != null;
        assert word != null;
        //LN is this adding the last word of the collocation ?
        searchstr += word;
      }
      //XXX System.err.println("searchstr: "+searchstr+" morphWords: "+(morphWords != null ? Arrays.toString(morphWords) : "null"));

      //XXX System.err.println("searchstr: "+searchstr+" str: "+str+" "+pos+
      //XXX     " is_defined(searchstr, pos): "+is_defined(searchstr, pos)+
      //XXX     " morphWords: "+(morphWords != null ? Arrays.toString(morphWords) : "null")+
      //XXX     " toReturn: "+toReturn);
      indexWord = null;
      if (searchstr.equalsIgnoreCase(str) == false && null != (indexWord = is_defined(searchstr, pos))) {
        //debug(Level.FINER, "indexWord for (\""+searchstr+"\", "+pos+"): "+indexWord);
        toReturn.add(getTrueCaseLemma(indexWord, pos));
      }
      phase1Done = true;
    }
    
    assert phase1Done;

    //
    // start phase2+
    //
    
    // in C code, executed on subsequent calls for same query string

    if (svprep > 0) {           
      // if verb has preposition, no more morphs
      //assert toReturn.isEmpty() == false; // we should already have added 1 thing right ?
      svprep = 0;
    } else if (svcnt == 1) {
      //assert toReturn.isEmpty() == false; // we should already have added 1 thing right ?
      tmp = dictionary.getExceptions(str, pos);
      for (int i=1; tmp != null && i<tmp.length; ++i) {
        toReturn.add(tmp[i]);
      }
    } else {
      svcnt = 1; // LN pushes us back to above case (for subsequent calls) all this is destined for death anyway
      assert str != null; 
      tmp = dictionary.getExceptions(str, pos);
      if (tmp != null && tmp.length != 0 && tmp[1].equalsIgnoreCase(str) == false) {
        for (int i=1; i < tmp.length; ++i) {
          toReturn.add(tmp[i]);
        }
      }
    }
    // always include full length exact matches
    indexWord = is_defined(str, pos);
    if (indexWord != null) {
      addTrueCaseLemmas(indexWord, toReturn);
    }
    final List<String> uniqed = uniq(toReturn);
    morphyCache.put(cacheKey, uniqed);
    if (log.isLoggable(Level.FINER)) {
      log.finer("returning "+toReturn+" for origstr: \""+origstr+"\" "+pos+" str: "+str);
    }
    return uniqed;
  }

  private void addTrueCaseLemmas(final IndexWord indexWord, final List<String> lemmas) {
    for(final Word word : indexWord.getSenses()) {
      // lemma's are already "cleaned"
      lemmas.add(word.getLemma());
    }
  }

  //TODO FIXME XXX
  // - this does not always produce unique answers - remove it and handle it all
  //   in 1 spot in morphstr - not in morphword() or morphprep()
  // - revisit all equalsIgnoreCase() checks in here
  @Deprecated
  private String getTrueCaseLemma(final IndexWord indexWord, final POS pos) {
    // LN dumb, wrong, default impl
    //return indexWord.getLemma();
    
    // heuristic: pick any Word in the given POS and return its lemma
    final Synset synsets[] = indexWord.getSynsets();
    for(final Synset sense : synsets) {
      if (sense.getPOS() == pos) {
        //System.err.println("getTrueCaseLemma("+indexWord+", "+pos+"): "+sense.getWord(0).getLemma()+
        //    " num Words: "+sense.getWords().length);
        for(final Word word : sense.getWords()) {
          //System.err.println("  "+word);
          if (word.getLemma().equalsIgnoreCase(indexWord.getLemma())) {
            //System.err.println("indexWord: "+indexWord+" lemma: "+word.getLemma());
            //new Exception().printStackTrace();
            return clean(word.getLemma());
          }
        }
      }
    }
    throw new IllegalStateException(indexWord+" in "+pos+" not found among "+
        synsets.length+" synsets?");
  }

  static String clean(final String s) {
    if (s == null) return s;
    return s.replace('_', ' ');
  }

  /**
   * Must be an exact match in the dictionary.
   * (C version in search.c only returns true/false)
   */
  private IndexWord is_defined(final String lemma, final POS pos) {
    if (lemma.length() == 0) return null;
    return dictionary.lookupIndexWord(pos, lemma);
  }

  private static final String[] NO_STRINGS = new String[0];

  /**
   * Try to find baseform (lemma) of <b>individual word</b> <param>word</param>
   * in POS <param>pos</param>.
   * Port of morph.c morphword().
   */
  private String[] morphword(final String word, final POS pos) {
    if (word == null || word.length() == 0) {
      return null;
    }
    // first look for word on exception list
    final String[] tmp = dictionary.getExceptions(word, pos);
    if (tmp != null && tmp.length != 0) {
      // found it in exception list
      // LN skips first one because of modified getExceptions semantics
      final String[] rest = new String[tmp.length - 1];
      System.arraycopy(tmp, 1, rest, 0, rest.length);
      if (log.isLoggable(Level.FINER)) {
        log.finer("getExceptions returning: "+Arrays.toString(rest));
      }
      return rest;
    }

    if (pos == POS.ADV) {     
      // only use exception list for adverbs
      return null;
    }

    String tmpbuf = null;
    String end = "";
    if (pos == POS.NOUN) {
      if (word.endsWith("ful")) {
        tmpbuf = word.substring(0, word.length() - "ful".length());
        end = "ful";
        //special case for *ful "boxesful" -> "boxful"
      } else if (word.length() <= 2 || word.endsWith("ss")) {
        // check for noun ending with 'ss' or short words
        return null;
      }
    }

    // If not in exception list, try applying rules from tables

    if (tmpbuf == null) {
      tmpbuf = word;
    }

    final int offset = offsets[pos.getWordNetCode()];
    final int cnt = cnts[pos.getWordNetCode()];
    String[] toReturn = null;
    String lastRetval = null;
    for(int i = 0; i < cnt; i++) {
      final String retval = wordbase(tmpbuf, (i + offset));
      if (lastRetval != null) {
        // LN added a little caching
        if (lastRetval.equals(retval)) {
          continue;
        }
      } else {
        lastRetval = retval;
      }
      IndexWord indexWord;
      if (retval.equals(tmpbuf) == false && null != (indexWord = is_defined(retval, pos))) {
        if (toReturn == null) {
          toReturn = new String[]{ getTrueCaseLemma(indexWord, pos) };
        } else {
          // not common to have > 1
          final String nextWord = getTrueCaseLemma(indexWord, pos);
          if (nextWord.equals(last(toReturn))) {
            // don't need to store this duplicate
            continue;
          }
          final String[] newToReturn = new String[toReturn.length + 1];
          System.arraycopy(toReturn, 0, newToReturn, 0, toReturn.length);
          newToReturn[toReturn.length] = nextWord;
          toReturn = newToReturn;
          if (log.isLoggable(Level.FINER)) {
            log.finer("returning: \""+retval+"\"");
          }
        }
        return new String[]{ retval + end };
      }
    }
    return toReturn;
  }

  private static <T> T last(T[] ts) {
    if(ts == null || ts.length == 0) {
      return null;
    } else {
      return ts[ts.length - 1];
    }
  }

  /**
   * Port of morph.c wordbase().
   */
  private String wordbase(final String word, final int enderIdx) {
    if (log.isLoggable(Level.FINEST)) {
      log.finest("word: "+word+" enderIdx: "+enderIdx);
    }
    if (word.endsWith(SUFX[enderIdx])) {
      return word.substring(0, word.length() - SUFX[enderIdx].length()) + ADDR[enderIdx];
    }
    return word;
  }

  /**
   * Find a preposition in the verb string and return its
   * corresponding word number. 
   * Port of morph.c hasprep().
   */
  private int hasprep(final String s, final int wdcnt) {
    if (log.isLoggable(Level.FINER)) {
      log.finer("s: "+s+" wdcnt: "+wdcnt);
    }
    for (int wdnum = 2; wdnum <= wdcnt; ++wdnum) {
      int startIdx = s.indexOf('_');
      assert startIdx >= 0;
      ++startIdx; // bump past '_'
      for(final String prep : PREPOSITIONS) {
        // LN if matches a known preposition on a word boundary
        if (s.regionMatches(startIdx, prep, 0, prep.length()) &&
           ( startIdx + prep.length() == s.length() ||
             s.charAt(startIdx + prep.length()) == '_' )
           ) {
          if (log.isLoggable(Level.FINER)) {
            log.finer("s: "+s+" has prep \""+prep+"\" @ word "+wdnum);
          }
          return wdnum;
        }
      }
    }
    log.finer("s contains no prep");
    return 0;
  }

  /** 
   * Note: all letters in <param>s</param> are lowercase.
   * Port of morph.c morphprep()
   */
  private String morphprep(final String s) {
    // Assume that the verb is the first word in the phrase.  Strip it
    // off, check for validity, then try various morphs with the
    // rest of the phrase tacked on, trying to find a match.

    String[] lastwd = null;
    String end = null;
    final int rest = s.indexOf('_');
    final int last = s.lastIndexOf('_');
    if (rest != last) {           
      // implies more than 2 words (required for prepositional phrase)
      if (lastwd != null) {
        // last word found as a NOUN
        checkLosingVariants(lastwd, "morphprep()");
        // end = s[2:-1*] * noun stemmed form of last word
        end = s.substring(rest, last) + lastwd[0];
      }
    }

    final String firstWord = s.substring(0, rest);
    if(isPossibleVerb(firstWord) == false) {
      return null;
    }

    // First try to find the verb (which were are assuming is the first word) in
    // the exception list

    String retval = null;
    final String[] exc_words = dictionary.getExceptions(firstWord, POS.VERB);
    if (log.isLoggable(Level.FINER) && 
        exc_words != null && exc_words.length > 0 && exc_words[1].equals(firstWord) == false) {
      log.finer("exc_words "+Arrays.toString(exc_words)+
          " found for firstWord \""+firstWord+"\" but exc_words[1] != firstWord");
    }
    if (exc_words != null && exc_words.length != 0 && exc_words[1].equals(firstWord) == false) {
      if (exc_words.length != 2) {
        if (log.isLoggable(Level.WARNING)) {
          log.warning("losing exception list variant(s)?!: "+Arrays.toString(exc_words));
        }
      }
      retval = exc_words[1] + s.substring(rest);
      IndexWord indexWord;
      if (null != (indexWord = is_defined(retval, POS.VERB))) {
        if (log.isLoggable(Level.FINER)) {
          log.finer("returning indexWord "+indexWord);
        }
        return getTrueCaseLemma(indexWord, POS.VERB);
      } else if (lastwd != null) {
        assert end != null;
        retval = exc_words[1] + end;
        if (null != (indexWord = is_defined(retval, POS.VERB))) {
          if (log.isLoggable(Level.FINER)) {
            log.finer("returning indexWord "+indexWord);
          }
          return getTrueCaseLemma(indexWord, POS.VERB);
        }
      }
    }

    final int offset = offsets[POS.VERB.getWordNetCode()];
    final int cnt = cnts[POS.VERB.getWordNetCode()];
    for (int i = 0; i < cnt; ++i) {
      final String exc_word = wordbase(firstWord, (i + offset));
      if (exc_word != null && exc_word.equals(firstWord) == false) { 
        // ending is different
        retval = exc_word + s.substring(rest);
        if (log.isLoggable(Level.FINER)) {
          log.finer("test retval "+retval);
        }
        IndexWord indexWord;
        if (null != (indexWord = is_defined(retval, POS.VERB))) {
          if (log.isLoggable(Level.FINER)) {
            log.finer("returning indexWord "+indexWord);
          }
          return getTrueCaseLemma(indexWord, POS.VERB);
        } else if (lastwd != null) {
          retval = exc_word + end;
          if (null != (indexWord = is_defined(retval, POS.VERB))) {
            if (log.isLoggable(Level.FINER)) {
              log.finer("returning indexWord "+indexWord);
            }
            return getTrueCaseLemma(indexWord, POS.VERB);
          }
        }
      }
    }
    retval = firstWord + s.substring(rest);
    if (false == s.equals(retval)) {
      // this makes no senses -- copied from morph.c
      return retval;
    }
    if (lastwd != null) {
      retval = firstWord + end;
      if (false == s.equals(retval)) {
        return retval;
      }
    }
    return null;
  }

  private void checkLosingVariants(final String[] words, final String message) {
    if (words != null && words.length != 1) {
      if (log.isLoggable(Level.WARNING)) {
        log.warning(message+" losing variants!: "+Arrays.toString(words));
      }
    }
  }

  private boolean isPossibleVerb(final CharSequence word) {
    for (int i = 0, cnt = word.length(); i < cnt; i++) {
      if (Character.isLetterOrDigit(word.charAt(i)) == false &&
          word.charAt(i) != '-') {
        // added minor extension to allow verbs containing dashes
        return false;
      }
    }
    return true;
  }

  /** 
   * Count the number of words in a string delimited by space, underscore or
   * the passed in separator.
   * Port of wnutil.c cntwords().
   */
  static int countWords(final String s, final char separator) {
    int wdcnt = 0;
    int i = 0;
    final int len = s.length();
    while (i < len) {
      if (s.charAt(i) == separator || s.charAt(i) == ' ' || s.charAt(i) == '_') {
        wdcnt++;
        while (i < len && 
          (s.charAt(i) == separator || s.charAt(i) == ' ' || s.charAt(i) == '_')) {
          ++i;
        }
      } else {
        ++i;
      }
    }
    return ++wdcnt;
  }

  // LN getindex() is only 1 form of sloppy search (periods, hyphens, underscores

  //private static final int WORDBUF = 256;
  //private static final int MAX_FORMS = 5;

  ///** 'smart' search of index file.  Find word in index file, trying different
  // * techniques - replace hyphens with underscores, replace underscores with
  // * hyphens, strip hyphens and underscores, strip periods. 
  // * search.c's getindex() function
  // */
  //private IndexWord[] getindex(String searchstr, POS pos) {
  //  static IndexWord offsets[MAX_FORMS];
  //  static int offset;
  //  // FIXME if pos == POS.SAT_ADJ , pos = POS.ADJ

  //  char strings[MAX_FORMS][WORDBUF]; // vector of search strings

  //  // This works like strrok(): if passed with a non-null string,
  //  // prepare vector of search strings and offsets.  If string
  //  // is null, look at current list of offsets and return next
  //  // one, or null if no more alternatives for this word.

  //  if (searchstr != null) {
  //    offset = 0;
  //    strtolower(searchstr);
  //    for (int i = 0; i < MAX_FORMS; ++i) {
  //      strcpy(strings[i], searchstr);
  //      offsets[i] = 0;
  //    }

  //    strsubst(strings[1], '_', '-');
  //    strsubst(strings[2], '-', '_');

  //    // remove all spaces and hyphens from last search string, then
  //    // all periods
  //    for (int i = 0, j = 0, k = 0; i < searchstr.length(); ++i) {
  //      final char c = searchstr.get(i);
  //      if (c != '_' && c != '-') {
  //        strings[3][j++] = c;
  //      }
  //      if (c != '.') {
  //        strings[4][k++] = c;
  //      }
  //    }
  //    strings[3][j] = '\0';
  //    strings[4][k] = '\0';

  //    // Get offset of first entry.  Then eliminate duplicates
  //    // and get offsets of unique strings.

  //    if (strings[0][0] != null) {
  //      offsets[0] = index_lookup(strings[0], dbase);
  //    }

  //    for (i = 1; i < MAX_FORMS; i++) {
  //      if ((strings[i][0]) != NULL && (strcmp(strings[0], strings[i]))) {
  //        offsets[i] = index_lookup(strings[i], dbase);
  //      }
  //    }
  //  }

  //  for (int i = offset; i < MAX_FORMS; ++i) {
  //    if (offsets[i]) {
  //      offset = i + 1;
  //      return offsets[i];
  //    }
  //  }

  //  return null;
  //}

  /**
   * Remove duplicates from <param>list</param>.
   * Assumes <param>list</param> contains no null elements and is short (brute
   * force algorithm).
   */
  private static <T> List<T> uniq(List<T> list) {
    //log.warning("input list: "+list+" list.size(): "+list.size());
    if (list == null || list.size() <= 1) {
      return list;
    }
    int n = list.size();
    for(int i=0; i<n; ++i) {
      final T ith = list.get(i);
      for(int j=i+1; j<n; ++j) {
        final T jth = list.get(j);
        if (ith.equals(jth)) {
          // overwrite jth with n-1th and decrement n and j
          list.set(j, list.get(n-1));
          --n;
          --j;
        }
      }
    }
    if (n < list.size()) {
      // TODO new list would plug small memory leak here
      list = list.subList(0, n);
    }
    //final Set<T> set = new HashSet<T>(list);
    //log.warning("output list: "+list+" list.size(): "+list.size());
    return list;
  }
}
