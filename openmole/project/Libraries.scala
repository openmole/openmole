import org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._
import sbt.Keys._
import sbt._

object Libraries {

  lazy val gridscaleVersion = "2.0-SNAPSHOT"
  lazy val bouncyCastleVersion = "1.50"
  lazy val aceVersion = "01.08.2014"
  lazy val d3Version = "3.5.12"
  lazy val tooltipserVersion = "3.3.0"
  lazy val netLogo5Version = "5.3.1"
  lazy val netLogo6Version = "6.0.2"
  lazy val scalaTagsVersion = "0.6.5"
  lazy val scalaJSDomVersion = "0.9.3"
  lazy val rxVersion = "0.3.2"
  lazy val scalaUpickleVersion = "0.4.4"
  lazy val scalaAutowireVersion = "0.2.6"
  lazy val sourcecodeVersion = "0.1.3"
  lazy val scaladgetVersion = "0.9.5"
  lazy val json4sVersion = "3.5.0"
  lazy val circeVersion = "0.8.0"
  lazy val catsVersion = "0.9.0"
  lazy val scalajsVersion = "0.6.18"

  lazy val scaladgetJS = libraryDependencies += "fr.iscpif" %%% "scaladget" % scaladgetVersion
  lazy val scalajsDomJS = libraryDependencies += "org.scala-js" %%% "scalajs-dom" % scalaJSDomVersion
  lazy val rxJS = libraryDependencies += "com.lihaoyi" %%% "scalarx" % rxVersion
  lazy val scalaTagsJS = libraryDependencies += "com.lihaoyi" %%% "scalatags" % scalaTagsVersion
  lazy val autowireJS = libraryDependencies += "com.lihaoyi" %%% "autowire" % scalaAutowireVersion
  lazy val upickleJS = libraryDependencies += "com.lihaoyi" %%% "upickle" % scalaUpickleVersion
  lazy val sourcecodeJS = libraryDependencies += "com.lihaoyi" %%% "sourcecode" % sourcecodeVersion
  lazy val scalajsMarked = libraryDependencies += "com.github.karasiq" %%% "scalajs-marked" % "1.0.2"
  lazy val htmlparser2 = libraryDependencies += "com.definitelyscala" %%% "scala-js-htmlparser2" % "1.0.2"

  lazy val d3 = "org.webjars" % "d3js" % d3Version
  lazy val ace = "org.webjars" % "ace" % aceVersion
  lazy val tooltipster = "org.webjars" % "tooltipster" % tooltipserVersion

  lazy val scalatest = "org.scalatest" %% "scalatest" % "3.0.1" % "test"

  /** ------- Bundles -------------- */


  lazy val addScalaLang = libraryDependencies += "org.openmole.library" %% "org-scala-lang-scala-library" % scalaVersion.value

