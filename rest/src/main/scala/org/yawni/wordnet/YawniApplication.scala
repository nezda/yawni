package org.yawni.wordnet

import javax.ws.rs.core._
import com.google.common.collect.Sets._

class YawniApplication extends Application {
  override
  def getClasses() = newHashSet(
    classOf[SearchResource], 
    classOf[APIResource], 
    classOf[AutocompleteResource], 
    classOf[AboutResource],
    classOf[HelloWorldResource],
    classOf[NodeWriter])
}
