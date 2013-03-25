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
trait Web extends Libraries with Core with Defaults {
  private implicit val dir = file("web")
  private[Web] implicit val org = organization := "org.openmole.web"
  lazy val web = Project(id = "web", base = file("web")) aggregate(webCore)

  lazy val webCore = OsgiProject("org.openmole.web.core", "core",
    exports = Seq("org.openmole.web"),
    buddyPolicy = Some("global"),
    imports = Seq("org.h2.*", "*;resolution:=optional")) dependsOn
    (h2, jetty, slick, logback, scalatra, bonecp, scalaLang, coreModel, coreSerializer)
}