package root

import sbt._
import Keys._
import com.typesafe.sbt.osgi.OsgiKeys
import OsgiKeys._
import root.libraries._

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/17/13
 * Time: 6:50 PM
 * To change this template use File | Settings | File Templates.
 */
object Libraries extends Defaults {
  val dir = file("libraries")

  lazy val all = Project(id = "openmole-libraries",
    base = file("libraries")) aggregate (jetty, scalatra, logback, h2, bonecp, slick, slf4j, xstream, icu4j, groovy,
      objenesis, scalaLang, Apache.all, jodaTime, gnuCrypto, db4o, jasypt, robustIt, netlogo4, netlogo5, opencsv,
      netlogo4_noscala, netlogo5_noscala, gridscale, guava, jsyntaxpane, gral, miglayout, netbeans, mgo, jline, jacksonJson)

  lazy val jetty = OsgiProject("org.eclipse.jetty", exports = Seq("org.eclipse.jetty.*", "javax.*")) settings
    (libraryDependencies ++= Seq("org.eclipse.jetty" % "jetty-webapp" % "8.1.8.v20121106",
      "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016"))

  lazy val scalatra = OsgiProject("org.scalatra",
    buddyPolicy = Some("global"),
    exports = Seq("org.scalatra.*, org.fusesource.*"),
    privatePackages = Seq("!scala.*", "!org.slf4j.*", "!org.json4s", "*")) settings
    (libraryDependencies ++= Seq("org.scalatra" %% "scalatra" % "2.2.1",
      "org.scalatra" %% "scalatra-scalate" % "2.2.1",
      "org.scalatra" %% "scalatra-json" % "2.2.1")) dependsOn (slf4j)

  lazy val jacksonJson = OsgiProject("org.json4s") settings (
    libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.4",
    exportPackage += "com.fasterxml.*"
  )

