package root

import sbt._
import Keys._
import org.openmole.buildsystem.OMKeys._
import scala.Some

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/17/13
 * Time: 9:06 PM
 * To change this template use File | Settings | File Templates.
 */
object Web extends Defaults {
  import Libraries._
  import ThirdParties._

  val dir = file("web")
  override val org = "org.openmole.web"

  lazy val core = OsgiProject("org.openmole.web.core", "core",
    exports = Seq("org.openmole.web"),
    buddyPolicy = Some("global"),
    imports = Seq("org.h2.*", "*;resolution:=optional")) dependsOn
    (h2, jetty, slick, logback, scalatra, bonecp, scalaLang, base.Core.implementation, base.Core.serializer, xstream, jacksonJson, iceTar, arm)

  lazy val misc = OsgiProject("org.openmole.web.misc.tools", "misc/tools",
    exports = Seq("org.openmole.web.misc.tools")) dependsOn
    (scalajHttp)

  override def OsgiSettings = super.OsgiSettings ++ Seq(bundleType := Set("core"))
}