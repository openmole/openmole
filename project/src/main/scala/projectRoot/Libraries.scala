package projectRoot

import com.typesafe.sbt.osgi.{OsgiKeys, SbtOsgi}
import sbt._
import Keys._


/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/17/13
 * Time: 6:50 PM
 * To change this template use File | Settings | File Templates.
 */
trait Libraries extends Defaults {
  lazy val libraries = Project(id = "openmole-libraries",
    base = file("libraries")) aggregate(jetty,scalatra,logback, h2, bonecp, slick, slf4j)

  private implicit val dir = file("libraries")

  lazy val jetty = OsgiProject("org.eclipse.jetty", exports = Seq("org.eclipse.jetty.*", "javax.*")) settings
    (libraryDependencies ++= Seq("org.eclipse.jetty" % "jetty-webapp" % "8.1.8.v20121106",
      "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016")
    )

  lazy val scalatra = OsgiProject("org.scalatra",
    buddyPolicy = Some("global"),
    exports = Seq("org.scalatra.*, org.fusesource.*"),
    privatePackages = Seq("!scala.*","!org.slf4j.*", "*")) settings
    (libraryDependencies ++= Seq("org.scalatra" %% "scalatra" % "2.2.1-SNAPSHOT", "org.scalatra" %% "scalatra-scalate" % "2.2.1-SNAPSHOT")) dependsOn(slf4j)

  lazy val logback = OsgiProject("ch.qos.logback", exports = Seq("ch.qos.logback.*", "org.slf4j.impl")) settings (libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.9")
  /*Project(id = "ch-qos-logback",
    base = file("libraries/ch.qos.logback"),
    settings = OSGIProject("ch.qos.logback") ++ Seq(libraryDependencies ++= Seq("ch.qos.logback" % "logback-classic" % "1.0.9")))*/

  lazy val h2 = OsgiProject("org.h2", buddyPolicy = Some("global"), privatePackages = Seq("META-INF.*")) settings (libraryDependencies += "com.h2database" % "h2" % "1.3.170")

  lazy val bonecp = OsgiProject("com.jolbox.bonecp", buddyPolicy = Some("global")) settings (libraryDependencies += "com.jolbox" % "bonecp" % "0.8.0-rc1")

  lazy val slick = OsgiProject("com.typesafe.slick", exports = Seq("scala.slick.*")) settings (libraryDependencies += "com.typesafe.slick" %% "slick" % "1.0.0")

  lazy val slf4j = OsgiProject("org.slf4j") settings (libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.2")
}
