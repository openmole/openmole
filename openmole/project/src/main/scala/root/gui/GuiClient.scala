package root.gui

import sbt._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import root.{ GuiDefaults }
import root.Libraries._

object Client extends GuiDefaults {
  override val dir = super.dir / "client"

  val jqueryPath = s"META-INF/resources/webjars/jquery/$jqueryVersion/jquery.js"
  val acePath = s"META-INF/resources/webjars/ace/$aceVersion/src-min/ace.js"

  lazy val core = OsgiProject("org.openmole.gui.client.core") enablePlugins (ScalaJSPlugin) dependsOn
    (Ext.dataui, Shared.shared, Misc.utils, Misc.js , root.Doc.doc ) settings (
      libraryDependencies ++= Seq(autowire, upickle, scalaTags, rx, scalajsDom, scaladget, async),
      skip in packageJSDependencies := false,
      jsDependencies += jquery / jqueryPath,
      jsDependencies += ace / acePath,
      jsDependencies += ace / "src-min/mode-sh.js" dependsOn acePath,
      jsDependencies += ace / "src-min/mode-scala.js" dependsOn acePath,
      jsDependencies += ace / "src-min/theme-github.js" dependsOn acePath,
      jsDependencies += bootstrap / "js/bootstrap.min.js" dependsOn jqueryPath,
      jsDependencies += d3 / "d3.min.js" dependsOn jqueryPath,
      jsDependencies += tooltipster / "js/jquery.tooltipster.min.js" dependsOn jqueryPath
    )
}