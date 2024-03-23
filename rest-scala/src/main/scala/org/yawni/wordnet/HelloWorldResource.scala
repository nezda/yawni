package org.yawni.wordnet

import javax.ws.rs._
import javax.ws.rs.core._

@Path("/helloworld")
class HelloWorldResource {
  @GET
  //@Produces(Array(MediaType.TEXT_PLAIN))
  def sayHello(): String = {
    "Hello, world!"
  }
}