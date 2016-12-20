import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

object Libraries {

  val gridscaleVersion = "1.88-SNAPSHOT"

  val bouncyCastleVersion = "1.50"

  val jqueryVersion = "2.2.1"

  val aceVersion = "01.08.2014"

  val d3Version = "3.5.12"

  val tooltipserVersion = "3.3.0"

  lazy val gridscale = "fr.iscpif.gridscale.bundle" %% "gridscale" % gridscaleVersion

  lazy val gridscaleSSH = "fr.iscpif.gridscale.bundle" %% "ssh" % gridscaleVersion

  lazy val gridscalePBS = "fr.iscpif.gridscale.bundle" %% "pbs" % gridscaleVersion

  lazy val gridscaleSGE = "fr.iscpif.gridscale.bundle" %% "sge" % gridscaleVersion

  lazy val gridscaleCondor = "fr.iscpif.gridscale.bundle" %% "condor" % gridscaleVersion

  lazy val gridscaleSLURM = "fr.iscpif.gridscale.bundle" %% "slurm" % gridscaleVersion

  lazy val gridscaleGlite = "fr.iscpif.gridscale.bundle" %% "egi" % gridscaleVersion

  lazy val gridscaleHTTP = "fr.iscpif.gridscale.bundle" %% "http" % gridscaleVersion

  lazy val gridscaleOAR = "fr.iscpif.gridscale.bundle" %% "oar" % gridscaleVersion

  lazy val bouncyCastle = "org.bouncycastle" % "bcpkix-jdk15on" % bouncyCastleVersion

  lazy val scalatra = "org.openmole" %% "org-scalatra" % "2.4.0"

  lazy val logback = "org.openmole" %% "ch-qos-logback" % "1.0.9"

  lazy val h2 = "org.openmole" %% "org-h2" % "1.4.192"

  lazy val bonecp = "org.openmole" %% "com-jolbox-bonecp" % "0.8.0-rc1"

  lazy val slick = "org.openmole" %% "com-typesafe-slick" % "3.1.1"

  lazy val slf4j = "org.openmole" %% "org-slf4j" % "1.7.10"

  lazy val xstream = "org.openmole" %% "com-thoughtworks-xstream" % "1.4.8"

  lazy val scalaLang = "org.openmole" %% "org-scala-lang-scala-library" % "2.11.8"

  lazy val jasypt = "org.openmole" %% "org-jasypt-encryption" % "1.9.2"

  lazy val netLogo5Version = "5.3.1"
  lazy val netlogo5 = "org.openmole" % "ccl-northwestern-edu-netlogo5" % netLogo5Version

  lazy val guava = "com.google.guava" % "guava" % "19.0"

  lazy val scalaTagsVersion = "0.6.2"
  lazy val scalaJSDomVersion = "0.9.1"
  lazy val rxVersion = "0.3.1"
  lazy val scalaUpickleVersion = "0.4.3"
  lazy val scalaAutowireVersion = "0.2.6"
  lazy val sourcecodeVersion = "0.1.2"

  lazy val upickle = "org.openmole" %% "upickle" % scalaUpickleVersion
  lazy val autowire = "org.openmole" %% "autowire" % scalaAutowireVersion
  lazy val scalaTags = "org.openmole" %% "com-scalatags" % scalaTagsVersion
  lazy val rx = "org.openmole" %% "rx" % rxVersion

  lazy val scaladgetVersion = "0.9.1-SNAPSHOT"

  lazy val scalajsVersion = "0.6.13"
  lazy val scaladgetJS = libraryDependencies += "fr.iscpif" %%% "scaladget" % scaladgetVersion
  lazy val scalajsDomJS = libraryDependencies += "org.scala-js" %%% "scalajs-dom" % scalaJSDomVersion
  lazy val rxJS = libraryDependencies += "com.lihaoyi" %%% "scalarx" % rxVersion
  lazy val scalaTagsJS = libraryDependencies += "com.lihaoyi" %%% "scalatags" % scalaTagsVersion
  lazy val autowireJS = libraryDependencies += "com.lihaoyi" %%% "autowire" % scalaAutowireVersion
  lazy val upickleJS = libraryDependencies += "com.lihaoyi" %%% "upickle" % scalaUpickleVersion
  lazy val sourcecodeJS = libraryDependencies += "com.lihaoyi" %%% "sourcecode" % sourcecodeVersion

  lazy val scalajsTools = "org.openmole" %% "scalajs-tools" % scalajsVersion
  lazy val scalajs = "org.openmole" %% "scalajs" % scalajsVersion

  lazy val mgo = "org.openmole" %% "fr-iscpif-mgo" % "2.0"
  lazy val family = "org.openmole" %% "fr-iscpif-family" % "1.3"
  lazy val monocle = "org.openmole" %% "monocle" % "1.2.0"

  lazy val d3 = "org.webjars" % "d3js" % d3Version

  lazy val jquery = "org.webjars" % "jquery" % jqueryVersion

  lazy val ace = "org.webjars" % "ace" % aceVersion

  lazy val tooltipster = "org.webjars" % "tooltipster" % tooltipserVersion

  lazy val opencsv = "org.openmole" %% "au-com-bytecode-opencsv" % "2.3"

  lazy val arm = "org.openmole" %% "com-jsuereth-scala-arm" % "1.4"

  lazy val scalajHttp = "org.openmole" %% "org-scalaj-scalaj-http" % "0.3.15"

  lazy val scopt = "org.openmole" %% "com-github-scopt" % "3.2.0"

  lazy val scalabc = "org.openmole" %% "fr-iscpif-scalabc" % "0.4"

  lazy val equinoxOSGi = "org.eclipse" % "osgi" % "3.10.0-v20140606-1445"

  lazy val scalatest = "org.scalatest" %% "scalatest" % "2.2.4" % "test"

  lazy val scalatexSite = "org.openmole" %% "com-lihaoyi-scalatex-site" % "0.3.6"

  lazy val math = "org.openmole" %% "org-apache-commons-math" % "3.5"

  lazy val collections = "org.openmole" %% "org-apache-commons-collections" % "4.1"

  lazy val exec = "org.openmole" %% "org-apache-commons-exec" % "1.1"

  lazy val log4j = "org.openmole" %% "org-apache-log4j" % "1.2.17"

  lazy val logging = "org.openmole" %% "org-apache-commons-logging" % "1.2"

  lazy val lang3 = "org.openmole" %% "org-apache-commons-lang3" % "3.4"

  lazy val sshd = "org.openmole" %% "org-apache-sshd" % "1.0.0"

  lazy val ant = "org.openmole" %% "org-apache-ant" % "1.8.0"

  lazy val codec = "org.openmole" %% "org-apache-commons-codec" % "1.10"

  lazy val async = "org.openmole" %% "scala-async" % "0.9.1"

  lazy val jgit = "org.openmole" %% "org-eclipse-jgit" % "3.7.1"

  lazy val toolxitBibtex = "org.openmole" %% "toolxit-bibtex" % "0.1"

  lazy val scalaz = "org.openmole" %% "scalaz" % "7.2.0"

  lazy val clapper = "org.openmole" %% "org-clapper" % "1.0.5"

  lazy val asm = "org.openmole" %% "org-objectweb-asm" % "5.1"

  lazy val configuration = "org.openmole" %% "org-apache-commons-configuration2" % "2.1"

  lazy val spray = "io.spray" %% "spray-json" % "1.3.2"

  lazy val json4s = "org.openmole" %% "org-json4s" % "3.4.0"

}
