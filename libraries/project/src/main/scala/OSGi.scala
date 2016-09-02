package root

import sbt._
import Keys._
import com.typesafe.sbt.osgi.OsgiKeys
import OsgiKeys._
import org.openmole.buildsystem.OMKeys._

object OSGi extends Defaults {

  val dir = file("bundles")

  lazy val scalatraVersion = "2.4.0"
  lazy val jettyVersion = "9.2.14.v20151106"

  lazy val scalatra = OsgiProject("org.scalatra",
    exports = Seq("org.scalatra.*, org.fusesource.*", "grizzled.*", "com.fasterxml.jackson.*", "org.json4s.*", "org.eclipse.jetty.*", "javax.*"),
    privatePackages = Seq("!scala.*", "!org.slf4j.*", "*")) settings(
      libraryDependencies += "org.scalatra" %% "scalatra" % scalatraVersion,
      libraryDependencies += "org.scalatra" %% "scalatra-json" % scalatraVersion,
      libraryDependencies += "org.scalatra" %% "scalatra-auth" % scalatraVersion,
      libraryDependencies += "org.eclipse.jetty" % "jetty-webapp" % jettyVersion,
      libraryDependencies += "org.eclipse.jetty" % "jetty-server" % jettyVersion,
      libraryDependencies +=  "org.json4s" %% "json4s-jackson" % "3.3.0",
      version := scalatraVersion)


