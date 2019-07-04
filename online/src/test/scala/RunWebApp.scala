import org.eclipse.jetty.server.Server
import org.eclipse.jetty.webapp.WebAppContext
import org.eclipse.jetty.server.ServerConnector

object RunWebApp extends App {
  val server = new Server
  val http = new ServerConnector(server)
  http.setPort(8080)
  server.setConnectors(Array(http))

  val context = new WebAppContext()
  context.setServer(server)
  context.setContextPath("/")
  //FIXME hardcoded path
  context.setWar("target/yawni-wordnet-online-2.0.0-SNAPSHOT")

  server.setHandler(context)

  try {
    println(">>> STARTING EMBEDDED JETTY SERVER, PRESS ANY KEY TO STOP")
    server.start()
    while (System.in.available() == 0) {
      Thread.sleep(5000)
    }
    server.stop()
    server.join()
  } catch {
    case exc : Exception => {
      exc.printStackTrace()
      System.exit(100)
    }
  }
}
