import sbt._
import Keys._

import root.Web._
import root.Application._
import root.ThirdParties._
import root.Defaults
import root._


object HelloBuild extends Defaults {
  implicit val dir = file(".")
  lazy val root = Project(id = "root", base = dir) aggregate
    (libraries.all, web, application, base.all, thirdParties)

  override def settings = super.settings ++ Seq(
    resolvers += "openmole-public" at "http://maven.openmole.org/public"
  )
}
