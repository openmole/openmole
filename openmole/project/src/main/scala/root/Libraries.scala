package root

import sbt._
import Keys._
import com.typesafe.sbt.osgi.OsgiKeys
import OsgiKeys._
import root.libraries._
import org.openmole.buildsystem.OMKeys._
import fr.iscpif.jsmanager.JSManagerPlugin._

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/17/13
 * Time: 6:50 PM
 * To change this template use File | Settings | File Templates.
 */
object Libraries extends Defaults(Apache) {

  val dir = file("libraries")

  val gridscaleVersion = "1.76"

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

  lazy val jetty = "org.openmole" %% "org.eclipse.jetty" % "8.1.8.v20121106"

  lazy val scalatraVersion = "2.3.0"

  lazy val scalatra = "org.openmole" %% "org.scalatra" % scalatraVersion

  lazy val jacksonJson = "org.openmole" %% "org.json4s" % "3.2.9"

  lazy val logback = "org.openmole" %% "ch.qos.logback" % "1.0.9"

  lazy val h2 = "org.openmole" %% "org.h2" % "1.3.170"

  lazy val bonecp = "org.openmole" %% "com.jolbox.bonecp" % "0.8.0-rc1"

  lazy val slick = "org.openmole" %% "com.typesafe.slick" % "2.1.0-RC3"

  lazy val slf4j = "org.openmole" %% "org.slf4j" % "1.7.2"

  lazy val xstream = "org.openmole" %% "com.thoughtworks.xstream" % "1.4.7"

  lazy val groovy = "org.openmole" %% "org.codehaus.groovy" % "2.3.3"

  lazy val scalaLang = "org.openmole" %% "org.scala-lang.scala-library" % "2.11.1"

  //  lazy val scalaCompiler = OsgiProject("org.scala-lang.scala-compiler", exports = Seq("scala.tools.*", "scala.reflect.macros.*"),
  //    privatePackages = Seq("!scala.*", "*"), buddyPolicy = Some("global")) settings (libraryDependencies <<= scalaVersion { s ⇒ Seq("org.scala-lang" % "scala-compiler" % s) })

  lazy val jodaTime = "org.openmole" %% "org.joda.time" % "1.6"

  lazy val gnuCrypto = "org.openmole" %% "org.gnu.crypto" % "2.0.1"

  lazy val jasypt = "org.openmole" %% "org.jasypt.encryption" % "1.8"

  lazy val robustIt = "org.openmole" %% "uk.com.robustit.cloning" % "1.7.4"

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

  lazy val scalaTags = OsgiProject("com.scalatags", exports = Seq("scalatags.*")) settings (
    libraryDependencies += "com.scalatags" %%% "scalatags" % "0.4.0", version := "0.4.0")

  lazy val scalaRx = OsgiProject("com.scalarx", exports = Seq("rx.*")) settings (
    libraryDependencies += "com.scalarx" %%% "scalarx" % "0.2.6", version := "0.2.6")

  lazy val upickle = OsgiProject("upickle", exports = Seq("upickle.*")) settings (
    libraryDependencies += "com.lihaoyi" %%% "upickle" % "0.1.7", version := "0.1.7")

  lazy val scalajsDom = OsgiProject("org.scala-lang.modules.scalajs", exports = Seq("org.scalajs.dom.*")) settings (
    libraryDependencies += "org.scala-lang.modules.scalajs" %%% "scalajs-dom" % "0.6", version := "0.6")

  lazy val autowire = OsgiProject("autowire", exports = Seq("autowire.*")) settings (
    libraryDependencies += "com.lihaoyi" %% "autowire" % "0.1.2", version := "0.1.2")

  lazy val mgo = OsgiProject("fr.iscpif.mgo") settings (
    libraryDependencies += "fr.iscpif" %% "mgo" % "1.76",
    bundleType := Set("plugin"),
    version := "1.76"
  ) dependsOn (monocle)

  val monocleVersion = "0.5.0"

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
