package root

import com.typesafe.sbt.osgi.OsgiKeys
import sbt._
import Keys._
import org.openmole.buildsystem.OMKeys._

object ThirdParties extends Defaults {

  lazy val dir = file("third-parties")

  lazy val iceTar = OsgiProject("com.ice.tar") settings (bundleType := Set("core"))

  lazy val toolxitBibtexMacros = OsgiProject("toolxit.bibtex.macros", "toolxit.bibtex/macros") settings (
//    bundleType := Set("core"),
    libraryDependencies <+= scalaVersion("org.scala-lang" % "scala-reflect" % _)
  )

  lazy val toolxitBibtex = OsgiProject("toolxit.bibtex.core", "toolxit.bibtex/core", exports = Seq("toolxit.bibtex.*"), imports = Seq("*"), privatePackages = Seq("!scala.*", "*")) dependsOn (toolxitBibtexMacros) settings (
//    bundleType := Set("core"),
    OsgiKeys.importPackage := Seq("*"),
    libraryDependencies ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.3",
      "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.3",
      "org.freemarker" % "freemarker" % "2.3.19",
      Libraries.slf4j
    )
  )

}
