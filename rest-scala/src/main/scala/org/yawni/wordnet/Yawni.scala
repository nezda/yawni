package org.yawni.wordnet

import javax.ws.rs._
import javax.ws.rs.core._
import javax.ws.rs.core.Response.Status._

import org.yawni.wordnet._
import org.yawni.util._
import org.yawni.wordnet.POS._
import org.yawni.wordnet.GlossAndExampleUtils._
import scala.xml._
import scala.collection.JavaConverters._
import java.util.TreeSet // don't want List

/**
 * Functions to search WordNet and render results as XML NodeSeqs
 */
object Yawni {
  def init() = {
    // trigger preload
    val wn = WordNet.getInstance
    val query = "was";
    //System.err.println("query: "+query+" results: "+wn.lookupBaseForms(query, POS.ALL));
    //println("query: "+query+" results: "+wn.lookupBaseForms(query, POS.ALL));
  }

  // group by Word
  def query(someString: String): NodeSeq = {
    val wn = WordNet.getInstance
    var results: NodeSeq = NodeSeq.Empty
    for (pos <- List(NOUN, VERB, ADJ, ADV)) {
      val noCaseForms = new TreeSet(String.CASE_INSENSITIVE_ORDER)
      val forms = wn.lookupBaseForms(someString, pos)
      for (form <- forms.asScala) {
        if (! noCaseForms.contains(form)) {
          // block no case duplicates ("hell"/"Hell", "villa"/"Villa")
          noCaseForms.add(form)
          val word = wn.lookupWord(form, pos)
          if (word != null)
            results ++= (wordSummary(word) ++ appendSenses(word) ++ <hr/>)
        }
      }
    }
    if (results == NodeSeq.Empty) {
      if (someString.trim.length != 0)
        results ++= <h4>No results found</h4>
    }
    //println(results)
    results
  }

  private def wordSummary(word: Word) = {
    val synsets = word.getSynsets
    val taggedCount = word.getTaggedSenseCount
    <span>The <span class="pos">{ word.getPOS.getLabel }</span> 
    <span class="summaryWord">{ WordCaseUtils.getDominantCasedLemma(word) }</span> 
    has { synsets.size } sense{ if (synsets.size == 1) "" else "s"} ({
    if (taggedCount == 0)
      "none"
    else
      if (taggedCount == synsets.size)
        if (taggedCount == 2) "both"
        else "all"
      else
        "first " + taggedCount
    }
    from tagged texts)</span>
  }

  private def appendSenses(word: Word) = {
    <ol>{
    for (synset <- word.getSynsets.asScala)
      yield <li>{ render(word, synset.getWordSense(word)) }</li>
    }</ol>
  }

  //private def render(word: Word, wordSense: WordSense) = {
  //  val verbose = false
  //  wordSense.getSynset.getLongDescription(verbose)
  //  <span>
  //  { "{ " + wordSense.getSynset.map(_.getLemma).mkString(" • ") + " } — " + wordSense.getSynset.getGloss }
  //  </span>
  //}

  //private def render(word: Word, wordSense: WordSense) = {
  //  <div class="synset"> { wordSense.getSynset.map(_.getLemma).mkString(" • ") } </div> ++
  //  <div class="gloss"> { wordSense.getSynset.getGloss } </div>
  //}
  
  private def focalWord(word: Word, wordSense: WordSense) = {
    if (word.getLowercasedLemma.equalsIgnoreCase(wordSense.getLemma))
      <span class="focalWord">{ wordSense.getLemma }</span>
    else
      Text(wordSense.getLemma)
  }

  private def render(word: Word, wordSense: WordSense) = {
    val synset = wordSense.getSynset
//    <div class="synset"> { synset.map(_.getLemma).mkString(" • ") } </div> ++
    val wordSenses = synset.iterator()
    val synsetXML = new NodeBuffer
    if (wordSenses.hasNext) synsetXML.append(focalWord(word, wordSenses.next))
    while (wordSenses.hasNext) {
      synsetXML.append(Text(" • "))
      synsetXML.append(focalWord(word, wordSenses.next))
    }
    <div class="synset"> { synsetXML } </div>
    <div class="gloss">
      <div class="definitions"> { getDefinitionsChunk(synset) } </div>
      { renderExamples(synset) }
    </div>
  }

  private def renderExamples(synset: Synset) = {
    val examples = getExamplesChunk(synset)
    if (! examples.isEmpty)
      <div class="examples"> { examples } </div>
    else
      NodeSeq.Empty
  }
}
