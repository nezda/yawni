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

import net.liftweb.common.{Empty, Full, Logger}
import net.liftweb.util._

import net.liftweb.http._
import Helpers._

import provider._

import org.yawni.wordnet.snippet._
import scala.language.postfixOps

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify Lift's environment
 */
class Boot {
  def boot() {
    // where to search for snippet (functions)
    LiftRules.addToPackages("org.yawni.wordnet")

    LiftRules.htmlProperties.default.set((r: Req) => Html5Properties(r.userAgent))

    Yawni.init()

    StatelessJson.init()
    LiftRules.enableLiftGC = false
    // autocomplete mouse over seems to require this
    LiftRules.useXhtmlMimeType = false

    // manual plumbing/wiring for singleton object snippet:
    LiftRules.snippetDispatch.append(Map("Ajax" -> Ajax))

    // Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart = Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)

    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd = Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)
    
    LiftRules.early.append(makeUtf8)

    LiftRules.noticesAutoFadeOut.default.set((notices: NoticeType.Value) => {
      notices match {
        case NoticeType.Notice => Full((8 seconds, 4 seconds))
        case _                 => Empty
      }
    })

    // Dump browser information each time a new connection is made
    LiftSession.onBeginServicing = BrowserLogger.haveSeenYou _ :: LiftSession.onBeginServicing

    LiftRules.securityRules = () => {
      SecurityRules(
        content = Some(
          ContentSecurityPolicy(
            scriptSources = List(ContentSourceRestriction.UnsafeEval,
              ContentSourceRestriction.UnsafeInline,
              ContentSourceRestriction.Self),
            styleSources = List(ContentSourceRestriction.UnsafeInline,
              ContentSourceRestriction.Self),
            imageSources = List(ContentSourceRestriction.All,
              ContentSourceRestriction.Scheme("data")),
          )))
      //
    }
  }
  private def makeUtf8(req: HTTPRequest): Unit = {req.setCharacterEncoding("UTF-8")}
}

object BrowserLogger {
  object HaveSeenYou extends SessionVar(false)
  object Log extends Logger

  def haveSeenYou(session: LiftSession, request: Req) {
    if (! HaveSeenYou.is) {
      Log.info("Created session " + session.uniqueId + 
        " IP: {" + request.request.remoteAddress + 
        "} UserAgent: {{" + request.userAgent.openOr("N/A") + "}}")
      HaveSeenYou(true)
    }
  }
}
