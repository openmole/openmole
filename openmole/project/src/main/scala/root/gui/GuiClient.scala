package root.gui

import sbt._
import root.{ GuiDefaults, base }
import root.Libraries._
import root.ThirdParties._
import fr.iscpif.jsmanager.JSManagerPlugin._

object Client extends GuiDefaults {
  override val dir = super.dir / "client"

  lazy val client = OsgiProject("org.openmole.gui.client") dependsOn
    (autowire, scalaTags, scalaRx, scalajsDom, Tools.js, Shared.shared, Ext.dataui) settings (
      jsCall := "Plot().run();",
      outputPath := super.dir.getAbsolutePath + "/server/src/main/webapp/"
    )
}