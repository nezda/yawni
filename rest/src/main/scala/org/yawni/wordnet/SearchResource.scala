package org.yawni.wordnet

import javax.ws.rs._
import javax.ws.rs.core._
import scala.xml._

import javax.xml.bind.annotation._
import scala.reflect._

@Path("/search/")
class SearchResource {
  def init() = {
    // trigger preload
    val wn = WordNet.getInstance
    val query = "was";
    //System.err.println("query: "+query+" results: "+wn.lookupBaseForms(query, POS.ALL));
    //println("query: "+query+" results: "+wn.lookupBaseForms(query, POS.ALL));
  }

  @POST
  @Consumes(Array("application/json;charset=UTF-8"))
  //@Produces(Array("application/json", "text/javascript"))
  //@Produces(Array("application/json"))
  //def handleJSON(searchRequest:String): String = {
  //def handleJSON(searchRequest:SearchRequest): SearchRequest = {
  def handleJSON(searchRequest:SearchRequest): NodeSeq = {
    //println("json: "+searchRequest)
    //for {
    //  json <- req.json // get the JSON
    //  JObject(List(JField("command", JString(cmd)), JField("params", JString(params)))) <- json // extract the command
    //} yield JavaScriptResponse(SetHtml("json_result", cmd match { // build the response
    //      case "show" => Yawni.query(params)
    //      //case "textile" =>  TextileParser.toHtml(params, Empty)
    //      //case "count" => Text(params.length+" Characters")
    //      case x => <b>Problem... didn't handle JSON message {x}</b>
    //    }))
    //searchRequest.toString
    Yawni.query(searchRequest.params)
  }
}

// {"command":"show","params":"dog"}
@XmlRootElement
//@XmlAccessorType(XmlAccessType.FIELD)
case class SearchRequest() {
  @XmlElement
  //@BeanProperty
  var command:String = null
  @XmlElement
  //@BeanProperty
  var params:String = null

  override 
  def toString = "[SearchRequest command: %s params: %s]".format(command, params)
}

