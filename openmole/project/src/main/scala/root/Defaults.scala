package root

import org.openmole.buildsystem.BuildSystemDefaults

import sbt._
import Keys._

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/17/13
 * Time: 6:43 PM
 * To change this template use File | Settings | File Templates.
 */
trait Defaults extends BuildSystemDefaults {
  def dir: File

  def org = "org.openmole"

  override def settings = super.settings ++
    Seq(version := "0.9.0-SNAPSHOT",
      scalaVersion := "2.10.1",
      publishArtifact in (packageDoc in install) := false,
      publishArtifact in (packageSrc in install) := false,
      copyDependencies := false,
      concurrentRestrictions in Global :=
        Seq(
          Tags.limitAll(7)
        )
    )
}
