package root

import sbt._
import Keys._
import root._
import sbtunidoc.Plugin._
import UnidocKeys._
import sbt.inc.Analysis

object Installer extends Defaults(Application, Base, Gui, Web) {
  val dir = file("installer")

  lazy val docProj = Project("documentation", dir / "documentation") aggregate (subProjects: _*) settings (
    unidocSettings: _*
  ) settings (compile := Analysis.Empty, scalacOptions in (ScalaUnidoc, unidoc) += "-Ymacro-no-expand",
      unidocProjectFilter in (ScalaUnidoc, unidoc) := inAnyProject -- inProjects(Libraries.subProjects: _*) -- inProjects(ThirdParties.subProjects: _*)
    )
}
