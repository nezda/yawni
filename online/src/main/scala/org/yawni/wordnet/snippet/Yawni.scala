package org.yawni.wordnet.snippet

import scala.xml.{ Text, NodeSeq, NodeBuffer }
import net.liftweb.http.{ S, SHtml, XmlResponse }
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.util.Helpers._

import org.yawni.wordnet._
import org.yawni.util._
import org.yawni.wordnet.POS._
import org.yawni.wordnet.GlossAndExampleUtils._
import scala.collection.jcl.Conversions._
import scala.collection.jcl._
import java.util.TreeSet // don't want List

import net.liftweb.http.{ Req, GetRequest, PostRequest, LiftRules, JsonResponse, PlainTextResponse }
import net.liftweb.common.{Full, Box}
import net.liftweb.http.js.JE._

import net.liftweb.json._
import net.liftweb.json.JsonAST._

/**
 * Handles rendering of 
 */
object Yawni {
  def init() = {
    LiftRules.dispatch.prepend(Yawni.dispatch)
    // trigger preload
    val wn = WordNet.getInstance
    val query = "was";
    //System.err.println("query: "+query+" results: "+wn.lookupBaseForms(query, POS.ALL));
    //println("query: "+query+" results: "+wn.lookupBaseForms(query, POS.ALL));
  }

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
  //
  // other good server stats include lift stats like those shown in the example default.html:
  //  <div class="column span-23 last" style="text-align: center">
  //  <h4 class="alt"><a href='http://liftweb.net'><i>Lift</i></a> is Copyright 2007-2010 WorldWide Conferencing, LLC.  Distributed under an Apache 2.0 License.
  //    <br/>
  //    Lift version <lift:version_info.lift/> built on <lift:version_info.date/>.
  //    <br/>
  //    Stats: Total Memory: <lift:runtime_stats:total_mem/>
  //    Free Memory: <lift:runtime_stats:free_mem/>
  //    Open Sessions: <lift:runtime_stats:sessions/>
  //    Updated At: <lift:runtime_stats:updated_at/>
  //  </h4>
  //</div>

  def aboutResponse() = {
    XmlResponse(
      <about>
        Yawni Online
        <serverStats>
          <totalMemory>{ "%,d".format(Runtime.getRuntime.totalMemory) }</totalMemory>
          <freeMemory>{ "%,d".format(Runtime.getRuntime.freeMemory) }</freeMemory>
          <maxMemory>{ "%,d".format(Runtime.getRuntime.maxMemory) }</maxMemory>
        </serverStats>
      </about>
      // Open Sessions: <lift:runtime_stats:sessions/>
      // Updated At: <lift:runtime_stats:updated_at/>
    )
  }

  implicit def asScalaIterator[A](it : java.lang.Iterable[A]) = new MutableIterator.Wrapper(it.iterator)

  def suggest(prefix: String):JArray = {
    val wn = WordNet.getInstance
    val toReturn = new TreeSet(String.CASE_INSENSITIVE_ORDER)
    for (pos <- List(NOUN, VERB, ADJ, ADV);
         forms <- wn.searchByPrefix(prefix, pos);
         form <- forms if toReturn.size < 10
         ) { toReturn.add(form.getLemma) }
    // too simple
    //JArray(toReturn.map(JString(_)).toList)
    //fail JArray(toReturn.map(JObject(JField("id"), JString(_), JField("name"), JString(_)).toList)
    //fail x.map(JObject(JField("id", JString(_:String)) :: JField("name", JString(_:String)) :: Nil))
    //fail x.map(JObject(JField("id", "1") :: JField("name", JString(_)) :: Nil))
    //experimental JArray(for (s <- x) yield JObject(JField("id", s) :: JField("name", s) :: Nil))
    JArray((for (s <- toReturn) yield JObject(JField("value", JString(s)) :: JField("name", JString(s)) :: Nil)).toList)
  }

  // required data format described http://docs.jquery.com/Plugins/Autocomplete/autocomplete#url_or_dataoptions
  def autocomplete(prefix: String, limit: Int):String = {
    val wn = WordNet.getInstance
    val toReturn = new TreeSet(String.CASE_INSENSITIVE_ORDER)
    for (pos <- List(NOUN, VERB, ADJ, ADV);
         forms <- wn.searchByPrefix(prefix, pos);
         form <- forms if toReturn.size < limit
         ) { toReturn.add(form.getLemma) }
    //JArray(toReturn.map(JString(_)).toList)
    // really weird that it can't handle JSON ??
    //JString(toReturn.mkString("\n"))
    toReturn.mkString("\n")
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

  //implicit def asScalaIterator[A](it : java.lang.Iterable[A]) = new MutableIterator.Wrapper(it.iterator)

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
