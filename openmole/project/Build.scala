import sbt._
import Keys._

import root._


object Root extends Defaults(Base, Libraries, Gui, ThirdParties, Web, Application, Bin) {
  implicit val dir = file(".")
  lazy val all = Project(id = "root", base = dir) aggregate (subProjects: _*)

  override def settings = super.settings ++ Seq(
    //make openmole repo the resolver of last resort
    resolvers += DefaultMavenRepository,
    resolvers += "openmole-public" at "http://maven.openmole.org/public",
    resolvers += Resolver.sonatypeRepo("snapshots")
  )
}
