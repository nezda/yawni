package org.yawni.wordnet

import javax.ws.rs._
import javax.ws.rs.core._
import scala.xml._

@Path("/api/{someString}")
class APIResource {
  def init(): Unit = {
    println("APIResource.init()!")
    // trigger preload
    val wn = WordNet.getInstance
    val query = "was"
    //System.err.println("query: "+query+" results: "+wn.lookupBaseForms(query, POS.ALL))
    //println("query: "+query+" results: "+wn.lookupBaseForms(query, POS.ALL))
  }

  @GET
  @Produces(Array("application/xhtml+xml; charset=utf-8"))
  def xmlResponse(@PathParam("someString") someString: String): Elem = {
    <div> { Yawni.query(someString) } </div>
  }
}