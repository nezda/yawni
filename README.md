[![Build Status](https://github.com/nezda/yawni/workflows/Java%20CI/badge.svg?branch=main)](https://github.com/nezda/yawni/actions)

## Introduction / Why Yawni and Why WordNet ?
<img alt="Yawning!" align="right" src=".assets/yawni-logo.png">

Yawni is an <abbr title="Application Programming Interface">API</abbr> to Princeton University's [WordNet®](https://wordnet.princeton.edu/). WordNet is a graph; it is a potentially invaluable resource for injecting knowledge into applications. WordNet is probably the single most used <abbr title="Natural Language Processing">NLP</abbr> resource ; many companies have it as their cornerstone.  It embodies one of the most fundamental of all NLP problems: ["word-sense disambiguation"](https://en.wikipedia.org/wiki/Word-sense_disambiguation). The Yawni code library can be used to add lexical and semantic knowledge, primarily derived from WordNet, to your applications.

Yawni is written in the Java programming language.  

The Yawni website is <https://www.yawni.org/>

Yawni currently consists of 3 main modules:

* [`api/`](https://github.com/nezda/yawni/tree/main/api)
  Yawni WordNet API: a pure Java standalone object-oriented interface to the WordNet
  database of lexical and semantic relationships.

* [`data*/`](https://github.com/nezda/yawni/tree/main/data30)
  Yawni WordNet Data: Jar file containing the Princeton WordNet 3.0 data files, and derivative files to support efficient,
  exhaustive access to this information.

* [`browser/`](https://github.com/nezda/yawni/tree/main/browser)
  Yawni WordNet Browser: A GUI browser of WordNet content using the Yawni API.

## 🚀 Quick Start

### Basic steps 👣
0. Install JDK 8 (or greater), Apache Maven 3.0.3 (or greater)
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
    WordNetInterface wn = WordNet.getInstance();
```

   Numerous unit tests that serve as great executable examples are included 
   in [`api/src/test/java/org/yawni/`](https://github.com/nezda/yawni/tree/main/api/src/test/java/org/yawni/).  For a more complex example application, check 
   out the [`browser/`](https://github.com/nezda/yawni/tree/main/browser) sub-module.
     
## <ins>Y</ins>et <ins>A</ins>nother <ins>W</ins>ord<ins>N</ins>et <ins>I</ins>nterface !?

WordNet consists of enough data to exceed the recommended capacity of Java Collections 
(e.g., `java.util.SortedMap<String, X>`), but not enough to justify a full relational database.

There are a lot of Java interfaces to WordNet already.
Here are 8 of the Java APIs, along with their URL and software license.
- Dr. Dan Bikel / Stanford NLP WordNet <https://nlp.stanford.edu/nlp/javadoc/wn/doc/> ; [“Academic User”](https://nlp.stanford.edu/nlp/javadoc/wn/LICENSE)
- JAWS (Java API for WordNet Searching) <https://github.com/jaytaylor/jaws>; BSD-2-Clause License
- Jawbone ; <https://sites.google.com/site/mfwallace/jawbone>; MIT license
- JWI (MIT Java Wordnet Interface) ; <https://projects.csail.mit.edu/jwi/>; non-commercial license
- Java WordNet Interface (javawn) <https://sourceforge.net/projects/javawn/>; GPL 2.0
- WordNet JNI Java Native Support (WNJN) ; <http://wnjn.sourceforge.net/> ; GPL 2.0
- JWNL (Java WordNet Library) ; <https://sourceforge.net/projects/jwordnet/>; BSD
- extJWNL (Extended Java WordNet Library) ; <https://sourceforge.net/projects/extjwnl/>; BSD

Many of the pure Java ones (like Yawni), are actually derivatives of [Oliver Steele](https://osteele.com/) 's original JWordNet. In fact, **Yawni** *is* the _“new”_ name of that original Java WordNet, JWordNet.

# Why Yawni ?
- commercial-grade implementation
  - 🚀 very fast & small memory footprint 👣
  - pure Java ☕ so it’s compatible with any JVM language! Scala, Clojure, Kotlin, …
  - facilitates access to all aspects of WordNet data and algorithms including "Morphy" morphological processing (i.e., lemmatization, i.e., stemming) routines
  - simple, intuitive, and well documented 📚 API
  - all required resources load from jars by default making deployment a snap 💥
  - all query results are immutable 🔒; safe for used in caches and/or accessed by concurrent threads
  - easy Apache Maven-based build with minimal dependencies
  - extensive unit tests 🧪 provide peace of mind (and great examples!)
- includes refined GUI browser featuring
  - user-friendly 😊 🎛 🔍 & snappy 🚀
  - incremental find 🔍 (Ctrl+Shift+F / ⌘ ⇧ F)
  - no limits on search: Never see “Search too large. Narrow search and try again...” again!  
  - comprehensive keyboard navigation ⌨ 🧭 support (arrows ⇦ ⇨ ⇧ ⇩, tab ↹, etc.)
  - multi-window 🪟🪟 support (Ctrl+N / ⌘ N)
  - cross-platform 🔀 including zero-install Java Web Start version
- commercial-friendly Apache license

#### Changes in 2.x versions

- Extreme speed improvements: literally faster than the C version (benchmark source included)
  - Bloom filters used to avoid fruitless lookups (no loss in accuracy!)
  - re-implemented `LRUCache` using Google Guava's `MapMaker`
  - `FileManager.CharStream` and `FileManager.NIOCharStream` utilize in-memory and `java.nio` for maximum speed
- Major reduction in memory requirements
  - use of primitives where possible (hidden by API)
  - eliminated unused / unneeded fields
- Implemented `Morphy` stemming / lemmatization algorithms
- Completely rewritten GUI browser in Java Swing featuring
  - incremental find
  - no limits on search: Never see “Search too large. Narrow search and try again...” again!
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
- Updated to leverage Java 1.6 and beyond
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
