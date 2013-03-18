import sbt._
import Keys._

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/17/13
 * Time: 9:06 PM
 * To change this template use File | Settings | File Templates.
 */
trait Web extends libraries with Defaults {
  lazy val web = Project(id = "web", base = file("web")) aggregate(webCore)

  lazy val webCore = Project(id = "web-core", base = file("web/core"),
    settings = OSGIProject("org.openmole.web.core",
      exports = Seq("org.openmole.web"),
      buddyPolicy = Some("global"),
      privatePackages = Seq("WEB-INF.*")) ++ Seq(libraryDependencies ++= Seq(
        "org.openmole.core" % "org.openmole.core.serializer" % "0.8.0-SNAPSHOT",
        "org.openmole.core" % "org.openmole.core.implementation" % "0.8.0-SNAPSHOT",
        "org.openmole.ide" % "org.openmole.ide.core.implementation" % "0.8.0-SNAPSHOT",
        "org.openmole" % "org.scala-lang.scala-library" % "0.8.0-SNAPSHOT",
        "org.openmole" % "org.scalatra" % "0.8.0-SNAPSHOT",
        "org.openmole" % "org.eclipse.jetty" % "0.8.0-SNAPSHOT",
        "org.openmole" % "com.typesafe.slick" % "0.8.0-SNAPSHOT"
    ))) dependsOn(logback, h2, bonecp)
}