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
package org.yawni.wordnet.browser;

import org.yawni.wordnet.Synset;
import org.yawni.wordnet.WordSense;
import org.yawni.wordnet.Word;
import org.yawni.wordnet.POS;
import org.yawni.wordnet.WordNetInterface;
import org.yawni.wordnet.WordNet;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;

import javax.xml.parsers.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;
import org.yawni.wordnet.RelationArgument;
import org.yawni.wordnet.RelationType;

/**
 * Try to use <a href="https://www.w3.org/TR/wordnet-rdf/">https://www.w3.org/TR/wordnet-rdf/</a>
 * - note odd Word/Collocation distinction - seems like this should be optional
 * 
 * Consider XStream (ThoughtWorks)
 * - supports JSON
 *
 * Goals:
 * - common XML serialization that apps / other APIs can be built out of using XSLT
 * - a simple commandline interface (minimum boot time, so RandomAccessFile)
 * - simple JSON REST interface
 * 
 * WordNetQueryResult
 *   queryTerm:"foo",
 *   queryPOS:"NOUN",
 *   queryPointerType:"HYPONYM"
 * 
 *   NounSynsets
 *     NounSynset
 *     NounWordSense
 *   VerbSynsets
 *     VerbSynset
 *     VerbWordSense
 *   AdjectiveSynsets
 *     AdjectiveSynset
 *     AdjectiveSatelliteSynset
 *     AdjectiveWordSense
 *     AdjectiveSatelliteWordSense
 *   AdverbSynsets
 *     AdverbSynset
 *     AdverbWordSense
 */

// goal: factor getDescription(boolean) and getLongDescription(boolean) out of Synset
// and getDescription() and getLongDescription() out of WordSense
//
// cool to specify chain of these to define a panel

class SynsetWriter {
  boolean lexicalFileInfo;
  boolean synsetId;
  boolean senseNumber;
  boolean gloss;
  public Appendable write(Synset synset, Appendable out) {
    for (final WordSense wordSense : synset) {
      new WordSenseWriter().write(wordSense, out);
    }
    return out;
  }
}

class WordSenseWriter {
  boolean senseNumber;
  boolean antonym;
  public Appendable write(WordSense wordSense, Appendable out) {
    // lexicalForm (case sensitive!)
    // senseNumber
    // synset
    return out;
  }
}

class WordWriter {
  public Appendable write(Word word, Appendable out) {
    // lexicalForm
    // pos
    for (final Synset synset : word.getSynsets()) {
      new SynsetWriter().write(synset, out);
    }
    return out;
  }
}

class RelationWriter {
  // Synset → Synset (aka semantic)
  // WordSense → WordSense (aka lexical)
  public Appendable write(Appendable out) {
    // source PointerTarget
    // target PointerTarget
    // RelationType
    return out;
  }
}

public class Searcher {
  public static void test(String[] args) throws Exception {
    final DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
    final DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
    final Document doc = docBuilder.newDocument();

/*
<?xml version="1.0" encoding="UTF-8"?>
<root>
<child name="value">Filler, ... I could have had a foo!</child>
</root>
*/

    final Element root = doc.createElement("root");
    doc.appendChild(root);

    //final Comment comment = doc.createComment("Just a thought");
    //root.appendChild(comment);

    final Element child = doc.createElement("child");
    child.setAttribute("name", "value");
    root.appendChild(child);

    // add a text element to the child
    final Text text = doc.createTextNode("Filler, ... I could have had a foo!");
    child.appendChild(text);

    // Output the XML

    // set up a transformer
    final TransformerFactory transfac = TransformerFactory.newInstance();
    final Transformer trans = transfac.newTransformer();
    //trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
    trans.setOutputProperty(OutputKeys.INDENT, "yes");

    // create string from XML tree
    final StringWriter sw = new StringWriter();
    final StreamResult result = new StreamResult(sw);
    final DOMSource source = new DOMSource(doc);
    trans.transform(source, result);
    final String xmlString = sw.toString();

    System.out.println(xmlString);
  }

  private static void torture(final WordNetInterface wn, final String lemma, final POS pos) {
    final Word word = wn.lookupWord(lemma, pos);
    if (word == null) {
      return;
    }
    for (final Synset synset : word.getSynsets()) {
      // count hypernyms
      System.err.println(lemma+" "+pos.name()+" hypernyms: "+countHypernyms(wn, synset,
          new LinkedHashSet<>()));
    }
  }

  // wnb detects prints cycle for a while, then dumps this to stderr: WordNet library error: Error Cycle detected\ninhibit
  // wordnet online just says: WARNING: The search exceeded the result limit, so the following list is valid but incomplete. Only the top levels of the list are displayed.
  // cycles in hypernyms
  // - control#2V
  //   - restrain#V
  //     - inhibit#1V
  //       - restrain#1V

  // only found a couple Hypernym cycles:
  // 2982 cycle: [Synset 2422663@[POS verb]<verb.social>{restrain, keep, keep back, hold back}]
  // 335 cycle: [Synset 2423762@[POS verb]<verb.social>{inhibit, bottle up, suppress}]

//   hyponym cycles
//   hold
//   reduce
//   wink
//   mortify
//   classify
//   trellis
//   snaffle
//   suppurate

  private static int countHypernyms(final WordNetInterface wn, final RelationArgument child, LinkedHashSet<RelationArgument> path) {
//    System.err.println("child: "+child);
    for (final RelationArgument parent : child.getRelationTargets(RelationType.HYPERNYM)) {
//    for (final RelationArgument parent : child.getRelationTargets(RelationType.HYPONYM)) {
      if (path.contains(parent)) {
        System.err.println("cycle: "+parent);
        continue;
      }
      path.add(parent);
      countHypernyms(wn, parent, path);
//      System.err.println("parent: "+parent);
      path.remove(parent);
    }
    return path.size();
  }

  private static void trueCaseLemmatize(final WordNetInterface wn, final String word, final Appendable output) throws Exception {
    for (final POS pos : POS.CATS) {
      boolean posShown = false;
      for (final String lemma : wn.lookupBaseForms(word, pos)) {
        if (! posShown) {
          output.append(pos.name());
          output.append(' ');
          posShown = true;
        }
        output.append(lemma);
        output.append(' ');
        torture(wn, lemma, pos);
      }
    }
    // using POS.ALL - slightly more efficient, but can't know POS of stem
    //for (final String lemma : wn.lookupBaseForms(word, POS.ALL)) {
    //  output.append(lemma);
    //  output.append(" ");
    //}
  }

  // TODO torture test: recursively get all hypernyms / hyponyms -- see if caches blow out memory
  
  // Goal: make a Hypernyms Iterator
  // - emit the Pointers (source + RelationType + target)
  // Goal2: make any-of-Set<RelationType> Iterator

  public static void main(String[] args) throws Exception {
    final WordNetInterface wn = WordNet.getInstance();
    final Appendable output = System.err;
    final Scanner scanner = new Scanner(System.in);
    while (scanner.hasNext()) {
      final String word = scanner.next();
      output.append(word);
      output.append(' ');
      trueCaseLemmatize(wn, word, output);
      output.append('\n');
    }
  }
}