package root

import sbt._
import Keys._
import com.typesafe.sbt.osgi.OsgiKeys
import OsgiKeys._
import root.libraries._
import org.openmole.buildsystem.OMKeys._

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/17/13
 * Time: 6:50 PM
 * To change this template use File | Settings | File Templates.
 */
object Libraries extends Defaults(Apache) {

  val dir = file("libraries")

  val gridscaleVersion = "1.75"

  val bouncyCastleVersion = "1.50"

  lazy val gridscale = "fr.iscpif.gridscale.bundle" %% "gridscale" % gridscaleVersion

  lazy val gridscaleSSH = Seq(
    "fr.iscpif.gridscale.bundle" %% "ssh" % gridscaleVersion,
    bouncyCastle
  )

  lazy val gridscalePBS = "fr.iscpif.gridscale.bundle" %% "pbs" % gridscaleVersion

  lazy val gridscaleSGE = "fr.iscpif.gridscale.bundle" %% "sge" % gridscaleVersion

  lazy val gridscaleCondor = "fr.iscpif.gridscale.bundle" %% "condor" % gridscaleVersion

  lazy val gridscaleSLURM = "fr.iscpif.gridscale.bundle" %% "slurm" % gridscaleVersion

  lazy val gridscaleGlite = "fr.iscpif.gridscale.bundle" %% "glite" % gridscaleVersion

  lazy val gridscaleDirac = "fr.iscpif.gridscale.bundle" %% "dirac" % gridscaleVersion

  lazy val gridscaleHTTP = "fr.iscpif.gridscale.bundle" %% "http" % gridscaleVersion

  lazy val gridscaleOAR = "fr.iscpif.gridscale.bundle" %% "oar" % gridscaleVersion

  lazy val bouncyCastle = "org.bouncycastle" % "bcpkix-jdk15on" % bouncyCastleVersion

