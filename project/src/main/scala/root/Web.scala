package root

import sbt._
import Keys._

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/17/13
 * Time: 9:06 PM
 * To change this template use File | Settings | File Templates.
 */
package object web extends Defaults {
  import base._
  import libraries._

  lazy val dir = file("web")
  override lazy val org = organization := "org.openmole.web"
  lazy val all = Project("web", dir) aggregate (core)

  lazy val core = OsgiProject("org.openmole.web.core", "core",
    exports = Seq("org.openmole.web"),
    buddyPolicy = Some("global"),
    imports = Seq("org.h2.*", "*;resolution:=optional")) dependsOn
    (h2, jetty, slick, logback, scalatra, bonecp, scalaLang, base.core.model, base.core.serializer, xstream)
}