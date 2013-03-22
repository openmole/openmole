package projectRoot

import sbt._
import Keys._

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/17/13
 * Time: 9:06 PM
 * To change this template use File | Settings | File Templates.
 */
trait Web extends Libraries with Defaults {
  private implicit val dir = file("web")
  private implicit val org = organization := "org.openmole.web"
  lazy val web = Project(id = "web", base = file("web")) aggregate(webCore)

  lazy val webCore = OsgiProject("org.openmole.web.core", "core",
    exports = Seq("org.openmole.web"),
    buddyPolicy = Some("global"),
    imports = Seq("org.h2.*", "*;resolution:=optional")) settings(
      libraryDependencies <++= version {v => Seq(
        "org.openmole.core" % "org.openmole.core.serializer" % v,
        "org.openmole.core" % "org.openmole.core.implementation" % v,
        "org.openmole.ide" % "org.openmole.ide.core.implementation" % v,
        "org.openmole" % "org.scala-lang.scala-library" % v
        //"org.openmole" % "org.scalatra" % v
        //"org.openmole" % "org.eclipse.jetty" % v,
        //"org.openmole" % "org.h2" % v
        //"org.openmole" % "com.typesafe.slick" % v
        //"org.openmole" % "ch.qos.logback" % v
        //"org.openmole" % "com.jolbox.bonecp" % v
      )}
    ) dependsOn(h2, jetty, slick, logback, scalatra, bonecp)
}