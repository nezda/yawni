package org.yawni.wordnet

import com.google.inject.Guice
import com.google.inject.Injector
import com.google.inject.servlet.GuiceServletContextListener
import com.google.inject.servlet.ServletModule
import com.sun.jersey.guice.JerseyServletModule
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer
import scala.collection.JavaConverters._
import scala.xml._
import javax.ws.rs.ext.MessageBodyWriter
import com.sun.jersey.api.core.ResourceConfig

class YawniGuiceConfig extends GuiceServletContextListener {
  override
  protected def getInjector(): Injector = {
    Guice.createInjector(new JerseyServletModule() {
      override
      protected def configureServlets(): Unit = {
        bind(classOf[SearchResource])
        bind(classOf[APIResource])
        bind(classOf[AutocompleteResource])
        bind(classOf[AboutResource])
        bind(classOf[HelloWorldResource])
        bind(classOf[StaticWrapperServlet]); 
        serve("*.ico", "*.css", "*.js", "*.gif", "*.png", "*.html").`with`(classOf[StaticWrapperServlet])
        //// can't get this to work in Scala
        //bind(classOf[MessageBodyWriter[NodeSeq]]).to(classOf[NodeWriter]) 
        //serve("/*").`with`(classOf[GuiceContainer]);
        val params:java.util.Map[String, String] = Map(
          "javax.ws.rs.Application" -> "org.yawni.wordnet.YawniApplication"
          ,ResourceConfig.FEATURE_IMPLICIT_VIEWABLES -> "true"
          //,"com.sun.jersey.spi.container.ContainerRequestFilters" -> "com.sun.jersey.api.container.filter.LoggingFilter"
          //,"com.sun.jersey.spi.container.ContainerResponseFilters" -> "com.sun.jersey.api.container.filter.LoggingFilter"
          //,ResourceConfig.FEATURE_TRACE -> "true"
          ).asJava
        serve("/*").`with`(classOf[GuiceContainer], params);
      }
    })
  }
}
