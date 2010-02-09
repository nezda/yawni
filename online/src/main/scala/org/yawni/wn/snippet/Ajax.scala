package org.yawni.wn.snippet

//import _root_.net.liftweb.http._
////import _root_.net.liftweb.widgets.autocomplete._
//import S._
//import SHtml._
//import js._
//import JsCmds._
//import js.jquery._
//import _root_.net.liftweb.http.jquery._
//import JqJsCmds._
////import _root_.net.liftweb.common._
////import _root_.net.liftweb.util._
//import _root_.net.liftweb.util.Helpers._
//import _root_.scala.xml.{Text, NodeSeq}

import scala.xml.{Text, NodeSeq}
import net.liftweb.http.{S, SHtml}
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.util.Helpers._

import org.yawni.wordnet._
import scala.collection.jcl.Conversions._

class Ajax {

  def sample(xhtml: NodeSeq): NodeSeq = {
    // local state for the counter
    var cnt = 0

    // get the id of some elements to update
    val spanName: String = S.attr("id_name") openOr "cnt_id"
    val msgName: String = S.attr("id_msgs") openOr "messages"

    //// build up an ajax <a> tag to increment the counter
    //def doClicker(text: NodeSeq) =
    //  a(() => {cnt = cnt + 1; SetHtml(spanName, Text(cnt.toString))}, text)

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

    val nounResults = wn.lookupWordSenses(word, POS.NOUN)
    println("nounResults.size: "+nounResults.size);
    //val verbResults = wn.lookupSynsets(word, POS.VERB)
    //val adjResults = wn.lookupSynsets(word, POS.ADJ)
    //val advResults = wn.lookupSynsets(word, POS.ADV)
    
    var results: NodeSeq = <h2>{word}</h2>
    if (! nounResults.isEmpty) {
      println("has nouns");
      results ++= <h4>Noun</h4>
      <ol>{
        //for {sense <- nounResults}
        for (sense <- nounResults)
          yield <li>{ sense.toString }</li>
      }</ol>
    }
    println("results: "+results);
    results
  }
}
