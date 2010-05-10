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

import scala.xml.{ Elem, Text, NodeSeq, Group }
import net.liftweb.http.{ S, SHtml, DispatchSnippet }
import net.liftweb.http.js.JsCmds._
import net.liftweb.util.Helpers._

//  def doSearch(msg: NodeSeq) = {
//    //SHtml.ajaxText("", q => DisplayMessage(msgName,
//    //                                 bind("text", msg, "value" -> Text(q)),
//    //                                 4 seconds, 1 second))
//    //SHtml.ajaxText("", q => SetHtml("resultz", Text(query(q).toString)))
//  }
//}

// Manual Plumbing/Wiring: dispatch method here and this line Boot:
//   LiftRules.snippetDispatch.append(Map("Ajax" -> Ajax))
// If Ajax were a class, (but still extended and implemented DispatchSnippet), we could forgoe
// this manual wiring in Boot.  Not clear why it can't look for object which extends DispatchSnippet ?
//
// Benefits: 
// + much more efficient
// + closure of handler method can have nested 'fields' and defs, thus it has equivalent power
// Drawbacks: 
// - more typing
//class Ajax extends DispatchSnippet {
object Ajax extends DispatchSnippet {
  override def dispatch = { 
    case "searchField" => searchField
  }
  // searchField closure
  def searchField(xhtml: NodeSeq): NodeSeq = {
    // build up an ajax text box
    def searchBox: Elem = {
      SHtml.ajaxText("", q => SetHtml("resultz", Yawni.query(q)), ("id", "searchBoxId"))
    }
    // searchBox ajaxText will activate on blur so this is just for show
    def searchButton: Elem = {
      SHtml.ajaxButton("Search", () => Noop)
    }
    bind("ajax", xhtml,
         "searchButton" -%> searchButton,
         "searchBox" -%> searchBox
    ) ++ Script(OnLoad(SetValueAndFocus("searchBoxId", "")))
  }
}

// Automatic Plumbing/Wiring: <lift:Ajax.searchField> in app template triggers search for 
// public class (not object!) snippet.Ajax with public member method 'searchField'.
// Benefits: 
// + include simplicity / 'automaticness'
// Drawbacks: 
// - reflection search happens for every new session and is not cached, so it is not very efficient.
// - magic is harder to follow and template naming is tightly bound to code (no indirection)
//class Ajax {
//  // searchField closure
//  def searchField(xhtml: NodeSeq): NodeSeq = {
//    // build up an ajax text box
//    def searchBox = {
//      SHtml.ajaxText("", q => SetHtml("resultz", Yawni.query(q)), ("id", "searchBoxId"))
//    }
//    // searchBox ajaxText will activate on blur so this is just for show
//    def searchButton = {
//      SHtml.ajaxButton("Search", () => Noop)
//    }
//    // bind the view to the functionality
//    bind("ajax", xhtml,
//         "searchButton" -%> searchButton,
//         "searchBox" -%> searchBox
//    ) ++ Script(JqOnLoad(SetValueAndFocus("searchBoxId", "")))
//  }
//}
