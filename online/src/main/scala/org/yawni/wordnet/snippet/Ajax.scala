package org.yawni.wordnet.snippet

import scala.xml.{ Text, NodeSeq }
import net.liftweb.http.{ S, SHtml }
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.util.Helpers._

import org.yawni.wordnet._
import org.yawni.util._
import org.yawni.wordnet.POS._
import scala.collection.jcl.Conversions._
import java.util.TreeSet // don't want List

class Ajax {

  def sample(xhtml: NodeSeq): NodeSeq = {
    //// build up an ajax <a> tag to increment the counter
    //def doClicker(text: NodeSeq) =
    //  a(() => { cnt = cnt + 1; SetHtml(spanName, Text(cnt.toString)) }, text)

    //// create an ajax select box
    //def doSelect(msg: NodeSeq) =
    //  ajaxSelect((1 to 50).toList.map(i => (i.toString, i.toString)),
    //             Full(1.toString),
    //             v => DisplayMessage(msgName,
    //                                 bind("sel", msg, "number" -> Text(v)),
    //                                 5 seconds, 1 second))

    // build up an ajax text box
    def doText(msg: NodeSeq) =
      //SHtml.ajaxText("", v => DisplayMessage(msgName,
      //                                 bind("text", msg, "value" -> Text(v)),
      //                                 4 seconds, 1 second))
      
      //SHtml.ajaxText("", v => SetHtml("resultz", Text(query(v).toString)))
      SHtml.ajaxText("", v => SetHtml("resultz", query1(v)))

    // bind the view to the functionality
    bind("ajax", xhtml,
         //"clicker" -> doClicker _,
         //"select" -> doSelect _,
         "text" -> doText _
         )
  }

  // group by Word
  def query1(someString: String): NodeSeq = {
    val wn = WordNet.getInstance
    var results: NodeSeq = NodeSeq.Empty
    for (pos <- List(NOUN, VERB, ADJ, ADV)) {
      val noCaseForms = new TreeSet(String.CASE_INSENSITIVE_ORDER)
      val forms = wn.lookupBaseForms(someString, pos);
      for (form <- forms) {
        if (! noCaseForms.contains(form)) {
          // block no case duplicates ("hell"/"Hell", "villa"/"Villa")
          noCaseForms.add(form)
          val word = wn.lookupWord(form, pos)
          if (word != null)
            results ++= (wordSummary(word) ++ appendSenses(word))
        }
      }
    }
    if (results == NodeSeq.Empty) {
      results ++= <h4>No results found</h4>
    }
    results
  }

  private def wordSummary(word: Word) = {
    val synsets = word.getSynsets
    val taggedCount = word.getTaggedSenseCount
    <span>The <span class="pos">{ word.getPOS().getLabel() }
      </span> <span class="summaryWord">{ WordCaseUtils.getDominantCasedLemma(word) }
      </span> has { synsets.size } sense{ if (synsets.size == 1) "" else "s"} ({
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
    for (synset <- word.getSynsets)
      yield <li>{ render(word, synset.getWordSense(word)) }</li>
    }</ol>
  }

  private def render(word: Word, wordSense: WordSense) = {
    //println("render: "+word+" "+wordSense)
    val verbose = false
    wordSense.getSynset.getLongDescription(verbose)
  }

  // oversimplified version ; doesn't deal with case where there is stemming ambiguity (group by Word)
  private def query0(someString: String): NodeSeq = {
    val wn = WordNet.getInstance
    //var results: NodeSeq = <h2>“{ someString }”</h2>
    var results: NodeSeq = NodeSeq.Empty
    for (pos <- List(NOUN, VERB, ADJ, ADV)) {
      val posResults = wn.lookupWordSenses(someString, pos)
      if (! posResults.isEmpty) {
        results ++= <h4>{ pos.getLabel.capitalize }</h4>
        <ol>{
          for (sense <- posResults)
            yield <li>{ render(sense) }</li>
        }</ol>
        <hr/>
      }
    }
    if (results == NodeSeq.Empty) {
      results ++= <h4>No results found</h4>
    }
    results
  }

  private def render(sense: WordSense) = {
    //sense.toString
    //sense.getLongDescription
    val verbose = false
    sense.getSynset.getLongDescription(verbose)
  }
}
