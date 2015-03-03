package root.libraries

import root.Defaults
import sbt._
import sbt.Keys._
import org.openmole.buildsystem.OMKeys._

object Apache extends Defaults {

  //todo: switch to discluding pattern
  val dir = file("target/libraries") / "apache"

  lazy val config = OsgiProject("org.apache.commons.configuration", privatePackages = Seq("org.apache.commons.*")) settings
    (libraryDependencies += "commons-configuration" % "commons-configuration" % "1.10", version := "1.10")

  lazy val mathVersion = "3.4.1"
  lazy val math = OsgiProject("org.apache.commons.math", exports = Seq("org.apache.commons.math3.*"), privatePackages = Seq("assets.*")) settings
    (libraryDependencies += "org.apache.commons" % "commons-math3" % mathVersion, version := mathVersion)

  lazy val exec = OsgiProject("org.apache.commons.exec") settings
    (libraryDependencies += "org.apache.commons" % "commons-exec" % "1.1", version := "1.1")

  lazy val log4j = OsgiProject("org.apache.log4j") settings
    (libraryDependencies += "log4j" % "log4j" % "1.2.17", version := "1.2.17")

  lazy val logging = OsgiProject("org.apache.commons.logging") settings
    (libraryDependencies += "commons-logging" % "commons-logging" % "1.1.1", version := "1.1.1")

  lazy val sshd = OsgiProject("org.apache.sshd", exports = Seq("org.apache.sshd.*", "org.apache.mina.*"), dynamicImports = Seq("*"), privatePackages = Seq("META-INF.*")) settings
    (libraryDependencies += "org.apache.sshd" % "sshd-core" % "0.13.0", version := "0.13.0")

  lazy val ant = OsgiProject("org.apache.ant") settings
    (libraryDependencies += "org.apache.ant" % "ant" % "1.8.0", version := "1.8.0")

  lazy val codec = OsgiProject("org.apache.commons.codec") settings
    (libraryDependencies += "commons-codec" % "commons-codec" % "1.10", version := "1.10")
}
