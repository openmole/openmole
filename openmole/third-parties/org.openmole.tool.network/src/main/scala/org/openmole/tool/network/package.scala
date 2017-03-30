package org.openmole.tool

import java.net.InetAddress

import scala.util.Try

package object network {

  // It is set to localhost for mac since the hostname might be set via DHCP
  def fixHostName =
    if (System.getProperty("os.name").toLowerCase.contains("mac")) "localhost"
    else Try { InetAddress.getLocalHost().getHostName() }.getOrElse("localhost")

}
