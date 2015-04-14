package root.runtime

import root.{ Defaults, _ }
import sbt.Keys._
import sbt._
import org.openmole.buildsystem.OMKeys._

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/17/13
 * Time: 9:06 PM
 * To change this template use File | Settings | File Templates.
 */
object REST extends Defaults {
  import Libraries._
  import ThirdParties._

  val dir = file("runtime/rest")
  implicit val artifactPrefix = Some("org.openmole.rest")

  lazy val messages = Project("org-openmole-rest-messages", dir / "messages")

  lazy val server = OsgiProject(
    "server",
    privatePackages = Seq("org.openmole.rest.messages.*"),
    imports = Seq("org.h2", "!com.sun.*", "*")) dependsOn
    (Core.workflow, iceTar, root.Runtime.console, messages) settings
    (libraryDependencies ++= Seq(bouncyCastle, jetty, logback, scalatra, scalaLang, arm, codec))

  lazy val client = Project("org-openmole-rest-client", dir / "client") settings (
    libraryDependencies += "org.apache.httpcomponents" % "httpclient" % "4.3.5",
    libraryDependencies += "org.apache.httpcomponents" % "httpmime" % "4.3.5",
    libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.10"
  ) dependsOn (messages)

}
