import org.eclipse.jetty.server.{Server, ServerConnector}
import org.eclipse.jetty.webapp.WebAppContext


object RunWebApp extends App {
  val server = new Server
  // https://xy2401.com/local-docs/java/jetty.9.4.24.v20191120/session-configuration-housekeeper.html
  // no effect server.setAttribute("jetty.sessionIdManager.workerName", "yawni0")
  val http = new ServerConnector(server)
  http.setPort(8080)
  server.setConnectors(Array(http))

  val context = new WebAppContext()
  context.setServer(server)
  context.setContextPath("/")
  //FIXME hardcoded path
  context.setWar("rest-scala/target/yawni-wordnet-rest-scala-2.0.0-SNAPSHOT/")

  server.setHandler(context)

  server.start()
  // Keep the main thread alive while the server is running.
  //  XXX needed? from https://happycoding.io/tutorials/java-server/embedded-jetty
  server.join()

//  try {
//    println(">>> STARTING EMBEDDED JETTY SERVER, PRESS ANY KEY TO STOP")
//    server.start()
//    while (System.in.available() == 0) {
//      Thread.sleep(5000)
//    }
//    server.stop()
//    server.join()
//  } catch {
//    case exc : Exception => {
//      exc.printStackTrace()
//      System.exit(100)
//    }
//  }
}
