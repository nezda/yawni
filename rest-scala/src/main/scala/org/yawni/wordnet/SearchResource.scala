package org.yawni.wordnet

import javax.ws.rs._
import javax.ws.rs.core._
import scala.xml._

import javax.xml.bind.annotation._
import scala.reflect._

@Path("/search/")
class SearchResource {
  def init(): Unit = {
    println("SearchResource.init()!")
    // trigger preload
    val wn = WordNet.getInstance
    val query = "was"
    // System.err.println("query: "+query+" results: "+wn.lookupBaseForms(query, POS.ALL))
    // println("query: "+query+" results: "+wn.lookupBaseForms(query, POS.ALL))
  }

  @POST
  @Consumes(Array("application/json;charset=UTF-8"))
  //@Produces(Array("application/json", "text/javascript"))
  //@Produces(Array("application/json"))
  //@Produces(Array("text/plain"))
  def handleJSON(searchRequest: SearchRequest): NodeSeq = {
    Yawni.query(searchRequest.params)
  }
}

// {"command": "show","params": "dog"}
@XmlRootElement
//@XmlAccessorType(XmlAccessType.FIELD)
case class SearchRequest() {
  @XmlElement
  //@BeanProperty
  var command: String = _
  @XmlElement
  //@BeanProperty
  var params: String = _

  override 
  def toString = s"[SearchRequest command: $command params: $params]"
}