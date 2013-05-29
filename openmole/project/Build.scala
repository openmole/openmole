import sbt._
import Keys._

import root._


object HelloBuild extends Defaults {
  implicit val dir = file(".")
  lazy val all = Project(id = "root", base = dir) aggregate
    (Libraries.all, Web.all, Application.all, Base.all, ThirdParties.all, Gui.all) //todo: meta should be application.meta

  override def settings = super.settings ++ Seq(
    //make openmole repo the resolver of last resort
    resolvers ++= Seq(DefaultMavenRepository,"openmole-public" at "http://maven.openmole.org/public")
  )
}
