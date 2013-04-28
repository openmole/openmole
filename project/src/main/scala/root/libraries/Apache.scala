package root.libraries

import root.Defaults
import sbt._
import sbt.Keys._

package object apache extends Defaults {
  val dir = file("libraries") / "apache"

  lazy val all = Project("libraries-apache", dir) aggregate (pool, config, math, exec, log4j, logging, sshd, ant)

  lazy val pool = OsgiProject("org.apache.commons.pool") settings
    (libraryDependencies += "commons-pool" % "commons-pool" % "1.5.4")

  lazy val config = OsgiProject("org.apache.commons.configuration", privatePackages = Seq("org.apache.commons.*")) settings
    (libraryDependencies += "commons-configuration" % "commons-configuration" % "1.6")

  lazy val math = OsgiProject("org.apache.commons.math", exports = Seq("org.apache.commons.math3.*")) settings
    (libraryDependencies += "org.apache.commons" % "commons-math3" % "3.0")

  lazy val exec = OsgiProject("org.apache.commons.exec") settings
    (libraryDependencies += "org.apache.commons" % "commons-exec" % "1.1")

  lazy val log4j = OsgiProject("org.apache.log4j") settings
    (libraryDependencies += "log4j" % "log4j" % "1.2.17")

  lazy val logging = OsgiProject("org.apache.commons.logging") settings
    (libraryDependencies += "commons-logging" % "commons-logging" % "1.1.1")

  lazy val sshd = OsgiProject("org.apache.sshd") settings
    (libraryDependencies += "org.apache.sshd" % "sshd-core" % "0.8.0")

  lazy val ant = OsgiProject("org.apache.ant") settings
    (libraryDependencies += "org.apache.ant" % "ant" % "1.8.0")

}