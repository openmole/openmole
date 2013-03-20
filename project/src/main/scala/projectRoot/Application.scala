package src.main.scala.projectRoot

import sbt._

trait Application extends Web with libraries {
  lazy val application = Project("application", file("application"))

  lazy val openmoleUI = OSGIProject("org-openmole-ui")
}