/*
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.yawni.wordnet.snippet

import scala.xml.{ Text, NodeSeq }
import net.liftweb.http.{ S, SHtml }
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.jquery.JqJsCmds._
import net.liftweb.util.Helpers._

class Ajax {

  def sample(xhtml: NodeSeq): NodeSeq = {
    //// build up an ajax <a> tag to increment the counter
    //def doClicker(text: NodeSeq) =
    //  a(() => { cnt = cnt + 1; SetHtml(spanName, Text(cnt.toString)) }, text)

    //// create an ajax select box
    //def doSelect(msg: NodeSeq) =
    //  ajaxSelect((1 to 50).toList.map(i => (i.toString, i.toString)),
    //             Full(1.toString),
    //             v => DisplayMessage(msgName,
    //                                 bind("sel", msg, "number" -> Text(v)),
    //                                 5 seconds, 1 second))

// search field HTML
// <form>
//   <input onblur="if(this.value==''){this.value=this.defaultValue};this.style.color=(this.value==this.defaultValue)?'#aaa':'#000';" style="border-right: #666 1px solid; padding-right: 4px; border-top: #666 1px solid; padding-left: 22px; background: url(http://www.codestore.net/store.nsf/rsrc/bloggifs41/$file/find.gif) #fff no-repeat 3px 50%; padding-bottom: 4px; border-left: #666 1px solid; color: #aaa; padding-top: 4px; border-bottom: #666 1px solid" onfocus="this.style.color='#000';if(this.value==this.defaultValue){this.value=''}" value="Search Here"> 
// </form>


    // build up an ajax text box
    def doText(msg: NodeSeq) =
      //SHtml.ajaxText("", v => DisplayMessage(msgName,
      //                                 bind("text", msg, "value" -> Text(v)),
      //                                 4 seconds, 1 second))
      
      //SHtml.ajaxText("", v => SetHtml("resultz", Text(query(v).toString)))
      //SHtml.ajaxText("Type a word to lookup in WordNet...", v => SetHtml("resultz", Yawni.query(v)), ("type", "search"), ("size", "50"))
      FocusOnLoad(SHtml.ajaxText("", v => SetHtml("resultz", Yawni.query(v)), ("type", "search"), ("size", "50")))

    // bind the view to the functionality
    bind("ajax", xhtml,
         //"clicker" -> doClicker _,
         //"select" -> doSelect _,
         "text" -> doText _
         )
  }
}
