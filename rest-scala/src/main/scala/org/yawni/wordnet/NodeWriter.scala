package org.yawni.wordnet

import java.io.OutputStream
import java.lang.annotation.Annotation

import javax.ws.rs.core.{MultivaluedMap, MediaType, Context}
import javax.ws.rs.ext.{MessageBodyWriter, Provider, Providers}
import java.lang.reflect.Type
//import com.sun.jersey.spi.resource.Singleton
//import com.google.inject.Provides

import scala.xml.NodeSeq

/**
 * Converts a Scala {@link NodeSeq} to a String for rendering nodes as HTML, XML, XHTML, etc.
 *
 * @version $Revision: 1.1 $
 * copied from jersey contrib
 */
@Provider
//@Provides
//@Singleton
class NodeWriter extends MessageBodyWriter[NodeSeq] {
  //@Context protected var providers: Providers = null;

  override
  def isWriteable(aClass: Class[_], aType: Type, annotations: Array[Annotation], mediaType: MediaType): Boolean = {
    classOf[NodeSeq].isAssignableFrom(aClass)
  }

  override
  def getSize(nodes: NodeSeq, aClass: Class[_], aType: Type, annotations: Array[Annotation], mediaType: MediaType): Long = -1L

  override
  def writeTo(nodes: NodeSeq, aClass: Class[_], aType: Type, annotations: Array[Annotation], mediaType: MediaType, stringObjectMultivaluedMap: MultivaluedMap[String, Object], outputStream: OutputStream) : Unit = {
    val answer = nodes.toString()
    outputStream.write(answer.getBytes("utf-8"))
  }
}