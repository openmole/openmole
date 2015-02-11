package org.openmole.buildsystem

import sbt._
import Keys._
import OMKeys._
import com.typesafe.sbt.SbtScalariform.{ scalariformSettings, ScalariformKeys }

import scalariform.formatter.preferences._

import sbt.inc.Analysis

trait BuildSystemDefaults extends Build with OsgiBundler with Assembly {
  def dir: File

  def org: String

  def projectName: String

  lazy val projectRefs: Seq[ProjectReference] = super.projects.map(_.project) ++ subProjects

  def subProjects: Seq[ProjectReference] = Nil

  val credential = Path.userHome / ".sbt" / "openmole.credentials"

  lazy val commonsSettings =
    Seq(scalacOptions ++= Seq("-feature", "-language:reflectiveCalls", "-language:implicitConversions",
      "-language:existentials", "-language:postfixOps", "-Yinline-warnings")
    ) ++ (if (credential.exists()) Seq(credentials += Credentials(credential)) else Seq.empty) ++ scalariformDefaults

  protected lazy val scalariformDefaults =
    Seq(
      ScalariformKeys.preferences in Compile <<= ScalariformKeys.preferences(p ⇒
        p.setPreference(DoubleIndentClassDeclaration, true)
          .setPreference(RewriteArrowSymbols, true)
          .setPreference(AlignParameters, true)
          .setPreference(AlignSingleLineCaseStatements, true)
          .setPreference(CompactControlReadability, true)
          .setPreference(PreserveDanglingCloseParenthesis, true)
      )
    ) ++ scalariformSettings

}
