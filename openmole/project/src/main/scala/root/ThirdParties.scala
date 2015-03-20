package root

import com.typesafe.sbt.osgi.OsgiKeys
import sbt._
import Keys._
import org.openmole.buildsystem.OMKeys._

object ThirdParties extends Defaults {

  lazy val dir = file("third-parties")

  lazy val iceTar = OsgiProject("com.ice.tar") settings (bundleType := Set("core"))

  lazy val toolxitBibtexMacros = OsgiProject("toolxit.bibtex.macros", "toolxit.bibtex/macros") settings (
    libraryDependencies += Libraries.scalaLang
  )

  lazy val toolxitBibtex = OsgiProject("toolxit.bibtex.core", "toolxit.bibtex/core", exports = Seq("toolxit.bibtex.*", "freemarker.*")) dependsOn (toolxitBibtexMacros) settings (
    libraryDependencies ++= Seq(
      Libraries.scalaLang,
      "org.freemarker" % "freemarker" % "2.3.19",
      Libraries.slf4j
    )
  )

}
