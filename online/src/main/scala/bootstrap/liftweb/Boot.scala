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
package bootstrap.liftweb

import net.liftweb.common._
//import common.{Box, Full, Empty, Failure}
import net.liftweb.util._
//import util.{Helpers, Log, NamedPF, Props}
import net.liftweb.http._
import net.liftweb.sitemap._
import net.liftweb.sitemap.Loc._
import Helpers._
import org.yawni.wordnet._;

import net.liftweb._
import provider._

import org.yawni.wordnet.snippet._
import org.yawni.roundedcorners._

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify Lift's environment
 */
class Boot {
  def boot {
    // where to search for snippet (functions)
    LiftRules.addToPackages("org.yawni.wordnet")

    //LiftRules.fixCSS("css" :: Nil, Empty)

    LiftRules.dispatch.prepend(Yawni.dispatch)
    LiftRules.dispatch.prepend(RoundedCornerService.dispatch)

    // manual plumbing/wiring for singleton object snippet:
    LiftRules.snippetDispatch.append(Map("Ajax" -> Ajax))

    // Build SiteMap
    val entries = Menu(Loc("Home", List("index"), "Home")) :: Nil
    LiftRules.setSiteMap(SiteMap(entries:_*))

    // not valid Scala
    // WordNetInterface wn = WordNet.getInstance();
    // String query = "was";
    // verbse Scala
    // val wn: WordNetInterface = WordNet.getInstance();
    // val query: String = "was";
    // typical compact Scala variants:
    //  - semicolons often optional (usually elided)
    //  - parens optional for zero arg methods (usually elided)
    //  - method-invoking "." optional
    // val wn = WordNet.getInstance()
    // val wn = WordNet getInstance
    val wn = WordNet.getInstance
    val query = "was";
    //System.err.println("query: "+query+" results: "+wn.lookupBaseForms(query, POS.ALL));
    //println("query: "+query+" results: "+wn.lookupBaseForms(query, POS.ALL));
    
    /*
     * Show the spinny image when an Ajax call starts
     */
    LiftRules.ajaxStart =
            Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

    /*
     * Make the spinny image go away when it ends
     */
    LiftRules.ajaxEnd =
            Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)
    
    LiftRules.early.append(makeUtf8)

    // Dump browser information each time a new connection is made
    LiftSession.onBeginServicing = BrowserLogger.haveSeenYou _ :: LiftSession.onBeginServicing
  }
  private def makeUtf8(req: HTTPRequest): Unit = {req.setCharacterEncoding("UTF-8")}
}

object BrowserLogger {
  object HaveSeenYou extends SessionVar(false)

  def haveSeenYou(session: LiftSession, request: Req) {
    if (! HaveSeenYou.is) {
      Log.info("Created session " + session.uniqueId + 
        " IP: {" + request.request.remoteAddress + 
        "} UserAgent: {{" + request.userAgent.openOr("N/A") + "}}")
      HaveSeenYou(true)
    }
  }
}
