package root.gui

import sbt._
import sbt.Keys._
import root.{ GuiDefaults, base }
import root.Libraries._
import root.ThirdParties._
import fr.iscpif.jsmanager.JSManagerPlugin._

object Client extends GuiDefaults {
  override val dir = super.dir / "client"

  lazy val client = OsgiProject("org.openmole.gui.client") dependsOn
    (Tools.tools, Shared.shared, Ext.dataui) settings (jsManagerSettings: _*) settings (
      libraryDependencies ++= Seq(upickleJS, autowireJS, scalaTagsJS, scalaRxJS, scalajsDom, scaladget),
      jsCall := "Plot().run();",
      outputPath := Server.dir + "/src/main/webapp/"
    )
}