  lazy val scalatra = "org.openmole.library" %% "org-scalatra" % "2.5.0"
  lazy val logback = "org.openmole.library" %% "ch-qos-logback" % "1.0.9"
  lazy val h2 = "org.openmole.library" %% "org-h2" % "1.4.195"
  lazy val bonecp = "org.openmole.library" %% "com-jolbox-bonecp" % "0.8.0-rc1"
  lazy val slick = "org.openmole.library" %% "com-typesafe-slick" % "3.2.0"
  lazy val slf4j = "org.openmole.library" %% "org-slf4j" % "1.7.10"
  lazy val xstream = "org.openmole.library" %% "com-thoughtworks-xstream" % "1.4.9"
  lazy val jasypt = "org.openmole.library" %% "org-jasypt-encryption" % "1.9.2"
  lazy val opencsv = "org.openmole.library" %% "au-com-bytecode-opencsv" % "2.3"
  lazy val arm = "org.openmole.library" %% "com-jsuereth-scala-arm" % "2.0"
  lazy val scalajHttp = "org.openmole.library" %% "org-scalaj-scalaj-http" % "0.3.15"
  lazy val scopt = "org.openmole.library" %% "com-github-scopt" % "3.5.0"
  lazy val scalabc = "org.openmole.library" %% "fr-iscpif-scalabc" % "0.4"
  lazy val scalatexSite = "org.openmole.library" %% "com-lihaoyi-scalatex-site" % "0.3.11"
  lazy val math = "org.openmole.library" %% "org-apache-commons-math" % "3.6.1"
  lazy val collections = "org.openmole.library" %% "org-apache-commons-collections" % "4.1"
  lazy val exec = "org.openmole.library" %% "org-apache-commons-exec" % "1.3"
  lazy val log4j = "org.openmole.library" %% "org-apache-log4j" % "1.2.17"
  lazy val logging = "org.openmole.library" %% "org-apache-commons-logging" % "1.2"
  lazy val lang3 = "org.openmole.library" %% "org-apache-commons-lang3" % "3.4"
  lazy val sshd = "org.openmole.library" %% "org-apache-sshd" % "1.2.0"
  lazy val ant = "org.openmole.library" %% "org-apache-ant" % "1.8.0"
  lazy val codec = "org.openmole.library" %% "org-apache-commons-codec" % "1.10"
  lazy val async = "org.openmole.library" %% "scala-async" % "0.9.6"
  lazy val jgit = "org.openmole.library" %% "org-eclipse-jgit" % "3.7.1"
  lazy val cats = "org.openmole.library" %% "cats" % catsVersion
  lazy val clapper = "org.openmole.library" %% "org-clapper" % "1.1.2"
  lazy val asm = "org.openmole.library" %% "org-objectweb-asm" % "5.1"
  lazy val configuration = "org.openmole.library" %% "org-apache-commons-configuration2" % "2.1"
  lazy val json4s = "org.openmole.library" %% "org-json4s" % json4sVersion
  lazy val circe = "org.openmole.library" %% "io-circe" % circeVersion
  lazy val scalajsTools = "org.openmole.library" %% "scalajs-tools" % scalajsVersion
  lazy val scalajs = "org.openmole.library" %% "scalajs" % scalajsVersion
  lazy val mgo = "org.openmole.library" %% "mgo" % "3.0-SNAPSHOT"
  lazy val family = "org.openmole.library" %% "fr-iscpif-family" % "1.3"
  lazy val monocle = "org.openmole.library" %% "monocle" % "1.4.0"
  lazy val upickle = "org.openmole.library" %% "upickle" % scalaUpickleVersion
  lazy val autowire = "org.openmole.library" %% "autowire" % scalaAutowireVersion
  lazy val scalaTags = "org.openmole.library" %% "com-scalatags" % scalaTagsVersion
  lazy val rx = "org.openmole.library" %% "rx" % rxVersion
  lazy val netlogo5 = "org.openmole.library" % "ccl-northwestern-edu-netlogo5" % netLogo5Version
  lazy val netlogo6 = "org.openmole.library" % "ccl-northwestern-edu-netlogo6" % netLogo6Version
  lazy val sourceCode = "org.openmole.library" %% "sourcecode" % sourcecodeVersion
  lazy val txtmark = "org.openmole.library" %% "com-github-rjeschke-txtmark" % "0.13"

  def httpClientVersion = "4.5.3"
  lazy val httpClient =
    Seq(
      "org.apache.httpcomponents" % "httpclient-osgi" % httpClientVersion,
      "org.apache.httpcomponents" % "httpmime" % httpClientVersion,
      "org.apache.httpcomponents" % "httpcore-osgi" % "4.4.7"
    )

  lazy val toolxitBibtex = "org.openmole" %% "toolxit-bibtex" % "0.2"

  lazy val gridscale = "org.openmole.library" %% "gridscale" % gridscaleVersion
  lazy val gridscaleSSH = "org.openmole.library" %% "gridscale-ssh" % gridscaleVersion
//  lazy val gridscalePBS = "fr.iscpif.gridscale.bundle" %% "pbs" % gridscaleVersion
//  lazy val gridscaleSGE = "fr.iscpif.gridscale.bundle" %% "sge" % gridscaleVersion
//  lazy val gridscaleCondor = "fr.iscpif.gridscale.bundle" %% "condor" % gridscaleVersion
//  lazy val gridscaleSLURM = "fr.iscpif.gridscale.bundle" %% "slurm" % gridscaleVersion
//  lazy val gridscaleGlite = "fr.iscpif.gridscale.bundle" %% "egi" % gridscaleVersion
  lazy val gridscaleHTTP = httpClient ++ Seq("org.openmole.library" %% "gridscale-http" % gridscaleVersion)
  lazy val gridscaleLocal = "org.openmole.library" %% "gridscale-local" % gridscaleVersion
//  lazy val gridscaleOAR = "fr.iscpif.gridscale.bundle" %% "oar" % gridscaleVersion

  lazy val scalaz = "org.scalaz" %% "scalaz-core" % "7.2.8"
  lazy val guava = "com.google.guava" % "guava" % "19.0"
  lazy val spray = "io.spray" %% "spray-json" % "1.3.2"
  lazy val bouncyCastle = "org.bouncycastle" % "bcpkix-jdk15on" % bouncyCastleVersion
  lazy val equinoxOSGi = "org.eclipse" % "osgi" % "3.10.0-v20140606-1445"
  lazy val osgiCompendium = "org.osgi" % "org.osgi.compendium" % "4.3.1"

  lazy val squants = "org.typelevel"  %% "squants"  % "1.3.0"
  lazy val shapeless = "com.chuusai" %% "shapeless" % "2.3.2"


}
