package root

import sbt._
import Keys._
import org.openmole.buildsystem.OMKeys._

object ThirdParties extends Defaults {

  lazy val dir = file("third-parties")

  lazy val iceTar = OsgiProject("com.ice.tar")

  lazy val nioUtils = OsgiProject("bbejeck.nio")

  lazy val scalaSwing =
    OsgiProject("org.scala-lang.scala-swing", exports = Seq("scala.swing.*", "scala.actors.*")) settings
      (libraryDependencies <+= scalaVersion { v ⇒ "org.scala-lang.modules" %% "scala-swing" % "1.0.1" })

  override def OsgiSettings = super.OsgiSettings ++ Seq(bundleType := Set("core", "libs"))

}
