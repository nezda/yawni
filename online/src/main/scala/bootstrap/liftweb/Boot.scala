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
 * to modify lift's environment
 */
class Boot {
  def boot {
    // where to search snippet
    LiftRules.addToPackages("org.yawni.wordnet")

    //LiftRules.fixCSS("css" :: Nil, Empty)

    LiftRules.dispatch.prepend(Yawni.dispatch)
    LiftRules.dispatch.prepend(RoundedCornerService.dispatch)

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
    
    LiftRules.early.append(makeUtf8)

    // Dump browser information each time a new connection is made
    LiftSession.onBeginServicing = BrowserLogger.haveSeenYou _ :: LiftSession.onBeginServicing
  }
  private def makeUtf8(req: HTTPRequest): Unit = {req.setCharacterEncoding("UTF-8")}
}

object BrowserLogger {
  object HaveSeenYou extends SessionVar(false)

  def haveSeenYou(session: LiftSession, request: Req) {
    if (!HaveSeenYou.is) {
      Log.info("Created session " + session.uniqueId + " IP: {" + request.request.remoteAddress + "} UserAgent: {{" + request.userAgent.openOr("N/A") + "}}")
      HaveSeenYou(true)
    }
  }
}

