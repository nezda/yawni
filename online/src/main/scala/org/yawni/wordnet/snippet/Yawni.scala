package org.yawni.wordnet.snippet

import scala.xml.{ Text, NodeSeq }
import net.liftweb.http.{ S, SHtml, XmlResponse }
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.util.Helpers._

import org.yawni.wordnet._
import org.yawni.util._
import org.yawni.wordnet.POS._
import scala.collection.jcl.Conversions._
import java.util.TreeSet // don't want List

import net.liftweb.http.{ Req, GetRequest, PostRequest, LiftRules, JsonResponse, PlainTextResponse }
import net.liftweb.common.{Full, Box}
import net.liftweb.http.js.JE._

object Yawni {
  def dispatch: LiftRules.DispatchPF = {
    // Req(url_pattern_list, suffix, request_type)
    case Req("api" :: someString :: Nil, _, GetRequest) => () => Full(xmlResponse(someString))
    case Req("about" :: Nil, _, GetRequest) => () => Full(aboutResponse)
  }

  //def get = JsonResponse(JsArray(
  //  JsObj("id" -> 1, "start" -> "2009-12-30T12:15:00.000+10:00",
  //    "end" -> "2009-12-30T12:45:00.000+10:00",
  //    "title" -> "some stuff"),
  //  JsObj("id" -> 2, "start" -> "2009-12-30T14:15:00.000+10:00", 
  //    "end" -> "2009-12-30T14:45:00.000+10:00",
  //    "title" -> "some other stuff")
  //))

  def xmlResponse(someString: String) = {
    XmlResponse(<html> { query(someString) } </html>)
  }

  // report something like org.yawni.wordnet.browser.Application
  //
  // really should generalize Application to parse a properties:
  //
  // better name:
  //  "ProjectProperties" / "BuildInfo" / "ProjectInfo"
  // "DOAP: Description of a Project" an XML/RDF vocabulary to describe software projects, and in particular open source
  // 
  //
  // - remove default class (currently it's this - make this a param)
  // - leave default properties file name (e.g., "application.properties")
  def aboutResponse() = {
    XmlResponse(
      <about>
        Coming soon...
      </about>
    )
  }

  // group by Word
  def query(someString: String): NodeSeq = {
    val wn = WordNet.getInstance
    var results: NodeSeq = NodeSeq.Empty
    for (pos <- List(NOUN, VERB, ADJ, ADV)) {
      val noCaseForms = new TreeSet(String.CASE_INSENSITIVE_ORDER)
      val forms = wn.lookupBaseForms(someString, pos)
      for (form <- forms) {
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
    for (synset <- word.getSynsets)
      yield <li>{ render(word, synset.getWordSense(word)) }</li>
    }</ol>
  }

  private def render(word: Word, wordSense: WordSense) = {
    val verbose = false
    wordSense.getSynset.getLongDescription(verbose)
  }
}