  lazy val includeOsgi = libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV }

  lazy val jetty = OsgiProject(
    "org.eclipse.jetty",
    exports = Seq("org.eclipse.jetty.*", "javax.*")) settings (
      libraryDependencies ++= Seq("org.eclipse.jetty" % "jetty-webapp" % "8.1.8.v20121106", "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016")
    )

  lazy val scalatraVersion = "2.3.0.RC3"

  lazy val scalatra = OsgiProject("org.scalatra",
    buddyPolicy = Some("global"),
    exports = Seq("org.scalatra.*, org.fusesource.*"),
    privatePackages = Seq("!scala.*", "!org.slf4j.*", "!org.json4s", "*")) settings
    (libraryDependencies ++= Seq("org.scalatra" %% "scalatra" % scalatraVersion,
      "org.scalatra" %% "scalatra-scalate" % scalatraVersion,
      "org.scalatra" %% "scalatra-json" % scalatraVersion), version := scalatraVersion) dependsOn (slf4j)

  lazy val jacksonJson = OsgiProject("org.json4s") settings (
    libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.9",
    exportPackage += "com.fasterxml.*",
    version := "3.2.9"
  )

  lazy val logback = OsgiProject("ch.qos.logback", exports = Seq("ch.qos.logback.*", "org.slf4j.impl")) settings
    (libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.9", version := "1.0.9")

  lazy val h2 = OsgiProject("org.h2", buddyPolicy = Some("global"), privatePackages = Seq("META-INF.*")) settings
    (libraryDependencies += "com.h2database" % "h2" % "1.3.170", version := "1.3.170")

  lazy val bonecp = OsgiProject("com.jolbox.bonecp", buddyPolicy = Some("global")) settings
    (libraryDependencies += "com.jolbox" % "bonecp" % "0.8.0-rc1", version := "0.8.0-rc1")

  lazy val slick = OsgiProject("com.typesafe.slick", exports = Seq("scala.slick.*")) settings
    (libraryDependencies += "com.typesafe.slick" %% "slick" % "2.1.0-M2", version := "2.1.0-M2")

  lazy val slf4j = OsgiProject("org.slf4j") settings (
    libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.2",
    version := "1.7.2"
  )

  lazy val xstream = OsgiProject(
    "com.thoughtworks.xstream",
    buddyPolicy = Some("global"),
    privatePackages = Seq("!scala.*", "*")) settings (
      libraryDependencies ++= Seq("com.thoughtworks.xstream" % "xstream" % "1.4.7", "net.sf.kxml" % "kxml2" % "2.3.0"),
      version := "1.4.7",
      bundleType += "dbserver")

  lazy val groovy = OsgiProject(
    "org.codehaus.groovy",
    buddyPolicy = Some("global"),
    exports = Seq("groovy.*", "org.codehaus.*"),
    privatePackages = Seq("!scala.*,*")) settings (
      libraryDependencies ++= Seq("org.codehaus.groovy" % "groovy-all" % "2.3.3", "org.fusesource.jansi" % "jansi" % "1.2.1"),
      version := "2.3.3"
    )

  lazy val scalaLang = OsgiProject("org.scala-lang.scala-library", exports = Seq("akka.*", "com.typesafe.*", "scala.*", "scalax.*", "jline.*"),
    privatePackages = Seq("*"), buddyPolicy = Some("global")
  ) settings
    (libraryDependencies <++= (scalaVersion) { sV ⇒
      Seq("org.scala-lang" % "scala-library" % sV,
        "org.scala-lang" % "scala-reflect" % sV,
        "jline" % "jline" % "2.11",
        "com.typesafe.akka" %% "akka-actor" % "2.3.3",
        "com.typesafe.akka" %% "akka-transactor" % "2.3.3",
        "com.typesafe" % "config" % "1.2.1",
        "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.3",
        "org.scala-lang" % "scala-compiler" % sV
      )
    }, bundleType += "dbserver", version := "2.11.1")

  //  lazy val scalaCompiler = OsgiProject("org.scala-lang.scala-compiler", exports = Seq("scala.tools.*", "scala.reflect.macros.*"),
  //    privatePackages = Seq("!scala.*", "*"), buddyPolicy = Some("global")) settings (libraryDependencies <<= scalaVersion { s ⇒ Seq("org.scala-lang" % "scala-compiler" % s) })

  lazy val jodaTime = OsgiProject("org.joda.time") settings (
    libraryDependencies += "joda-time" % "joda-time" % "1.6",
    version := "1.6"
  )

  lazy val gnuCrypto = OsgiProject("org.gnu.crypto") settings (
    libraryDependencies += "org.gnu.crypto" % "gnu-crypto" % "2.0.1",
    exportPackage += "gnu.crypto.*",
    version := "2.0.1"
  )

  lazy val jasypt = OsgiProject("org.jasypt.encryption", exports = Seq("org.jasypt.*")) settings (
    libraryDependencies += "org.jasypt" % "jasypt" % "1.8",
    version := "1.8"
  )

  lazy val robustIt = OsgiProject("uk.com.robustit.cloning", exports = Seq("com.rits.*"), privatePackages = Seq("org.objenesis.*")) settings (
    libraryDependencies += "uk.com.robust-it" % "cloning" % "1.7.4",
    libraryDependencies += "org.objenesis" % "objenesis" % "1.2",
    version := "1.7.4")

  lazy val netlogo4_noscala = OsgiProject("ccl.northwestern.edu.netlogo4.noscala", exports = Seq("org.nlogo.*"),
    privatePackages = Seq("!scala.*", "*")) settings
    (libraryDependencies ++=
      Seq("ccl.northwestern.edu" % "netlogo" % "4.1.3",
        "org.picocontainer" % "picocontainer" % "2.8",
        "org.objectweb" % "asm" % "3.1",
        "org.objectweb" % "asm-commons" % "3.1"), version := "4.1.3", autoScalaLibrary := false, bundleType := Set("all"), scalaVersion := "2.8.0", crossPaths := false,
        ivyScala ~= { (is: Option[IvyScala]) ⇒ //should disable the binary compat warnings this causes
          for (i ← is) yield i.copy(checkExplicit = false)
        })

  lazy val netlogo5_noscala = OsgiProject("ccl.northwestern.edu.netlogo5.noscala", exports = Seq("org.nlogo.*"),
    privatePackages = Seq("!scala.*", "*")) settings
    (libraryDependencies ++=
      Seq("ccl.northwestern.edu" % "netlogo" % "5.0.5",
        "org.picocontainer" % "picocontainer" % "2.13.6",
        "org.objectweb" % "asm-all" % "3.3.1"), version := "5.0.5", autoScalaLibrary := false, bundleType := Set("all"), scalaVersion := "2.9.2", crossPaths := false,
        ivyScala ~= { (is: Option[IvyScala]) ⇒ //See netlogo4_noscala
          for (i ← is) yield i.copy(checkExplicit = false)
        })

  lazy val netlogo4 = OsgiProject("ccl.northwestern.edu.netlogo4", exports = Seq("org.nlogo.*"),
    privatePackages = Seq("*")) settings
    (libraryDependencies ++=
      Seq("ccl.northwestern.edu" % "netlogo" % "4.1.3",
        "org.picocontainer" % "picocontainer" % "2.8",
        "org.objectweb" % "asm" % "3.1",
        "org.objectweb" % "asm-commons" % "3.1"), version := "4.1.3", scalaVersion := "2.8.0", bundleType := Set("plugin"))

  lazy val netlogo5 = OsgiProject("ccl.northwestern.edu.netlogo5", exports = Seq("org.nlogo.*"),
    privatePackages = Seq("*")) settings
    (libraryDependencies ++=
      Seq("ccl.northwestern.edu" % "netlogo" % "5.0.5",
        "org.scala-lang" % "scala-library" % "2.9.2",
        "org.objectweb" % "asm-all" % "3.3.1",
        "org.picocontainer" % "picocontainer" % "2.13.6"), version := "5.0.5", scalaVersion := "2.9.2", bundleType := Set("plugin"))

  lazy val guava = OsgiProject("com.google.guava",
    exports = Seq("com.google.common.*"), privatePackages = Seq("!scala.*", "*")) settings (libraryDependencies ++=
      Seq("com.google.guava" % "guava" % "16.0.1", "com.google.code.findbugs" % "jsr305" % "1.3.9"),
      version := "16.0.1"
    )

  lazy val jsyntaxpane = OsgiProject("jsyntaxpane", privatePackages = Seq("!scala.*", "*")) settings
    (libraryDependencies += "jsyntaxpane" % "jsyntaxpane" % "0.9.6", version := "0.9.6")

  lazy val gral = OsgiProject("de.erichseifert.gral", privatePackages = Seq("!scala.*", "*")) settings
    (libraryDependencies += "de.erichseifert.gral" % "gral-core" % "0.9-SNAPSHOT", version := "0.9")

  lazy val miglayout = OsgiProject("net.miginfocom.swing.miglayout", exports = Seq("net.miginfocom.*")) settings
    (libraryDependencies += "com.miglayout" % "miglayout" % "3.7.4", version := "3.7.4")

  lazy val netbeans = OsgiProject("org.netbeans.api", exports = Seq("org.netbeans.*", "org.openide.*")) settings
    (libraryDependencies ++= Seq("org.netbeans.api" % "org-netbeans-api-visual" % "RELEASE73",
      "org.netbeans.api" % "org-netbeans-modules-settings" % "RELEASE73"))

  lazy val mgo = OsgiProject("fr.iscpif.mgo") settings (
    libraryDependencies += "fr.iscpif" %% "mgo" % "1.75-SNAPSHOT",
    bundleType := Set("plugin"),
    version := "1.75"
  ) dependsOn (monocle)

  val monocleVersion = "0.4.0"

  lazy val monocle = OsgiProject("monocle") settings (
    libraryDependencies += "com.github.julien-truffaut" %% "monocle-core" % monocleVersion,
    libraryDependencies += "com.github.julien-truffaut" %% "monocle-generic" % monocleVersion,
    libraryDependencies += "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion,
    bundleType := Set("plugin"),
    version := monocleVersion
  )

  lazy val opencsv = OsgiProject("au.com.bytecode.opencsv") settings (
    libraryDependencies += "net.sf.opencsv" % "opencsv" % "2.0",
    version := "2.0",
    bundleType := Set("plugin")
  )

  lazy val jline = OsgiProject("net.sourceforge.jline") settings (
    libraryDependencies += "jline" % "jline" % "0.9.94",
    version := "0.9.94",
    exportPackage := Seq("jline.*"))

  lazy val arm = OsgiProject("com.jsuereth.scala-arm") settings (
    libraryDependencies += "com.jsuereth" %% "scala-arm" % "1.4",
    version := "1.4",
    exportPackage := Seq("resource.*"))

  lazy val scalajHttp = OsgiProject("org.scalaj.scalaj-http") settings (
    libraryDependencies += "org.scalaj" %% "scalaj-http" % "0.3.15",
    version := "0.3.15",
    exportPackage := Seq("scalaj.http.*")
  )

  lazy val scalaz = OsgiProject("org.scalaz", exports = Seq("scalaz.*")) settings
    (libraryDependencies += "org.scalaz" %% "scalaz-core" % "7.0.6", version := "7.0.6")

  lazy val scopt = OsgiProject("com.github.scopt", exports = Seq("scopt.*")) settings (
    libraryDependencies += "com.github.scopt" %% "scopt" % "3.2.0",
    version := "3.2.0"
  )

  override def OsgiSettings = super.OsgiSettings ++ Seq(bundleType := Set("core")) //TODO make library defaults
}
