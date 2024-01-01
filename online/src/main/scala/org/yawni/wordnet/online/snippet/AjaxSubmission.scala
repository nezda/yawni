package org.yawni.wordnet.online.snippet

import net.liftweb.http.SHtml
import net.liftweb.http.js.JE.JsRaw
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds.Noop
import net.liftweb.util.Helpers._

import scala.xml.Elem

// plan
// - text field
// - search button
// - searchResultArea : dynamically update output of searching with current text field value

class AjaxSubmission {
  def form: Elem = {
    var word = ""
    //<span>
    //  { SHtml.text("", w => word = w) }
    //  { SHtml.submit("Search", () => println(word)) }
    //</span>
    val button = <button type="button">Search!</button> %
        ("onclick" -> SHtml.ajaxCall(JsRaw("$('#word').attr('value')"), search _))
     (<div>
        <div>
          <input type="text" id="word"/>
          {button}
        </div>
     </div>)
  }
  def search(s:String): JsCmd = {   
    println("searched for "+s)
    Noop
  }
}
