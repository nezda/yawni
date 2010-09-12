package org.yawni.wordnet

import javax.ws.rs._
import javax.ws.rs.core._
import javax.ws.rs.core.Response.Status._

import org.yawni.wordnet._
import org.yawni.util._
import org.yawni.wordnet.POS._
import scala.xml._
import scala.collection.JavaConversions._
import java.util.TreeSet // don't want List

@Path("/autocomplete")
class AutocompleteResource {
  def init() = {
    // trigger preload
    val wn = WordNet.getInstance
    val query = "was";
    //System.err.println("query: "+query+" results: "+wn.lookupBaseForms(query, POS.ALL));
    //println("query: "+query+" results: "+wn.lookupBaseForms(query, POS.ALL));
  }

  // required data format described http://docs.jquery.com/Plugins/Autocomplete/autocomplete#url_or_dataoptions
  @GET
  @Produces(Array("text/plain"))
  def autocomplete(@QueryParam("q") prefix: String, @QueryParam("limit") @DefaultValue("25") limit: Int):String = {
    if (prefix == null) {
      throw new WebApplicationException(Response.status(BAD_REQUEST).entity("q is null").build)
    }
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
}
