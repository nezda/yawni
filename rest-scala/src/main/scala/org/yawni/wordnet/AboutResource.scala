package org.yawni.wordnet

import javax.ws.rs._
import javax.ws.rs.core._
import scala.xml.Elem

@Path("/about")
class AboutResource {
  // report something like org.yawni.wordnet.browser.Application
  //
  // really should generalize Application to parse a properties:
  //
  // better name:
  //  "ProjectProperties" / "BuildInfo" / "ProjectInfo"
  // "DOAP: Description of a Project" an XML/RDF vocabulary to describe software projects, and in particular open source
  // 
  //
  // - remove default class (currently it's this - make this a param)
  // - leave default properties file name (e.g., "application.properties")
  //
  // other good server stats include lift stats like those shown in the example default.html:
  //  <div class="column span-23 last" style="text-align: center">
  //  <h4 class="alt"><a href='http://liftweb.net'><i>Lift</i></a> is Copyright 2007-2010 WorldWide Conferencing, LLC.  Distributed under an Apache 2.0 License.
  //    <br/>
  //    Lift version <lift:version_info.lift/> built on <lift:version_info.date/>.
  //    <br/>
  //    Stats: Total Memory: <lift:runtime_stats:total_mem/>
  //    Free Memory: <lift:runtime_stats:free_mem/>
  //    Open Sessions: <lift:runtime_stats:sessions/>
  //    Updated At: <lift:runtime_stats:updated_at/>
  //  </h4>
  //</div>

  //  read in META-INF/maven/org.yawni/yawni-wordnet-data30/pom.properties

  @GET
  @Produces(Array(MediaType.APPLICATION_XML))
  def aboutResponse(): Elem = {
    <about>
      Yawni Online
      <serverStats>
        <totalMemory>{ f"${Runtime.getRuntime.totalMemory}%,d" }</totalMemory>
        <freeMemory>{ f"${Runtime.getRuntime.freeMemory}%,d" }</freeMemory>
        <maxMemory>{ f"${Runtime.getRuntime.maxMemory}%,d" }</maxMemory>
      </serverStats>
    </about>
    // Open Sessions: <lift:runtime_stats:sessions/>
    // Updated At: <lift:runtime_stats:updated_at/>
  }
}