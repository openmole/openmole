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

  val cred = Path.userHome / ".sbt" / "openmole.credentials"

  override def settings = super.settings ++
    Seq(scalacOptions ++= Seq("-feature", "-language:reflectiveCalls", "-language:implicitConversions",
      "-language:existentials", "-language:postfixOps", "-Yinline-warnings"),
      osgiVersion := "3.8.2.v20130124-134944"
    ) ++ (if (cred.exists()) Seq(credentials += Credentials(cred)) else Seq.empty)

  def gcTask { System.gc(); System.gc(); System.gc() }

  def Aggregator(name: String) = Project(name, dir) settings (compile in Compile := Analysis.Empty, install := false, assemble := false)

  protected lazy val scalariformDefaults = Seq(ScalariformKeys.preferences in Compile <<= ScalariformKeys.preferences(p ⇒
    p.setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(CompactControlReadability, true)
      .setPreference(PreserveDanglingCloseParenthesis, true))) ++ scalariformSettings

  def provided(p: Project) = p % "provided"
}
