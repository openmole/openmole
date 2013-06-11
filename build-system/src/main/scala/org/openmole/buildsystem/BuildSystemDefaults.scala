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

  def all: Project //An aggregate project that other parts of the build system use

  override def settings = super.settings ++
    Seq(scalacOptions ++= Seq("-feature", "-language:reflectiveCalls", "-language:implicitConversions",
      "-language:existentials", "-language:postfixOps", "-Yinline-warnings"),
      osgiVersion := "3.8.2.v20130124-134944"
    )

  def gcTask { System.gc(); System.gc(); System.gc() }

  def Aggregator(name: String) = Project(name, dir) settings (compile in Compile := Analysis.Empty)

  protected lazy val scalariformDefaults = Seq(ScalariformKeys.preferences in Compile <<= ScalariformKeys.preferences(p ⇒
    p.setPreference(DoubleIndentClassDeclaration, true)
      .setPreference(RewriteArrowSymbols, true)
      .setPreference(AlignParameters, true)
      .setPreference(AlignSingleLineCaseStatements, true)
      .setPreference(CompactControlReadability, true)
      .setPreference(PreserveDanglingCloseParenthesis, true))) ++ scalariformSettings

  def provided(p: Project) = p % "provided"
}
