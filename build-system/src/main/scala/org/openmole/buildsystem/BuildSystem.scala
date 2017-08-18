package org.openmole.buildsystem

import sbt._
import Keys._
import OMKeys._
import com.typesafe.sbt.SbtScalariform.{ scalariformSettings, ScalariformKeys }

import scalariform.formatter.preferences._

import sbt.inc.Analysis

object BuildSystem {

  val credential = Path.userHome / ".sbt" / "openmole.credentials"

  lazy val settings =
    (if (credential.exists()) Seq(credentials += Credentials(credential)) else Seq.empty) ++ scalariformDefaults

  protected lazy val scalariformDefaults =
    Seq(
      ScalariformKeys.preferences in Compile := ScalariformKeys.preferences(p ⇒
        p.setPreference(DoubleIndentClassDeclaration, true)
          .setPreference(RewriteArrowSymbols, true)
          .setPreference(AlignParameters, true)
          .setPreference(AlignSingleLineCaseStatements, true)
          .setPreference(CompactControlReadability, true)).value) ++ scalariformSettings

}
