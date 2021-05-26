package org.yawni.wordnet

import javax.servlet._
import javax.servlet.http._
import com.google.inject.Singleton

// courtesy
// http://jersey.576304.n2.nabble.com/Jersey-Guice-Serving-Static-Content-what-are-the-implications-of-both-solutions-td5437422.html
@Singleton
class StaticWrapperServlet extends HttpServlet { 
  override
  def service(req:HttpServletRequest, resp:HttpServletResponse): Unit = {
    val rd:RequestDispatcher = getServletContext().getNamedDispatcher("default"); 
    val wrapped:HttpServletRequest = new HttpServletRequestWrapper(req) { 
      override
      def getPathInfo():String = null
    } 
    rd.forward(wrapped, resp)
  } 
}
