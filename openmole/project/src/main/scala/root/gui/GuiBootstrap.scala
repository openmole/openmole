package root.gui

import org.openmole.buildsystem.OMKeys._

import sbt._
import root._
import root.Libraries._
import sbt.Keys._
import com.typesafe.sbt.osgi.OsgiKeys._

object Bootstrap extends GuiDefaults {
  override val dir = super.dir / "bootstrap"

  implicit def webJarResourcePath(moduleIDs: Seq[ModuleID]): Seq[String] = moduleIDs.flatMap {
    m ⇒
      Seq(
        "!META-INF.resources.webjars." + m.name + "." + m.revision + ".src.*",
        "!META-INF.resources.webjars." + m.name + "." + m.revision + ".fonts.*",
        "!META-INF.resources.webjars." + m.name + "." + m.revision + ".grunt.*",
        "!META-INF.resources.webjars." + m.name + "." + m.revision + ".less.*",
        "!META-INF.resources.webjars." + m.name + "." + m.revision + ".js.*",
        "!META-INF.resources.webjars." + m.name + "." + m.revision + ".dist.fonts.*",
        "!META-INF.resources.webjars." + m.name + "." + m.revision + ".dist.css.*",
        "!META-INF.resources.webjars." + m.name + "." + m.revision + ".textarea.*",
        "!META-INF.resources.webjars." + m.name + "." + m.revision + ".src-noconflict.*",
        "!META-INF.resources.webjars." + m.name + "." + m.revision + ".src-min-noconflict.*",
        "!META-INF.resources.webjars." + m.name + "." + m.revision + ".kitchen-sink.*",
        "!META-INF.resources.webjars." + m.name + "." + m.revision + ".demo.*",
        "!META-INF.resources.webjars." + m.name + "." + m.revision + ".src-min.snippets.*",
        "META-INF.resources.webjars." + m.name + "." + m.revision + ".*"
      )
  }

  lazy val webJarsResources = Seq(d3, bootstrap, jquery, ace)

  lazy val js = OsgiProject("org.openmole.gui.bootstrap.js", privatePackages = webJarsResources) settings
    (libraryDependencies ++= Seq(scalajsTools, scalajsDom, autowire, scalaTags, rx, upickle) ++ webJarsResources) dependsOn
    (Server.core, Client.core, Core.pluginManager, Core.workspace, Core.tools, Core.fileService)

  lazy val osgi = OsgiProject("org.openmole.gui.bootstrap.osgi") dependsOn
    (Server.core, Client.core, Ext.data, Core.workflow)
}
