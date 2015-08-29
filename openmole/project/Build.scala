import sbt._
import Keys._

import root._


object Root extends Defaults(Core, Libraries, Gui, ThirdParties, Bin, root.Doc) {
  implicit val dir = file(".")
  lazy val all = Project(id = "root", base = dir) aggregate (subProjects: _*)


  override def settings = super.settings ++ Seq(
    resolvers += DefaultMavenRepository,
    resolvers += "openmole-public" at "http://maven.openmole.org/public",
    resolvers += Resolver.sonatypeRepo("snapshots"),
    resolvers += Resolver.sonatypeRepo("releases"),
    publish := ()
  )
}