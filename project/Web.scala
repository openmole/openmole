import sbt._
import Keys._

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/17/13
 * Time: 9:06 PM
 * To change this template use File | Settings | File Templates.
 */
trait Web extends libraries {
  lazy val web = Project(id = "web", base = file("web")) aggregate(webCore)

  lazy val webCore = Project(id = "web-core", base = file("web/core"),
    settings = OSGIProject("org.openmole.web") ++ Seq(libraryDependencies ++= Seq(
        "org.openmole.core" % "org.openmole.core.serializer" % "0.8.0-SNAPSHOT",
        "org.openmole.core" % "org.openmole.core.implementation" % "0.8.0-SNAPSHOT",
        "org.openmole.ide" % "org.openmole.ide.core.implementation" % "0.8.0-SNAPSHOT",
        "org.scala-tools" % "scala-stm_2.10.0" % "0.6"
    ), resolvers ++= Seq("openmole" at "http://maven.openmole.org/snapshots", "openmole-releases" at "http://maven.openmole.org/public"))) dependsOn(jetty, scalatra, logback, h2, bonecp, slick)
}