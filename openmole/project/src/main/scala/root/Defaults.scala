package root

import org.openmole.buildsystem._
import OMKeys._

import sbt._
import Keys._

object Defaults {
  def macroParadise =
    addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.full)
}

abstract class Defaults(subBuilds: Defaults*) extends BuildSystemDefaults {

  override def subProjects = subBuilds flatMap (_.projectRefs)

  val projectName = "openmole"

  def org = "org.openmole"

  lazy val scalaVersionValue = "2.11.8"

  override def settings = super.settings ++
    Seq(
      scalaVersion in Global := scalaVersionValue,
      scalacOptions ++= Seq("-target:jvm-1.7", "-language:higherKinds"),
      javacOptions ++= Seq("-source", "1.7", "-target", "1.7"),
      publishArtifact in (packageDoc in install) := false,
      publishArtifact in (packageSrc in install) := false,
      addCompilerPlugin("org.scalamacros" %% "paradise" % "2.1.0" cross CrossVersion.fullMapped(_ ⇒ scalaVersionValue))
    )
}