  lazy val logback = OsgiProject("ch.qos.logback", exports = Seq("ch.qos.logback.*", "org.slf4j.impl")) settings
    (libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.9")

  lazy val h2 = OsgiProject("org.h2", buddyPolicy = Some("global"), privatePackages = Seq("META-INF.*")) settings
    (libraryDependencies += "com.h2database" % "h2" % "1.3.170")

  lazy val bonecp = OsgiProject("com.jolbox.bonecp", buddyPolicy = Some("global")) settings
    (libraryDependencies += "com.jolbox" % "bonecp" % "0.8.0-rc1")

  lazy val slick = OsgiProject("com.typesafe.slick", exports = Seq("scala.slick.*")) settings
    (libraryDependencies += "com.typesafe.slick" %% "slick" % "1.0.0")

  lazy val slf4j = OsgiProject("org.slf4j") settings (libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.2")

  lazy val xstream = OsgiProject("com.thoughtworks.xstream", buddyPolicy = Some("global"), privatePackages = Seq("!scala.*", "*")) settings
    (libraryDependencies ++= Seq("com.thoughtworks.xstream" % "xstream" % "1.4.1",
      "net.sf.kxml" % "kxml2" % "2.3.0"))

  lazy val icu4j = OsgiProject("com.ibm.icu") settings (libraryDependencies += "com.ibm.icu" % "icu4j" % "4.0.1")

  lazy val groovy = OsgiProject("org.codehaus.groovy", buddyPolicy = Some("global"), exports = Seq("groovy.*", "org.codehaus.*"),
    privatePackages = Seq("!scala.*,*")) settings (libraryDependencies ++= Seq("org.codehaus.groovy" % "groovy-all" % "2.0.5",
      "org.fusesource.jansi" % "jansi" % "1.2.1"))

  lazy val objenesis = OsgiProject("org.objenesis") settings (libraryDependencies += "org.objenesis" % "objenesis" % "1.2")

  lazy val scalaLang = OsgiProject("org.scala-lang.scala-library", exports = Seq("akka.*", "com.typesafe.*", "scala.*"),
    privatePackages = Seq("*"), buddyPolicy = Some("global")
  ) settings
    (libraryDependencies <++= (scalaVersion) { sV ⇒
      Seq("org.scala-lang" % "scala-library" % sV,
        "org.scala-lang" % "scala-reflect" % sV,
        "org.scala-lang" % "scala-actors" % sV,
        "org.scala-lang" % "scala-compiler" % sV,
        "org.scala-lang" % "jline" % sV,
        "com.typesafe.akka" %% "akka-actor" % "2.1.1",
        "com.typesafe.akka" %% "akka-transactor" % "2.1.1",
        "com.typesafe" % "config" % "1.0.0")
    })

  lazy val jodaTime = OsgiProject("org.joda.time") settings (libraryDependencies += "joda-time" % "joda-time" % "1.6")

  lazy val gnuCrypto = OsgiProject("org.gnu.crypto") settings (
    libraryDependencies += "org.gnu.crypto" % "gnu-crypto" % "2.0.1",
    exportPackage += "gnu.crypto.*"
  )

  lazy val jasypt = OsgiProject("org.jasypt.encryption", exports = Seq("org.jasypt.*")) settings (libraryDependencies += "org.jasypt" % "jasypt" % "1.8")

  lazy val db4o = OsgiProject("com.db4o", buddyPolicy = Some("global")) settings
    (libraryDependencies += "com.db4o" % "db4o-full-java5" % "8.1-SNAPSHOT")

  lazy val robustIt = OsgiProject("uk.com.robustit.cloning", exports = Seq("com.rits.*")) settings
    (libraryDependencies += "uk.com.robust-it" % "cloning" % "1.7.4")

  lazy val netlogo4_noscala = OsgiProject("ccl.northwestern.edu.netlogo4.noscala", exports = Seq("org.nlogo.*"),
    privatePackages = Seq("!scala.*", "*")) settings
    (libraryDependencies ++=
      Seq("ccl.northwestern.edu" % "netlogo" % "4.1.3",
        "org.picocontainer" % "picocontainer" % "2.8",
        "org.objectweb" % "asm" % "3.1",
        "org.objectweb" % "asm-commons" % "3.1"), version := "4.1.3", scalaVersion := "2.8.0")

  lazy val netlogo5_noscala = OsgiProject("ccl.northwestern.edu.netlogo5.noscala", exports = Seq("org.nlogo.*"),
    privatePackages = Seq("!scala.*", "*")) settings
    (libraryDependencies ++=
      Seq("ccl.northwestern.edu" % "netlogo" % "5.0.3",
        "org.picocontainer" % "picocontainer" % "2.8",
        "org.objectweb" % "asm-all" % "3.3.1",
        "org.scala-lang" % "scala-library" % "2.9.2"), version := "5.0.3", scalaVersion := "2.9.2")

  lazy val netlogo4 = OsgiProject("ccl.northwestern.edu.netlogo4", exports = Seq("org.nlogo.*"),
    privatePackages = Seq("*")) settings
    (libraryDependencies ++=
      Seq("ccl.northwestern.edu" % "netlogo" % "4.1.3",
        "org.picocontainer" % "picocontainer" % "2.8",
        "org.objectweb" % "asm" % "3.1",
        "org.objectweb" % "asm-commons" % "3.1"), version := "4.1.3", scalaVersion := "2.8.0")

  lazy val netlogo5 = OsgiProject("ccl.northwestern.edu.netlogo5", exports = Seq("org.nlogo.*"),
    privatePackages = Seq("*")) settings
    (libraryDependencies ++=
      Seq("ccl.northwestern.edu" % "netlogo" % "5.0.3",
        "org.scala-lang" % "scala-library" % "2.9.2",
        "org.objectweb" % "asm-all" % "3.3.1",
        "org.picocontainer" % "picocontainer" % "2.8"), version := "5.0.3", scalaVersion := "2.9.2")

  lazy val guava = OsgiProject("com.google.guava",
    exports = Seq("com.google.common.*"), privatePackages = Seq("*")) settings (libraryDependencies ++=
      Seq("com.google.guava" % "guava" % "13.0.1", "com.google.code.findbugs" % "jsr305" % "1.3.9")
    )

  lazy val gridscale = OsgiProject("fr.iscpif.gridscale",
    exports = Seq("fr.iscpif.gridscale.*", "net.schmizz.sshj.*", "org.glite.*", "org.globus.*", "org.ogf.*"),
    privatePackages = Seq("!scala.*", "!org.slf4j.*", "*")) settings
    (libraryDependencies += "fr.iscpif.gridscale" % "gridscale" % "1.45-SNAPSHOT",
      OsgiKeys.embeddedJars <<= Keys.externalDependencyClasspath in Compile map { deps ⇒
        deps filter (d ⇒ d.data.getName startsWith "bcprov") map (d ⇒ d.data)
      } //TODO make this easier to do using updateReport and filters.
    ) dependsOn (slf4j)

  lazy val jsyntaxpane = OsgiProject("jsyntaxpane", privatePackages = Seq("!scala.*", "*")) settings
    (libraryDependencies += "jsyntaxpane" % "jsyntaxpane" % "0.9.6")

  lazy val gral = OsgiProject("de.erichseifert.gral", privatePackages = Seq("!scala.*", "*")) settings
    (libraryDependencies += "de.erichseifert.gral" % "gral-core" % "0.9-SNAPSHOT")

  lazy val miglayout = OsgiProject("net.miginfocom.swing.miglayout", exports = Seq("net.miginfocom.*")) settings
    (libraryDependencies += "com.miglayout" % "miglayout" % "3.7.4")

  lazy val netbeans = OsgiProject("org.netbeans.api", exports = Seq("org.netbeans.*", "org.openide.*")) settings
    (libraryDependencies ++= Seq("org.netbeans.api" % "org-netbeans-api-visual" % "RELEASE73",
      "org.netbeans.api" % "org-netbeans-modules-settings" % "RELEASE73"))

  lazy val mgo = OsgiProject("fr.iscpif.mgo") settings (libraryDependencies += "fr.iscpif" %% "mgo" % "1.59")

  lazy val opencsv = OsgiProject("au.com.bytecode.opencsv") settings (libraryDependencies += "net.sf.opencsv" % "opencsv" % "2.0")

  lazy val jline = OsgiProject("net.sourceforge.jline") settings (libraryDependencies += "jline" % "jline" % "0.9.94", exportPackage := Seq("jline.*"))
}
