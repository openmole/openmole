package org.openmole.rest.server

import org.openmole.rest.server.db.SlickDB
import org.scalatra._
import javax.servlet.ServletContext
import com.typesafe.config.ConfigFactory
import slick.driver.H2Driver.simple._
import java.sql.{ Blob, Clob }

/**
 * This is the Scalatra bootstrap file. You can use it to mount servlets or
 * filters. It's also a good place to put initialization code which needs to
 * run at application start (e.g. database configurations), and init params.
 */
class Scalatra extends LifeCycle {

  override def init(context: ServletContext) {
    //val db = context.getAttribute("database").asInstanceOf[SlickDB]
    context.mount(new RESTAPI {}, "/*")
  }

}

