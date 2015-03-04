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

  lazy val core = OsgiProject(
    "org.openmole.web.core",
    "core",
    exports = Seq("org.openmole.web.*"),
    dynamicImports = Seq("*"),
    imports = Seq("org.h2.*", "*;resolution:=optional")) dependsOn
    (Core.workflow, Core.serializer, iceTar, misc) settings
    (libraryDependencies ++= Seq(bouncyCastle, h2, jetty, slick, logback, scalatra, scalate, bonecp, scalaLang, xstream, jacksonJson, arm, codec))

  lazy val misc = OsgiProject("org.openmole.web.misc.tools", "misc/tools") dependsOn
    (Core.workspace) settings (libraryDependencies ++= Seq(scalajHttp, arm))

  override def osgiSettings = super.osgiSettings ++ Seq(bundleType := Set("core"))
}
