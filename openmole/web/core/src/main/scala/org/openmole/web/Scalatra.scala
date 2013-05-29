package org.openmole.web

import _root_.akka.actor.{ Props, ActorSystem }
import org.scalatra._
import javax.servlet.ServletContext
import com.typesafe.config.ConfigFactory
import slick.driver.H2Driver.simple._
import java.sql.Clob

/**
 * This is the Scalatra bootstrap file. You can use it to mount servlets or
 * filters. It's also a good place to put initialization code which needs to
 * run at application start (e.g. database configurations), and init params.
 */
class Scalatra extends LifeCycle {

  val system = ActorSystem("WebEnvironment", ConfigFactory.load(classOf[ConfigFactory].getClassLoader))
  //val myActor = system.actorOf(Props[MyActor])

  override def init(context: ServletContext) {

    // Mount one or more servlets
    context.mount(new MoleRunner(system), "/*")
    context.mount(new SlickRoutes(), "/c/*")
  }

  override def destroy(context: ServletContext) {
    system.shutdown()
  }
}

object MoleData extends Table[(String, String, Clob)]("MoleData") {
  def id = column[String]("ID")
  def state = column[String]("STATE")
  def clobbedMole = column[Clob]("MOLEEXEC")

  def * = id ~ state ~ clobbedMole
}
