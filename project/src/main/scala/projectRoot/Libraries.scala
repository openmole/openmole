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
    base = file("libraries")) aggregate(jetty,scalatra,logback, h2, bonecp, slick)

  private implicit val dir = file("libraries")

  lazy val jetty = Project(id = "org-eclipse-jetty",
    base = file(dir+"/org.eclipse.jetty"),
    settings = OSGIProject("org.eclipse.jetty", exports = Seq("org.eclipse.jetty.*", "javax.*")))

  lazy val scalatra = Project(id = "org-scalatra",
    base = file("libraries/org.scalatra"),
    settings = OSGIProject("org.scalatra", buddyPolicy = Some("global"), exports = Seq("org.scalatra.*")) ++
      Seq(libraryDependencies ++= Seq("org.scalatra" %% "scalatra" % "2.2.0",
                                      "org.scalatra" %% "scalatra-scalate" % "2.2.0"),
          OsgiKeys.privatePackage := Seq("*")))

  lazy val logback = Project(id = "ch-qos-logback",
    base = file("libraries/ch.qos.logback"),
    settings = OSGIProject("ch.qos.logback") ++ Seq(libraryDependencies ++= Seq("ch.qos.logback" % "logback-classic" % "1.0.9")))

  lazy val h2 = OsgiProject("org.h2") settings (libraryDependencies += "com.h2database" % "h2" % "1.3.170")

  lazy val bonecp = OsgiProject("com.jolbox.bonecp") settings (libraryDependencies += "com.jolbox" % "bonecp" % "0.8.0-rc1")

  lazy val slick = Project(id = "com-typesafe-slick", base = file("libraries/com.typesafe.slick"),
    settings = OSGIProject("com.typesafe.slick") ++ Seq(libraryDependencies ++= Seq("com.typesafe.slick" %% "slick" % "1.0.0")))
}
