package org.openmole.tool

import java.net.{ InetAddress, Socket }

import scala.util._

package object network {

  // It is set to localhost for mac since the hostname might be set via DHCP
  def fixHostName =
    if (System.getProperty("os.name").toLowerCase.contains("mac")) "localhost"
    else Try { InetAddress.getLocalHost().getHostName() }.getOrElse("localhost")

  def isPortAcceptingConnections(host: String, port: Int) = {
    val s = Try(new Socket(host, port))
    s match {
      case Success(s) =>
        s.close(); true
      case Failure(_) => false
    }
  }

}
