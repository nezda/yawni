Yawni README file

Introduction / Why Yawni and Why WordNet ?

Yawni is an API to Princeton University's WordNet.  WordNet is a graph; it is a potentially 
invaluable resource for injecting knowledge into applications.  WordNet is
probably the single most used NLP resource ; many companies have it as their
cornerstone.  It embodies one of the most fundamental of all NLP problems:
["word-sense disambiguation"](https://en.wikipedia.org/wiki/Word-sense_disambiguation).  The Yawni code library can be used to add lexical
and semantic knowledge, primarily derived from WordNet, to your applications.  
Yawni is written in the Java programming language.  

The Yawni website is <https://www.yawni.org/>

Yawni currently consists of 3 main modules:

`api/`
  Yawni WordNet API: a pure Java standalone object-oriented interface to the WordNet
  database of lexical and semantic relationships.

`data/`
  Yawni WordNet Data: Jar file containing the Princeton WordNet 3.0 data files, and derivative files to support efficient,
  exhaustive access to this information.

`browser/`
  Yawni WordNet Browser: A GUI browser of WordNet content using the Yawni API.

Quick Start

Basic steps:
0. Install JDK 1.8 (or greater), Apache Maven 3.0.3 (or greater)
1. Specify the following Apache Maven dependencies in your project
    
    ```xml
    <dependency>
      <groupId>org.yawni</groupId>
      <artifactId>yawni-wordnet-api</artifactId>
      <version>2.0.0-SNAPSHOT</version>
    </dependency>
    <dependency>
      <groupId>org.yawni</groupId>
      <artifactId>yawni-wordnet-data30</artifactId>
      <version>2.0.0-SNAPSHOT</version>
    </dependency>
    ```
    
2. Start using the Yawni API!: all required resources are loaded on demand
     from the classpath (i.e., jars) made accessible via a singleton:
     
    ```java
    final WordNetInterface wn = WordNet.getInstance();
    ```

   Numerous unit tests serve as great executable examples are included 
   in `api/src/test/`.  For a more complex example application, check 
   out the `browser/` sub-module.
     
Yet Another WordNet Interface !? (get it?)

WordNet consists of enough data to exceed the recommended capacity of Java Collections 
(e.g., `java.util.SortedMap<String, X>`), but not enough to justify a full relational database.

There are a lot of Java interfaces to WordNet already.
Here are 8 of the Java APIs, along with their URL and software license.
- JAWS (Java API for WordNet Searching) <http://lyle.smu.edu/~tspell/jaws/index.html>; non-profit
- Jawbone ; <http://sites.google.com/site/mfwallace/jawbone>; MIT license
- JWI (MIT Java Wordnet Interface) ; <http://projects.csail.mit.edu/jwi/>; non-commercial license
- Java WordNet Library <http://www.cis.upenn.edu/~dbikel/software.html#wn> academic license
- Java WordNet Interface (javawn) <http://sourceforge.net/projects/javawn/>; GPL
- WordNet JNI Java Native Support (WNJN) ; <http://wnjn.sourceforge.net/> ; GPL
- JWNL (Java WordNet Library) ; <http://sourceforge.net/projects/jwordnet/>; BSD
- extJWNL (Extended Java WordNet Library) ; <http://sourceforge.net/projects/extjwnl/>; BSD

Many of the pure Java ones (like Yawni), are actually derivatives of Oliver Steele's original JWordNet.  
In fact, Yawni is the new name of that original Java WordNet, JWordNet.

Why Yawni ?
- commercial-grade implementation
  - very fast & small memory foot print
  - pure Java implementation
  - facilitates access to all aspects of WordNet data and algorithms including "Morphy" morphological processing (i.e., stemming) routines
  - simple, intuitive, and well documented API
  - all required resources can load from jars making deployment a snap
  - all query results are immutable ; safely cached and/or accessed by concurrent threads
  - easy Apache Maven-based build with minimal dependencies
  - extensive unit tests provide peace of mind (and great examples!)
- includes refined GUI browser featuring
  - user friendly, snappy
  - incremental find (Ctrl+Shift+F / ⇧ ⌘ F)
  - comprehensive keyboard support (arrows, tab, etc.)
  - multi-window support (Ctrl+N / ⌘ N)
  - cross-platform including zero-install Java Web Start version
- commercial-friendly Apache license

Changes in 2.x versions

- Extreme speed improvements: literally faster than the C version (benchmark source included)
  - Bloom filters used to avoid fruitless lookups (no loss in accuracy!)
  - re-implemented `LRUCache` using Google Guava's `MapMaker`
  - `FileManager.CharStream` and `FileManager.NIOCharStream` utilize in-memory and `java.nio` for maximum speed
- Major reduction in memory requirements
  - use of primitives where possible (hidden by API)
  - eliminated unused / unneeded fields
- Implemented `Morphy` stemming algorithms
- Completely rewritten GUI browser in Java Swing featuring
  - incremental find
  - no limits on search (Never see "Search too large.  Narrow search and try again..." again!)
- Support for WordNet 3.0 data files (and all older formats)
- Support for numerous optional and extended WordNet resources
  - 'sense tagged frequencies' (`WordSense.getSensesTaggedFrequency()`)
  - 'lexicographer category' (`Synset.getLexCategory()`)
  - 14 new 'morphosemantic' relations (`RelationType.RelationTypeType.MORPHOSEMANTIC`)
  - 'evocation' empirical ranks (`WordSense.getCoreRank()`)
- Supports reading ALL data files from JAR file
- Many bug fixes
  - fixed broken `RelationType`s
  - fixed Verb example sentences and generic frames (and made them directly accessible)
  - fixed iteration bugs and memory leaks
  - fixed various thread safety bugs
- Updated to leverage Java 1.6
  - generics
  - use of `Enum`, `EnumSet`, and `EnumMap` where apropos
  - uses maximally configurable slf4j logging system
  - added `LookaheadIterator` (analogous to old `LookaheadEnumeration`)
    - changed to even better Google Guava `AbstractIterator`
- Growing suite of unit tests
- Automated all build infrastructure using Apache Maven
- New / changed API methods
  - renamed `Word` → `WordSense`, `IndexWord` → `Word`, `Pointer` → `Relation`, `PointerType` → `RelationType`, `PointerTarget` → `RelationTarget`
    - easier to understand, agrees with W3C proposal (<https://www.w3.org/TR/wordnet-rdf/>)
  - `WordSense.getSenseNumber()`
  - `WordSense.getTaggedSenseCount()`
  - `WordSense.getAdjPosition()`
  - `WordSense.getVerbFrames()`
  - `Word.isCollocation()`
  - `Word.getRelationTypes()`
  - `Synset.getLexCategory()`
  - `RelationTarget.getSynset()`
  - `Word.getSenses() → Word.getSynsets()`
  - `Word.getWordSenses()`
  - `WordSense.getTargets()` → `WordSense.getRelationTargets()`
  - `DictionaryDatabase` iteration methods are `Iterable`s for ease of use (e.g., `for` loops)
  - all core classes implement `Comparable<T>`
  - all core classes implement `Iterable<WordSense>`
  - added iteration for all `WordSense`s and all `Relation`s (and all of a certain `RelationType`)
  - added support for `POS.ALL` where apropos
  - all major classes are `final`
  - currently, no major classes are `Serializable`
  - removed RMI client / server capabilities - deemed overkill 
  - removed applet - didn't justify its maintenance burden
