package org.yawni.wordnet.snippet

import scala.xml.{ Text, NodeSeq }
import net.liftweb.http.{ S, SHtml }
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.util.Helpers._

import org.yawni.wordnet._
import org.yawni.wordnet.POS._
import scala.collection.jcl.Conversions._

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
      SHtml.ajaxText("", v => SetHtml("resultz", query(v)))

    // bind the view to the functionality
    bind("ajax", xhtml,
         //"clicker" -> doClicker _,
         //"select" -> doSelect _,
         "text" -> doText _
         )
  }

  def query(word: String): NodeSeq = {
    val wn = WordNet.getInstance
    var results: NodeSeq = <h2>“{ word }”</h2>
    var inWordNet = false
    // TODO add dividers between POS results
    // TODO deal with case where there is stemming ambiguity (group by Word)
    for (pos <- List(NOUN, VERB, ADJ, ADV)) {
      val posResults = wn.lookupWordSenses(word, pos)
      if (! posResults.isEmpty) {
        inWordNet = true
        results ++= <h4>{ pos.getLabel.capitalize }</h4>
        <ol>{
          for (sense <- posResults)
            yield <li>{ render(sense) }</li>
        }</ol>
      }
    }
    if (! inWordNet) {
      results ++= <h4>No results found</h4>
    }
    results
  }

  def render(sense: WordSense) = {
    //sense.toString
    //sense.getLongDescription
    val verbose = false
    sense.getSynset.getLongDescription(verbose)
  }
}
