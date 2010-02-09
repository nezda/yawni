package org.yawni.wn.snippet

import _root_.scala.xml.NodeSeq
import _root_.net.liftweb.util.Helpers
import Helpers._

class HelloWorld {
  def howdy(in: NodeSeq): NodeSeq =
    Helpers.bind("b", in, "time" -> (new _root_.java.util.Date).toString)
    //S<span>Welcome to helloworld at {new java.util.Date}</span>
    // doesn't work
    //<b:time>{new _root_.java.util.Date}</b:time>
}

