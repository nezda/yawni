package org.yawni.wordnet

import javax.ws.rs.core._
import com.google.common.collect.Sets._

import java.util

class YawniApplication extends Application {
  override
  def getClasses: util.Set[Class[_]] = newHashSet(
    classOf[SearchResource], 
    classOf[APIResource], 
    classOf[AutocompleteResource], 
    classOf[AboutResource],
    classOf[HelloWorldResource],
    classOf[NodeWriter])
}