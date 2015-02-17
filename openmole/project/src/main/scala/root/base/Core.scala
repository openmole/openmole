package root.base

import root.BaseDefaults
import sbt._
import Keys._
import org.openmole.buildsystem.OMKeys._

object Core extends BaseDefaults {

  import Misc._
  import root.ThirdParties._
  import root.Libraries._
  import root.libraries.Apache

  implicit val artifactPrefix = Some("org.openmole.core")

  override val dir = file("core/core")

  lazy val workflow = OsgiProject("workflow", openmoleScope = Some("provided"), imports = Seq("*")) settings (
    includeOsgi,
    libraryDependencies ++= Seq(scalaLang, groovy, Apache.math, scalatest)
  ) dependsOn
    (Misc.eventDispatcher, Misc.exception, Misc.tools, Misc.updater, Misc.workspace, Misc.macros, Misc.pluginManager, serializer, Misc.replication % "test")

  lazy val serializer = OsgiProject("serializer", openmoleScope = Some("provided"), imports = Seq("*")) settings
    (includeOsgi,
      libraryDependencies += xstream) dependsOn
      (workspace, pluginManager, fileService, Misc.tools, iceTar)

  lazy val batch = OsgiProject("batch", openmoleScope = Some("provided"), imports = Seq("*")) dependsOn (
    workflow, workspace, Misc.tools, eventDispatcher, replication, updater, Misc.exception,
    serializer, fileService, pluginManager, iceTar) settings (libraryDependencies ++= Seq(gridscale, h2, guava, jasypt, slick, Apache.config))

  lazy val dsl = OsgiProject("dsl", openmoleScope = Some("provided"), imports = Seq("*")) dependsOn (workflow, Misc.logging)

}
