import org.eclipse.jetty.server.{Server, ServerConnector}
import org.eclipse.jetty.webapp.WebAppContext


object RunWebApp extends App {
  val server = new Server
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
  server.join()
}
