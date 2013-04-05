package root

import sbt._
import Keys._

object ThirdParties extends Defaults {

  lazy val dir = file("third-parties")

  lazy val thirdParties = Project("thirdParties", dir) aggregate(iceTar, scalaSwing, scopt)

  lazy val iceTar = OsgiProject("com.ice.tar")

  lazy val scalaSwing= OsgiProject("org.scala-lang.scala-swing", exports = Seq("scala.swing.*", "scala.actors.*")) settings
    (libraryDependencies <+= scalaVersion {sV => "org.scala-lang" % "scala-swing" % sV})

  lazy val scopt = OsgiProject("com.github.scopt", exports = Seq("scopt.*"))
}