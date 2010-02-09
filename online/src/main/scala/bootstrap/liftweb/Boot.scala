package bootstrap.liftweb

import _root_.net.liftweb.common._
import _root_.net.liftweb.util._
import _root_.net.liftweb.http._
import _root_.net.liftweb.sitemap._
import _root_.net.liftweb.sitemap.Loc._
import Helpers._
import org.yawni.wordnet._;

/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def boot {
    // where to search snippet
    LiftRules.addToPackages("org.yawni.wordnet")

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
  }
}

