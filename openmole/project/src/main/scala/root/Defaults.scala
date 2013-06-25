package root

import org.openmole.buildsystem._
import OMKeys._

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

  val projectName = "openmole"

  def org = "org.openmole"

  override def settings = super.settings ++
    Seq(version := "0.9.0-SNAPSHOT",
      scalaVersion := "2.10.2",
      publishArtifact in (packageDoc in install) := false,
      publishArtifact in (packageSrc in install) := false,
      concurrentRestrictions in Global :=
        Seq(
          Tags.limitAll(9)
        )
    )
}
