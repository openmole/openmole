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

  val gridscaleVersion = "1.79-SNAPSHOT"

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

  lazy val includeOsgi = libraryDependencies += "org.eclipse.core" % "org.eclipse.osgi" % osgiVersion.value

  lazy val includeOsgiProv = libraryDependencies += "org.eclipse.core" % "org.eclipse.osgi" % osgiVersion.value % "provided"

  lazy val jetty = "org.openmole" %% "org-eclipse-jetty" % "8.1.8.v20121106"

  lazy val scalatraVersion = "2.3.0"

  lazy val scalatra = "org.openmole" %% "org-scalatra" % scalatraVersion

  lazy val jacksonJson = "org.openmole" %% "org-json4s" % "3.2.9"

  lazy val logback = "org.openmole" %% "ch-qos-logback" % "1.0.9"

  lazy val h2 = "org.openmole" %% "org-h2" % h2Version

  lazy val bonecp = "org.openmole" %% "com-jolbox-bonecp" % "0.8.0-rc1"

  lazy val slick = "org.openmole" %% "com-typesafe-slick" % slickVersion

  lazy val slf4j = "org.openmole" %% "org-slf4j" % "1.7.2"

  lazy val xstream = "org.openmole" %% "com-thoughtworks-xstream" % "1.4.7"

  lazy val groovy = "org.openmole" %% "org-codehaus-groovy" % "2.3.3"

  lazy val scalaLang = "org.openmole" %% "org-scala-lang-scala-library" % "2.11.1"

  //  lazy val scalaCompiler = OsgiProject("org.scala-lang.scala-compiler", exports = Seq("scala.tools.*", "scala.reflect.macros.*"),
  //    privatePackages = Seq("!scala.*", "*"), buddyPolicy = Some("global")) settings (libraryDependencies <<= scalaVersion { s ⇒ Seq("org.scala-lang" % "scala-compiler" % s) })

  lazy val jodaTime = "org.openmole" %% "org-joda-time" % "1.6"

  lazy val jasypt = "org.openmole" %% "org-jasypt-encryption" % "1.8"

  lazy val robustIt = "org.openmole" %% "uk-com-robustit-cloning" % "1.7.4"

  lazy val netlogo4_noscala = "org.openmole" % "ccl-northwestern-edu-netlogo4-noscala" % "4.1.3"

  lazy val netlogo4 = "org.openmole" %% "ccl-northwestern-edu-netlogo4" % "4.1.3"

  lazy val netlogo5 = "org.openmole" %% "ccl-northwestern-edu-netlogo5" % netLogo5Version

  lazy val guava = "org.openmole" %% "com-google-guava" % "16.0.1"

  lazy val scalaTags = "org.openmole" %% "com-scalatags" % "0.4.0"

  lazy val scalaRx = "org.openmole" %% "com-scalarx" % "0.2.6"

  lazy val upickle = "org.openmole" %% "upickle" % "0.2.1"

  lazy val scalajsDom = "org.openmole" %% "org-scala-lang-modules-scalajs" % "0.6"

  lazy val autowire = "org.openmole" %% "autowire" % "0.1.4"

  lazy val mgo = "org.openmole" %% "fr-iscpif-mgo" % mgoVersion

  lazy val monocle = "org.openmole" %% "monocle" % "0.5.0"

  lazy val opencsv = "org.openmole" %% "au-com-bytecode-opencsv" % "2.0"

  lazy val jline = "org.openmole" %% "net-sourceforge-jline" % "0.9.94"

  lazy val arm = "org.openmole" %% "com-jsuereth-scala-arm" % "1.4"

  lazy val scalajHttp = "org.openmole" %% "org-scalaj-scalaj-http" % "0.3.15"

  lazy val scalaz = "org.openmole" %% "org-scalaz" % scalazVersion

  lazy val scopt = "org.openmole" %% "com-github-scopt" % "3.2.0"

  lazy val scalabc = "org.openmole" %% "scalabc" % scalabcVersion

  override def OsgiSettings = super.OsgiSettings ++ Seq(bundleType := Set("core")) //TODO make library defaults
}
