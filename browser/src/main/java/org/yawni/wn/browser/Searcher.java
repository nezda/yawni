/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.yawni.wn.browser;

import org.yawni.wn.Synset;
import org.yawni.wn.WordSense;
import org.yawni.wn.Word;
import org.yawni.wn.Relation;
import org.yawni.wn.RelationType;
import org.yawni.wn.POS;
import org.yawni.wn.DictionaryDatabase;
import org.yawni.wn.FileBackedDictionary;

import java.io.*;
import java.util.*;

import org.w3c.dom.*;

import javax.xml.parsers.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

/**
 * Try to use <a href="http://www.w3.org/TR/wordnet-rdf/">http://www.w3.org/TR/wordnet-rdf/</a>
 * - note odd Word/Collocation distinction - seems like this should be optional
 * 
 * Consider XStream (ThoughtWorks)
 * - supports JSON (REST)
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
 *   AjectiveSynsets
 *     AjectiveSynset
 *     AdjectiveSatelliteSynset
 *     AjectiveWordSense
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

  private static void trueCaseLemmatize(final DictionaryDatabase dictionary, final String word, final Appendable output) throws Exception {
    for (final POS pos : POS.CATS) {
      boolean posShown = false;
      for (final String lemma : dictionary.lookupBaseForms(word, pos)) {
        if (posShown == false) {
          output.append(pos.name());
          output.append(' ');
          posShown = true;
        }
        output.append(lemma);
        output.append(' ');
      }
    }
    // using POS.ALL - slightly more efficient, but can't know POS of stem
    //for (final String lemma : dictionary.lookupBaseForms(word, POS.ALL)) {
    //  output.append(lemma);
    //  output.append(" ");
    //}
  }

  // TODO torture test: recursively get all hypernyms / hyponyms -- see if caches blow out memory
  
  // Goal: make a Hypernyms Iterator
  // - emit the Pointers (source + RelationType + target)
  // Goal2: make any-of-Set<RelationType> Iterator

  public static void main(String[] args) throws Exception {
    final DictionaryDatabase dictionary = FileBackedDictionary.getInstance();
    final Appendable output = System.out;
    final Scanner scanner = new Scanner(System.in);
    while (scanner.hasNext()) {
      final String word = scanner.next();
      output.append(word);
      output.append(' ');
      trueCaseLemmatize(dictionary, word, output);
      output.append('\n');
    }
  }
}