  lazy val logback = OsgiProject("ch.qos.logback", exports = Seq("ch.qos.logback.*", "org.slf4j.impl"), dynamicImports = Seq("*")) settings
    (libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.9", version := "1.0.9")

  lazy val h2Version = "1.4.192"
  lazy val h2 = OsgiProject("org.h2", dynamicImports = Seq("*"), privatePackages = Seq("META-INF.*")) settings
    (libraryDependencies += "com.h2database" % "h2" % h2Version, version := h2Version)

  lazy val bonecp = OsgiProject("com.jolbox.bonecp", dynamicImports = Seq("*")) settings
    (libraryDependencies += "com.jolbox" % "bonecp" % "0.8.0-rc1", version := "0.8.0-rc1")

  lazy val slickVersion = "3.1.1"
  lazy val slick = OsgiProject("com.typesafe.slick", exports = Seq("slick.*"), privatePackages = Seq("org.reactivestreams.*")) settings
    (libraryDependencies += "com.typesafe.slick" %% "slick" % slickVersion, version := slickVersion)

  lazy val slf4j = OsgiProject("org.slf4j") settings(
    libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.10",
    version := "1.7.10"
    )

  lazy val xstream = OsgiProject(
    "com.thoughtworks.xstream",
    imports = Seq(
      "!com.bea.xml.stream.*",
      "!com.ctc.wstx.stax.*",
      "!net.sf.cglib.*",
      "!nu.xom.*",
      "!org.codehaus.jettison.*",
      "!org.dom4j.*",
      "!org.jdom.*",
      "!org.jdom2.*",
      "!org.w3c.*",
      "!org.xml.sax.*",
      "!sun.misc.*",
      "*"),
    privatePackages = Seq("!scala.*", "META-INF.*", "*")) settings(
    libraryDependencies ++= Seq("com.thoughtworks.xstream" % "xstream" % "1.4.8", "net.sf.kxml" % "kxml2" % "2.3.0"),
    version := "1.4.8")

  lazy val scalaLang = OsgiProject(
    "org.scala-lang.scala-library",
    global = true,
    exports = Seq("akka.*", "com.typesafe.*", "scala.*", "scalax.*", "jline.*"),
    privatePackages = Seq("*"), imports = Seq("!org.apache.tools.ant.*", "!sun.misc.*" ,"*")) settings
    (libraryDependencies <++= (scalaVersion) { sV ⇒
      Seq("org.scala-lang" % "scala-library" % sV,
        "org.scala-lang" % "scala-reflect" % sV,
        "org.scala-lang" % "scalap" % sV,
        "jline" % "jline" % "2.12.1",
        "org.scala-stm" %% "scala-stm" % "0.7",
        "com.typesafe" % "config" % "1.2.1",
        "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.3",
        "org.scala-lang" % "scala-compiler" % sV
      )
    }, version := scalaVersion.value)

  lazy val jodaTime = OsgiProject("org.joda.time") settings(
    libraryDependencies += "joda-time" % "joda-time" % "1.6",
    version := "1.6"
    )

  lazy val jasyptVersion = "1.9.2"
  lazy val jasypt = OsgiProject("org.jasypt.encryption", exports = Seq("org.jasypt.*")) settings(
    libraryDependencies += "org.jasypt" % "jasypt" % jasyptVersion,
    version := jasyptVersion
    )

  lazy val netLogo5Version = "5.3.1"

  lazy val netlogo5 = OsgiProject(
    "ccl.northwestern.edu.netlogo5",
    exports = Seq("org.nlogo.*"),
    privatePackages = Seq("**"),
    imports = Seq("!*")) settings
    (libraryDependencies ++= Seq("ccl.northwestern.edu" % "netlogo" % netLogo5Version % "provided" from s"https://github.com/NetLogo/NetLogo/releases/download/$netLogo5Version/NetLogo.jar",
      "org.scala-lang" % "scala-library" % "2.9.2" % "provided",
      "asm" % "asm-all" % "3.3.1" % "provided",
      "org.picocontainer" % "picocontainer" % "2.13.6" % "provided"), version := netLogo5Version, scalaVersion := "2.9.2", crossPaths := false, bundleType := Set("plugin"))

  lazy val guava = OsgiProject("com.google.guava",
    exports = Seq("com.google.common.*"), privatePackages = Seq("!scala.*", "*")) settings(libraryDependencies ++=
    Seq("com.google.guava" % "guava" % "18.0", "com.google.code.findbugs" % "jsr305" % "1.3.9"),
    version := "18.0"
    )

  lazy val scalaTagsVersion = "0.6.0"
  lazy val scalaRxVersion = "0.3.1"
  lazy val scalaDomVersion = "0.9.0"
  lazy val querkiJSQueryVersion = "0.11"
  lazy val scalaUpickleVersion = "0.4.1"
  lazy val scalaAutowireVersion = "0.2.5"
  lazy val scalajsVersion = "0.6.11"

  lazy val rx = OsgiProject("rx", exports = Seq("rx.*")) settings(
    libraryDependencies ++= Seq("com.lihaoyi" %% "scalarx" % scalaRxVersion),
    version := scalaRxVersion)

  lazy val scalajsTools = OsgiProject("scalajs-tools", exports = Seq("scala.scalajs.*", "org.scalajs.core.tools.*", "org.scalajs.core.ir.*", "com.google.javascript.*", "com.google.common.*", "rhino_ast.java.com.google.javascript.rhino.*", "org.json.*")) settings(
    libraryDependencies += "org.scala-js" %% "scalajs-tools" % scalajsVersion, version := scalajsVersion)

  lazy val scalaJS = OsgiProject("scalajs", exports = Seq("scala.scalajs.*"), imports = Seq("*")) settings (
    libraryDependencies += "org.scala-js" %% "scalajs-library" % scalajsVersion,
    version := scalajsVersion
  )

  lazy val scalaTags = OsgiProject("com.scalatags", exports = Seq("scalatags.*")) settings(
    libraryDependencies ++= Seq("com.lihaoyi" %% "scalatags" % scalaTagsVersion),
    version := scalaTagsVersion)

  lazy val scalatexSite =
    OsgiProject("com.lihaoyi.scalatex-site", exports = Seq("scalatex.*", "ammonite.*", "fastparse.*"), privatePackages = Seq("META-INF.**", "pprint.*", "scalaj.*", "scalaparse.*"), imports = Seq("*")) settings (
      libraryDependencies += "com.lihaoyi" %% "scalatex-site" % "0.3.6",
      version := "0.3.6")

  lazy val upickle = OsgiProject("upickle", exports = Seq("upickle.*", "jawn.*", "derive.*", "sourcecode.*"), imports = Seq("*")) settings(
    libraryDependencies ++= Seq("com.lihaoyi" %% "upickle" % scalaUpickleVersion),
    version := scalaUpickleVersion)

  lazy val autowire = OsgiProject("autowire", exports = Seq("autowire.*")) settings(
    libraryDependencies ++= Seq("com.lihaoyi" %% "autowire" % scalaAutowireVersion),
    version := scalaAutowireVersion)

  lazy val jsonSimpleVersion = "1.1.1"
  lazy val jsonSimple = OsgiProject("json-simple", exports = Seq("org.json.simple.*")) settings(
    libraryDependencies += "com.googlecode.json-simple" % "json-simple" % jsonSimpleVersion, version := jsonSimpleVersion)

  lazy val closureCompilerVersion = "v20130603"
  lazy val closureCompiler = OsgiProject("closure-compiler", exports = Seq("com.google.javascript.*")) settings(
    libraryDependencies += "com.google.javascript" % "closure-compiler" % closureCompilerVersion, version := closureCompilerVersion)

  lazy val mgoVersion = "2.0"

  lazy val mgo = OsgiProject("fr.iscpif.mgo") settings(
    libraryDependencies += "fr.iscpif" %% "mgo" % mgoVersion,
    version := mgoVersion
    ) dependsOn(monocle, scalaz)

  lazy val familyVersion = "1.3"
  lazy val family = OsgiProject("fr.iscpif.family") settings(
    libraryDependencies += "fr.iscpif" %% "family" % familyVersion,
    version := familyVersion
    )

  lazy val opencsv = OsgiProject("au.com.bytecode.opencsv") settings(
    libraryDependencies += "net.sf.opencsv" % "opencsv" % "2.3",
    version := "2.3"
    )

  lazy val arm = OsgiProject("com.jsuereth.scala-arm") settings(
    libraryDependencies += "com.jsuereth" %% "scala-arm" % "1.4",
    version := "1.4",
    exportPackage := Seq("resource.*"))

  lazy val scalajHttp = OsgiProject("org.scalaj.scalaj-http") settings(
    libraryDependencies += "org.scalaj" %% "scalaj-http" % "0.3.15",
    version := "0.3.15",
    exportPackage := Seq("scalaj.http.*")
    )

  lazy val scopt = OsgiProject("com.github.scopt", exports = Seq("scopt.*")) settings(
    libraryDependencies += "com.github.scopt" %% "scopt" % "3.2.0",
    version := "3.2.0"
    )

  lazy val scalabc = OsgiProject("fr.iscpif.scalabc", privatePackages = Seq("!scala.*", "!junit.*", "*")) settings(
    libraryDependencies += "fr.iscpif" %% "scalabc" % "0.4",
    version := "0.4"
    )

   lazy val async =
     OsgiProject("scala-async") settings (
       libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.9.1",
       version := "0.9.1",
       exportPackage := Seq("scala.async.*"))

  lazy val mathVersion = "3.5"
  lazy val math = OsgiProject("org.apache.commons.math", exports = Seq("org.apache.commons.math3.*"), privatePackages = Seq("assets.*")) settings
    (libraryDependencies += "org.apache.commons" % "commons-math3" % mathVersion, version := mathVersion)

  lazy val exec = OsgiProject("org.apache.commons.exec") settings
    (libraryDependencies += "org.apache.commons" % "commons-exec" % "1.1", version := "1.1")

  lazy val log4j = OsgiProject("org.apache.log4j") settings
    (libraryDependencies += "log4j" % "log4j" % "1.2.17", version := "1.2.17")

  lazy val logging = OsgiProject("org.apache.commons.logging") settings
    (libraryDependencies += "commons-logging" % "commons-logging" % "1.2", version := "1.2")

  lazy val lang3 = OsgiProject("org.apache.commons.lang3") settings (
    libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.4", version := "3.4")

  lazy val sshd = OsgiProject("org.apache.sshd", exports = Seq("org.apache.sshd.*", "org.apache.mina.*"), dynamicImports = Seq("*"), privatePackages = Seq("META-INF.*")) settings
    (libraryDependencies += "org.apache.sshd" % "sshd-core" % "1.0.0", version := "1.0.0")

  lazy val ant = OsgiProject("org.apache.ant") settings
    (libraryDependencies += "org.apache.ant" % "ant" % "1.8.0", version := "1.8.0")

  lazy val codec = OsgiProject("org.apache.commons.codec") settings
    (libraryDependencies += "commons-codec" % "commons-codec" % "1.10", version := "1.10")

  lazy val collections = OsgiProject("org.apache.commons.collections", exports = Seq("org.apache.commons.collections4.*")) settings
    (libraryDependencies += "org.apache.commons" % "commons-collections4" % "4.1", version := "4.1")

  lazy val jgit = OsgiProject("org.eclipse.jgit", privatePackages = Seq("!scala.*", "!org.slf4j.*", "*"))  settings (
    libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit.java7" % "3.7.1.201504261725-r", version := "3.7.1" )

  lazy val txtmark = OsgiProject("com.github.rjeschke.txtmark", privatePackages = Seq("!scala.*", "!org.slf4j.*", "*"))  settings (
    libraryDependencies += "com.github.rjeschke" % "txtmark" % "0.13", version := "0.13" )

 lazy val clapperVersion = "1.0.5"
 lazy val clapper = OsgiProject("org.clapper", exports = Seq("!scala.*","!grizzled.*","!jline.*","!org.fusesource.*","!org.slf4j.*","*")) settings (
   libraryDependencies += "org.clapper" % "classutil_2.11" % clapperVersion, version := clapperVersion)

  val monocleVersion = "1.2.0"

  lazy val monocle = OsgiProject("monocle", privatePackages = Seq("!scala.*", "!scalaz.*", "*")) settings(
    libraryDependencies += "com.github.julien-truffaut" %% "monocle-core" % monocleVersion,
    libraryDependencies += "com.github.julien-truffaut" %% "monocle-generic" % monocleVersion,
    libraryDependencies += "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion,
    version := monocleVersion
    ) dependsOn(scalaz)

  val scalazVersion = "7.2.0"

  lazy val scalaz = OsgiProject("scalaz", privatePackages = Seq("!scala.*", "*")) settings (
    libraryDependencies ++= Seq(
      "org.scalaz" %% "scalaz-core" % scalazVersion,
      "org.scalaz" %% "scalaz-effect" % scalazVersion
    ),
    version := scalazVersion)

  val asmVersion = "5.1"

  lazy val asm = OsgiProject("org.objectweb.asm") settings (
    libraryDependencies += "org.ow2.asm" % "asm" % asmVersion,
    version := asmVersion)


  lazy val config = OsgiProject("org.apache.commons.configuration2",
    privatePackages = Seq("!scala.*", "!org.apache.commons.logging.*","*")) settings (
    libraryDependencies += "org.apache.commons" % "commons-configuration2" % "2.0",
    libraryDependencies += "commons-beanutils" % "commons-beanutils" % "1.9.2",
    version := "2.0")

}
