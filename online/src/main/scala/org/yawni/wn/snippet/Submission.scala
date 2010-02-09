package org.yawni.wn.snippet

//import net.liftweb.http.S // Session
import net.liftweb.http.SHtml
import net.liftweb.util.Helpers._

//import com.test.controller._

class Submission {
  def form = {
    var word = ""

    <span>
      { SHtml.text("", w => word = w) }
      { SHtml.submit("Search", () => println(word)) }
    </span>

    //<span>
    //  { S.text("url", u => url = u) }
    //  { S.text("title", t => title = t) }
    //  { S.submit("Submit", ignore => LinkStore ! AddLink(url, title)) }
    //</span>
  }
}
