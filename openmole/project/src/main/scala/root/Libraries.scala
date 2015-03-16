package root

import sbt._
import Keys._
import com.typesafe.sbt.osgi.OsgiKeys
import OsgiKeys._
import org.openmole.buildsystem.OMKeys._
import org.scalajs.sbtplugin.ScalaJSPlugin
import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/17/13
 * Time: 6:50 PM
 * To change this template use File | Settings | File Templates.
 */
object Libraries extends Defaults {

  val dir = file("libraries")

  val gridscaleVersion = "1.84"

  val bouncyCastleVersion = "1.50"

  lazy val gridscale = "fr.iscpif.gridscale.bundle" %% "gridscale" % gridscaleVersion

  lazy val gridscaleSSH = "fr.iscpif.gridscale.bundle" %% "ssh" % gridscaleVersion

  lazy val gridscalePBS = "fr.iscpif.gridscale.bundle" %% "pbs" % gridscaleVersion

  lazy val gridscaleSGE = "fr.iscpif.gridscale.bundle" %% "sge" % gridscaleVersion

  lazy val gridscaleCondor = "fr.iscpif.gridscale.bundle" %% "condor" % gridscaleVersion

  lazy val gridscaleSLURM = "fr.iscpif.gridscale.bundle" %% "slurm" % gridscaleVersion

  lazy val gridscaleGlite = "fr.iscpif.gridscale.bundle" %% "glite" % gridscaleVersion

  lazy val gridscaleDirac = "fr.iscpif.gridscale.bundle" %% "dirac" % gridscaleVersion

  lazy val gridscaleHTTP = "fr.iscpif.gridscale.bundle" %% "http" % gridscaleVersion

  lazy val gridscaleOAR = "fr.iscpif.gridscale.bundle" %% "oar" % gridscaleVersion

  lazy val bouncyCastle = "org.bouncycastle" % "bcpkix-jdk15on" % bouncyCastleVersion

  lazy val includeOsgi = libraryDependencies += "org.eclipse.core" % "org.eclipse.osgi" % "3.8.2.v20130124-134944"

  lazy val jetty = "org.openmole" %% "org-eclipse-jetty" % "8.1.8.v20121106"

  lazy val scalatraVersion = "2.3.0"

  lazy val scalatra = "org.openmole" %% "org-scalatra" % scalatraVersion

  lazy val scalate = "org.openmole" %% "scalate" % scalatraVersion

  lazy val jacksonJson = "org.openmole" %% "org-json4s" % "3.2.9"

  lazy val logback = "org.openmole" %% "ch-qos-logback" % "1.0.9"

  lazy val h2 = "org.openmole" %% "org-h2" % "1.3.176"

  lazy val bonecp = "org.openmole" %% "com-jolbox-bonecp" % "0.8.0-rc1"

  lazy val slick = "org.openmole" %% "com-typesafe-slick" % "2.1.0"

  lazy val slf4j = "org.openmole" %% "org-slf4j" % "1.7.10"

  lazy val xstream = "org.openmole" %% "com-thoughtworks-xstream" % "1.4.8"

  lazy val groovy = "org.openmole" %% "org-codehaus-groovy" % "2.4.1"

  lazy val scalaLang = "org.openmole" %% "org-scala-lang-scala-library" % "2.11.5"

  lazy val jodaTime = "org.openmole" %% "org-joda-time" % "1.6"

  lazy val jasypt = "org.openmole" %% "org-jasypt-encryption" % "1.9.2"

  lazy val netlogo4_noscala = "org.openmole" % "ccl-northwestern-edu-netlogo4-noscala" % "4.1.3"

  lazy val netlogo4 = "org.openmole" % "ccl-northwestern-edu-netlogo4" % "4.1.3"

  lazy val netLogo5Version = "5.1.0"
  lazy val netlogo5 = "org.openmole" % "ccl-northwestern-edu-netlogo5" % netLogo5Version
  lazy val netlogo5_noscala = "org.openmole" % "ccl-northwestern-edu-netlogo5-noscala" % netLogo5Version

  lazy val guava = "org.openmole" %% "com-google-guava" % "18.0"

  lazy val jawn = "org.openmole" %% "jawn" % "0.6.0"

