WordNet is a graph.
It is potentially invaluable resource for injecting knowledge into applications.

WordNet is probably the single most used NLP resource ; many companies have it as their cornerstone.
It embodies one of the most fundamental of all NLP problems: "word sense disambiguation".

WordNet consists of enough data to exceed the recommended capacity of Java Collections 
(e.g., java.util.SortedMap<String, X>), but not enough to justify a full relational database.

There are a lot of Java interfaces to WordNet already.
Here are 7 of the Java APIs, along with their URL and software license.
- JAWS (Java API for WordNet Searching) http://lyle.smu.edu/~tspell/jaws/index.html ; non-profit
- Jawbone ; http://sites.google.com/site/mfwallace/jawbone ; MIT license
- JWI (MIT Java Wordnet Interface) ; http://projects.csail.mit.edu/jwi/ ; non-commercial license
- Java WordNet Library http://www.cis.upenn.edu/~dbikel/software.html#wn academic license
- Java WordNet Interface (javawn) http://sourceforge.net/projects/javawn/ ; GPL
- WordNet JNI Java Native Support (WNJN) ; http://wnjn.sourceforge.net/ ; GPL
- JWNL (Java WordNet Library) ; http://sourceforge.net/projects/jwordnet/ ; BSD

Many of the pure Java ones (like Yawni), are derivatives of Oliver Steele's JWordNet.  In fact, 
Yawni is the new name of JWordNet.

Why Yawni ?
- commercial-grade implementation
  - small memory foot print
  - very fast
  - simple, intuitive, and well documented API
  - easy Apache Maven-based build with minimal dependencies
  - extensive unit tests
- includes refined GUI browser featuring
  - user friendly, snappy
  - incremental find
  - good keyboard support
  - cross-platform including zero-install Java Webstart version
- commercial-friendly Apache license

Changes in 2.x versions

- Extreme speed improvements: literally faster than the C version (benchmark source included)
  - FileManager.CharStream and FileManager.NIOCharStream
  - Bloom filter used to avoid fruitless lookups (no loss in accuracy!)
  - re-implemented LRUCache in terms of java.util.LinkedHashMap (simpler; much more efficient)
- Major reduction in memory requirements
  - use of primitives where possible (hidden by API)
  - eliminated unused / unneeded fields
- Implemented Morphy stemming algorithms
- Completely rewritten GUI browser in Java Swing featuring
  - incremental find
  - no limits on search (Never see "Search too large.  Narrow search and try again..." again!)
- Support for WordNet 3.0 data files (and all older formats)
- Supports reading data files from JAR file
- Many bug fixes
  - fixed broken RelationType's
  - fixed Verb example sentences and generic frames (and made them directly accessible)
  - fixed iteration bugs and memory leaks
  - fixed various thread safety bugs
- Updated to leverage Java 1.5
  - generics
  - use of Enum, EnumSet, and EnumMap where apropos
  - re-implemented LRUCache in terms of LinkedHashMap (much more efficient)
  - uses maximally configurable slf4j logging system
  - added LookaheadIterator (analagous to old LookaheadEnumeration)
- Growing suite of unit tests
- Automated all build infrastructure using Apache Maven
- New / changed API methods
  - renamed Word → WordSense, IndexWord → Word, Pointer → Relation, PointerType → RelationType, PointerTarget → RelationTarget
    - easier to understand, agrees with W3C proposal (http://www.w3.org/TR/wordnet-rdf/)
  - WordSense.getSenseNumber()
  - WordSense.getTaggedSenseCount()
  - WordSense.getAdjPosition()
  - WordSense.getVerbFrames()
  - Word.isCollocation()
  - Word.getRelationTypes()
  - Synset.getLexCategory()
  - RelationTarget.getSynset()
  - Word.getSenses() → Word.getSynsets()
  - Word.getWordSenses()
  - WordSense.getTargets() → WordSense.getRelationTarets()
  - DictionaryDatabase iteration methods are Iterables for ease of use (e.g., for loops)
  - all core classes implement Comparable<T>
  - all core classes implement Iterable<WordSense>
  - added iteration for all WordSenses and all Relations (and all of certain RelationType)
  - added support for POS.ALL where apropos
  - all major classes are final
  - currently, no major classes are Serializable
  - removed RMI client / server capabities - deemed overkill 
  - removed applet - didn't justify its maintenance burden

