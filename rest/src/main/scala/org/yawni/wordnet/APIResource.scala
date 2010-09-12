package org.yawni.wordnet

import javax.ws.rs._
import javax.ws.rs.core._
import scala.xml._

@Path("/api/{someString}")
class APIResource {
  def init() = {
//    LiftRules.dispatch.prepend(Yawni.dispatch)
    // trigger preload
    val wn = WordNet.getInstance
    val query = "was";
    //System.err.println("query: "+query+" results: "+wn.lookupBaseForms(query, POS.ALL));
    //println("query: "+query+" results: "+wn.lookupBaseForms(query, POS.ALL));
  }

//  def dispatch: LiftRules.DispatchPF = {
//    // Req(url_pattern_list, suffix, request_type)
//    case Req("api" :: someString :: Nil, _, GetRequest) => () => Full(xmlResponse(someString))
//    case Req("query" :: someString :: Nil, _, GetRequest) => () => Full(xmlResponse(someString))
//    case Req("about" :: Nil, _, GetRequest) => () => Full(aboutResponse)
//  }

  //def get = JsonResponse(JsArray(
  //  JsObj("id" -> 1, "start" -> "2009-12-30T12:15:00.000+10:00",
  //    "end" -> "2009-12-30T12:45:00.000+10:00",
  //    "title" -> "some stuff"),
  //  JsObj("id" -> 2, "start" -> "2009-12-30T14:15:00.000+10:00", 
  //    "end" -> "2009-12-30T14:45:00.000+10:00",
  //    "title" -> "some other stuff")
  //))

  @GET
  def xmlResponse(@PathParam("someString") someString: String) = {
    <html> { Yawni.query(someString) } </html>
  }
}