  lazy val scalaTagsVersion = "0.4.6"
  lazy val scalaJSDomVersion = "0.8.0"
  lazy val scalaJQueryVersion = "0.8.0"
  lazy val rxVersion = "0.2.8"
  lazy val scalaUpickleVersion = "0.2.7"
  lazy val scalaAutowireVersion = "0.2.5"

  lazy val upickle = "org.openmole" %% "upickle" % scalaUpickleVersion

  lazy val autowire = "org.openmole" %% "autowire" % scalaAutowireVersion

  lazy val scalaTags = "org.openmole" %% "com-scalatags" % scalaTagsVersion

  lazy val rx = "org.openmole" %% "rx" % rxVersion

  lazy val scalajsVersion = "0.6.1"
  lazy val scalajsTools = "org.openmole" %% "scalajs-tools" % scalajsVersion
  lazy val scalajsLibrary = "org.openmole" %% "scalajs-library" % scalajsVersion
  lazy val scalajsDom = "org.openmole" %% "scalajs-dom" % scalaJSDomVersion
  lazy val scalajsJQuery = "org.openmole" %% "scalajs-jquery" % scalaJQueryVersion

  lazy val mgo = "org.openmole" %% "fr-iscpif-mgo" % "1.79"

  lazy val family = "org.openmole" %% "fr-iscpif-family" % "1.0"

  lazy val monocle = "org.openmole" %% "monocle" % "1.0.1"

  lazy val scaladget = "org.openmole" %% "scaladget" % "0.3.0"

  lazy val opencsv = "org.openmole" %% "au-com-bytecode-opencsv" % "2.3"

  lazy val arm = "org.openmole" %% "com-jsuereth-scala-arm" % "1.4"

  lazy val scalajHttp = "org.openmole" %% "org-scalaj-scalaj-http" % "0.3.15"

  lazy val scopt = "org.openmole" %% "com-github-scopt" % "3.2.0"

  lazy val scalabc = "org.openmole" %% "fr-iscpif-scalabc" % "0.4"

  lazy val equinoxApp = "org.eclipse.core" % "org.eclipse.equinox.app" % "1.3.100.v20120522-1841"

  lazy val equinoxCommon = "org.eclipse.core" % "org.eclipse.equinox.common" % "3.6.100.v20120522-1841"

  lazy val equinoxLauncher = "org.eclipse.core" % "org.eclipse.equinox.launcher" % "1.3.0.v20120522-1813"

  lazy val equinoxRegistry = "org.eclipse.core" % "org.eclipse.equinox.registry" % "3.5.200.v20120522-1841"

  lazy val equinoxPreferences = "org.eclipse.core" % "org.eclipse.equinox.preferences" % "3.5.1.v20121031-182809"

  lazy val equinoxContenttype = "org.eclipse.core" % "org.eclipse.core.contenttype" % "3.4.200.v20120523-2004"

  lazy val equinoxJobs = "org.eclipse.core" % "org.eclipse.core.jobs" % "3.5.300.v20120912-155018"

  lazy val equinoxRuntime = "org.eclipse.core" % "org.eclipse.core.runtime" % "3.8.0.v20120912-155025"

  lazy val equinoxOSGi = "org.eclipse.core" % "org.eclipse.osgi" % "3.8.2.v20130124-134944"

  lazy val scalatest = "org.scalatest" %% "scalatest" % "2.2.4" % "test"

  lazy val scalatexSite = "org.openmole" %% "com-lihaoyi-scalatex-site" % "0.1.1"

  lazy val apacheConfig = "org.openmole" %% "org-apache-commons-configuration" % "1.10"

  lazy val math = "org.openmole" %% "org-apache-commons-math" % "3.4.1"

  lazy val exec = "org.openmole" %% "org-apache-commons-exec" % "1.1"

  lazy val log4j = "org.openmole" %% "org-apache-log4j" % "1.2.17"

  lazy val logging = "org.openmole" %% "org-apache-commons-logging" % "1.1.1"

  lazy val sshd = "org.openmole" %% "org-apache-sshd" % "0.13.0"

  lazy val ant = "org.openmole" %% "org-apache-ant" % "1.8.0"

  lazy val codec = "org.openmole" %% "org-apache-commons-codec" % "1.10"

}
