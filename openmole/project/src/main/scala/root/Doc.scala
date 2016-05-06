package root

import org.scalajs.sbtplugin.ScalaJSPlugin
import org.openmole.buildsystem.OMKeys._
import root.Libraries._
import root.gui.Misc
import sbt.Keys._
import sbt._

object Doc extends Defaults {
  override def dir = file("doc")

  lazy val doc = OsgiProject("org.openmole.doc") enablePlugins (ScalaJSPlugin) settings (
    scalajsDomJS,
    scalaTagsJS,
    scaladgetJS,
    rxJS,
    libraryDependencies += scalaTags
  )
  override def osgiSettings = super.osgiSettings ++ Seq(bundleType := Set("doc"))

}

