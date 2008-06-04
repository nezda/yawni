package edu.brandeis.cs.steele.wn.browser;

import edu.brandeis.cs.steele.wn.Synset;
import edu.brandeis.cs.steele.wn.WordSense;
import edu.brandeis.cs.steele.wn.Word;
import edu.brandeis.cs.steele.wn.Pointer;
import edu.brandeis.cs.steele.wn.PointerType;

import java.io.*;

import org.w3c.dom.*;

import javax.xml.parsers.*;

import javax.xml.transform.*;
import javax.xml.transform.dom.*;
import javax.xml.transform.stream.*;

/*
Try to use http://www.w3.org/TR/wordnet-rdf/
- note odd Word/Collocation distinction - seems like this should be optional

WordNetQueryResult 
  queryTerm:"foo",
  queryPOS:"NOUN",
  queryPointerType:"HYPONYM"
  
  NounSynsets
    NounSynset
    NounWordSense
  VerbSynsets
    VerbSynset
    VerbWordSense
  AjectiveSynsets
    AjectiveSynset
    AdjectiveSatelliteSynset
    AjectiveWordSense
    AdjectiveSatelliteWordSense
  AdverbSynsets
    AdverbSynset
    AdverbWordSense
*/
class SynsetWriter {
  private final Synset synset;
  private final Appendable output;
  SynsetWriter(final Synset synset, final Appendable output) {
    this.synset = synset;
    this.output = output;
  }

  public Appendable write() {
    for(final WordSense wordSense : synset) {
      new WordSenseWriter(wordSense, output).write();
    }
    return output;
  }
}

class WordSenseWriter {
  private final WordSense wordSense;
  private final Appendable output;
  WordSenseWriter(final WordSense wordSense, final Appendable output) {
    this.wordSense = wordSense;
    this.output = output;
  }

  public Appendable write() {
    // lexicalForm (case sensitive!)
    // senseNumber
    // synset
    return output;
  }
}

class WordWriter {
  private final Word word;
  private final Appendable output;
  WordWriter(final Word word, final Appendable output) {
    this.word = word;
    this.output = output;
  }

  public Appendable write() {
    // lexicalForm
    // pos
    for(final Synset synset : word) {
      new SynsetWriter(synset, output).write();
    }
    return output;
  }
}

class PointerWriter {
  private final Appendable output;
  PointerWriter(final Appendable output) {
    this.output = output;
  }
  // Synset -> Synset (aka semantic)
  // WordSense -> WordSense (aka lexical)
  public Appendable write() {
    // source PointerTarget
    // target PointerTarget
    // PointerType
    return output;
  }
}

public class Searcher {
  public static void main(String[] args) throws Exception {
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
}
