package root.libraries

import root.Defaults
import sbt._
import sbt.Keys._
import org.openmole.buildsystem.OMKeys._

object Apache extends Defaults {

  //todo: switch to discluding pattern
  val dir = file("libraries") / "apache"

  override def OsgiSettings = super.OsgiSettings ++ Seq(bundleType := Set("core", "lib")) //TODO make library defaults

  lazy val pool = "org.openmole" %% "org-apache-commons-pool" % "1.5.4"

  lazy val config = "org.openmole" %% "org-apache-commons-configuration" % "1.6"

  lazy val math = "org.openmole" %% "org-apache-commons-math" % "3.2"

  lazy val exec = "org.openmole" %% "org-apache-commons-exec" % "1.1"

  lazy val log4j = "org.openmole" %% "org-apache-log4j" % "1.2.17"

  lazy val logging = "org.openmole" %% "org-apache-commons-logging" % "1.1.1"

  lazy val sshd = "org.openmole" %% "org-apache-sshd" % "0.11.0"

  lazy val ant = "org.openmole" %% "org-apache-ant" % "1.8.0"

  lazy val codec = "org.openmole" %% "org-apache-commons-codec" % "1.5"
}
