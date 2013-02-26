package org.openmole.web

import _root_.akka.actor.{ Props, ActorSystem }
import org.scalatra._
import javax.servlet.ServletContext
import org.openmole.web.MyServlet
import org.openmole.web.MyRepoServlet
import com.typesafe.config.ConfigFactory

/**
 * This is the Scalatra bootstrap file. You can use it to mount servlets or
 * filters. It's also a good place to put initialization code which needs to
 * run at application start (e.g. database configurations), and init params.
 */
class Scalatra extends LifeCycle {

  val system = ActorSystem("WebEnvironment", ConfigFactory.parseString(
    """
      akka {
        daemonic="on"
        actor {
          default-dispatcher {
            executor = "fork-join-executor"
            type = Dispatcher

            fork-join-executor {
              parallelism-min = 5
              parallelism-max = 10
            }
          }
        }
      }
    """).withFallback(ConfigFactory.load(classOf[ConfigFactory].getClassLoader)))
  //val myActor = system.actorOf(Props[MyActor])

  override def init(context: ServletContext) {

    // Mount one or more servlets
    context.mount(new MyServlet(system), "/*")
    context.mount(new SlickRoutes(), "/c/*")
    context.mount(new MyRepoServlet(system), "/repo/*")
  }

  override def destroy(context: ServletContext) {
    system.shutdown()
  }
}
