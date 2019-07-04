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

import scala.xml.{ Elem, NodeSeq }
import net.liftweb.http.{ SHtml, DispatchSnippet }
import net.liftweb.http.js.JsCmds._
import net.liftweb.util.Helpers._

object Ajax extends DispatchSnippet {
  override def dispatch: PartialFunction[String, NodeSeq => NodeSeq] = {
    case "searchField" => searchField
  }
  // searchField closure
  def searchField(xhtml: NodeSeq): NodeSeq = {
    // build up an ajax text box
    def searchBox: Elem = {
      SHtml.ajaxText("", q => SetHtml("resultz", Yawni.query(q)), ("id", "searchBoxID"))
    }
    // searchBox ajaxText will activate on blur so this is just for show
//    def searchButton: Elem = {
//      SHtml.ajaxButton("Search", () => Noop)
//    }
//    val msgName: String = S.attr("id_msgs") openOr "messages"
    val viewBind = {
//      "#searchButton" #> searchButton _ &
      "#searchBox" #> searchBox
    }
    viewBind(xhtml) ++ Script(OnLoad(SetValueAndFocus("searchBoxID", "")))
  }
}
