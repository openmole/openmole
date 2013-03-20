package src.main.scala.projectRoot

import sbt._
import Keys._
import src.main.scala.projectRoot.{libraries, Defaults}

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/17/13
 * Time: 9:06 PM
 * To change this template use File | Settings | File Templates.
 */
trait Web extends libraries with Defaults {
  private implicit val dir = file("web")
  lazy val web = Project(id = "web", base = file("web")) aggregate(webCore)

  lazy val webCore = OsgiProject("org.openole.web.core", "core",
    exports = Seq("org.openmole.web"),
    buddyPolicy = Some("global")) settings(
      libraryDependencies <++= version {v => Seq(
        "org.openmole.core" % "org.openmole.core.serializer" % v,
        "org.openmole.core" % "org.openmole.core.implementation" % v,
        "org.openmole.ide" % "org.openmole.ide.core.implementation" % v,
        "org.openmole" % "org.scala-lang.scala-library" % v,
        "org.openmole" % "org.scalatra" % v,
        "org.openmole" % "org.eclipse.jetty" % v
      )}
    ) dependsOn (slick, logback, h2, bonecp, jetty)
}