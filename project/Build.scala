import sbt._
import Keys._

import root._


object HelloBuild extends Defaults {
  implicit val dir = file(".")
  lazy val all = Project(id = "root", base = dir) aggregate
    (libraries.all, web.all, application.all, base.all, thirdparties.all, gui.all) //todo: all should be application.all

  override def settings = super.settings ++ Seq(
    resolvers += "openmole-public" at "http://maven.openmole.org/public"
  )
}
