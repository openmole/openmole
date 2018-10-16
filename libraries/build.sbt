import com.typesafe.sbt.osgi.OsgiKeys._
import org.openmole.buildsystem._

def dir = file("bundles")

def settings = Seq(
  resolvers += DefaultMavenRepository,
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.sonatypeRepo("releases"),
  scalaVersion in Global := "2.12.6",
  scalacOptions ++= Seq("-deprecation"),
  publishArtifact in (packageDoc in publishLocal) := false,
  publishArtifact in (packageSrc in publishLocal) := false,
  organization := "org.openmole.library",
  isSnapshot := true
) 


lazy val scalatraVersion = "2.5.0"
lazy val jettyVersion = "9.2.19.v20160908"

lazy val scalatra = OsgiProject(dir, "org.scalatra",
  exports = Seq("org.scalatra.*, org.fusesource.*", "grizzled.*", "org.eclipse.jetty.*", "javax.*"),
  privatePackages = Seq("!scala.*", "!org.slf4j.*", "*"),
  imports = Seq("scala.*", "org.slf4j.*")) settings(
  libraryDependencies += "org.scalatra" %% "scalatra" % scalatraVersion,
  libraryDependencies += "org.scalatra" %% "scalatra-auth" % scalatraVersion,
  libraryDependencies += "org.eclipse.jetty" % "jetty-webapp" % jettyVersion,
  libraryDependencies += "org.eclipse.jetty" % "jetty-server" % jettyVersion,
  version := scalatraVersion) settings(settings: _*)

lazy val json4s = OsgiProject(dir, "org.json4s",
  exports = Seq("org.json4s.*"),
  privatePackages = Seq("!scala.*", "!org.slf4j.*", "!com.thoughtworks.paranamer.*", "*"),
  imports = Seq("scala.*", "org.slf4j.*", "com.thoughtworks.paranamer.*", "")) settings (
  libraryDependencies +=  "org.json4s" %% "json4s-jackson" % "3.5.0",
  version := "3.5.0") settings(settings: _*)

lazy val shapelessVersion = "2.3.2"
lazy val shapeless =  OsgiProject(dir, "com.chuusai.shapeless", exports = Seq("shapeless.*")) settings (
  libraryDependencies += "com.chuusai" %% "shapeless" % shapelessVersion,
  version := shapelessVersion
) settings(settings: _*)

lazy val circeVersion = "0.9.1"
lazy val circe = OsgiProject(dir, "io.circe",
  exports = Seq("io.circe.*", "!cats.*", "!scala.*", "!shapeless.*"),
  privatePackages = Seq("jawn.*"),
  // TODO force cats version to be >= 0.9.0
  imports = Seq("scala.*", "cats.*", "shapeless.*")) settings (
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-generic-extras",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion),
  version := circeVersion) settings(settings: _*) dependsOn(shapeless)

lazy val logback = OsgiProject(dir, "ch.qos.logback", exports = Seq("ch.qos.logback.*", "org.slf4j.impl"), dynamicImports = Seq("*")) settings
  (libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.9", version := "1.0.9") settings(settings: _*)

lazy val h2Version = "1.4.196"
lazy val h2 = OsgiProject(dir, "org.h2", dynamicImports = Seq("*"), privatePackages = Seq("META-INF.*")) settings
  (libraryDependencies += "com.h2database" % "h2" % h2Version, version := h2Version) settings(settings: _*)

lazy val bonecp = OsgiProject(dir, "com.jolbox.bonecp", dynamicImports = Seq("*")) settings
  (libraryDependencies += "com.jolbox" % "bonecp" % "0.8.0-rc1", version := "0.8.0-rc1") settings(settings: _*)

lazy val slickVersion = "3.2.0"
lazy val slick = OsgiProject(dir,"com.typesafe.slick", exports = Seq("slick.*"), privatePackages = Seq("org.reactivestreams.*")) settings
  (libraryDependencies += "com.typesafe.slick" %% "slick" % slickVersion, version := slickVersion) settings(settings: _*)

lazy val slf4j = OsgiProject(dir,"org.slf4j") settings(
  libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.10",
  version := "1.7.10") settings(settings: _*)

lazy val xstream = OsgiProject(
  dir,
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
    "!org.joda.time.*",
    "!javax.*",
    "*"),
  privatePackages = Seq("!scala.*", "META-INF.services.*", "*")) settings(
  libraryDependencies ++= Seq("com.thoughtworks.xstream" % "xstream" % "1.4.10", "net.sf.kxml" % "kxml2" % "2.3.0"),
  version := "1.4.10") settings(settings: _*)

lazy val scalaLang = OsgiProject(
  dir,
  "org.scala-lang.scala-library",
  global = true,
  exports = Seq("akka.*", "com.typesafe.*", "scala.*", "scalax.*", "jline.*"),
  privatePackages = Seq("*", "META-INF.native.**"), imports = Seq("!org.apache.tools.ant.*", "!sun.misc.*" ,"*")) settings
  (libraryDependencies ++= {
    Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.0.6",
      "org.scala-lang" % "scala-library" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang" % "scalap" % scalaVersion.value,
      "jline" % "jline" % "2.12.1",
      "org.scala-stm" %% "scala-stm" % "0.8",
      "com.typesafe" % "config" % "1.2.1",
      "org.scala-lang" % "scala-compiler" % scalaVersion.value
    )
  }, version := scalaVersion.value) settings(settings: _*)

lazy val jasyptVersion = "1.9.2"
lazy val jasypt = OsgiProject(dir, "org.jasypt.encryption", exports = Seq("org.jasypt.*")) settings(
  libraryDependencies += "org.jasypt" % "jasypt" % jasyptVersion,
  version := jasyptVersion
  ) settings(settings: _*)

lazy val netLogo5Version = "5.3.1"

lazy val netlogo5 = OsgiProject(
  dir,
  "ccl.northwestern.edu.netlogo5",
  exports = Seq("org.nlogo.*"),
  privatePackages = Seq("**"),
  imports = Seq("!*")) settings(
    libraryDependencies ++= Seq(
      "ccl.northwestern.edu" % "netlogo" % netLogo5Version % "provided" from s"https://github.com/NetLogo/NetLogo/releases/download/$netLogo5Version/NetLogo.jar",
      "org.scala-lang" % "scala-library" % "2.9.2" % "provided",
      "asm" % "asm-all" % "3.3.1" % "provided",
      "org.picocontainer" % "picocontainer" % "2.13.6" % "provided"), version := netLogo5Version, scalaVersion := "2.9.2", crossPaths := false) settings(settings: _*)

lazy val netLogo6Version = "6.0.4"

lazy val netlogo6 = OsgiProject(
  dir,
  "ccl.northwestern.edu.netlogo6",
  exports = Seq("org.nlogo.*"),
  privatePackages = Seq("**"),
  imports = Seq("!*")) settings (
  //resolvers += Resolver.bintrayRepo("netlogo", "NetLogo-JVM"),
  libraryDependencies ++= Seq(
    "org.nlogo" % "netlogo" % netLogo6Version % "provided" from s"https://dl.bintray.com/netlogo/NetLogo-JVM/org/nlogo/netlogo/$netLogo6Version/netlogo-$netLogo6Version.jar",
    "org.scala-lang" % "scala-library" % "2.12.4" % "provided",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.5" % "provided",
    "commons-codec" % "commons-codec" % "1.10" % "provided",
    "org.ow2.asm" % "asm-all" % "5.0.4" % "provided",
    "org.picocontainer" % "picocontainer" % "2.13.6" % "provided",
    "org.parboiled" %% "parboiled" % "2.1.3" % "provided"
  ), version := netLogo6Version, scalaVersion := "2.12.4", crossPaths := false) settings(settings: _*)

lazy val scalaTagsVersion = "0.6.5"
lazy val scalaRxVersion = "0.4.0"
lazy val scalaDomVersion = "0.9.3"
lazy val scalaUpickleVersion = "0.4.4"
lazy val scalaBoopickleVersion = "1.2.6"
lazy val scalaAutowireVersion = "0.2.6"
lazy val scalajsVersion = "0.6.23"

lazy val rx = OsgiProject(dir, "rx", exports = Seq("rx.*")) settings(
  libraryDependencies ++= Seq("com.lihaoyi" %% "scalarx" % scalaRxVersion),
  version := scalaRxVersion) settings(settings: _*)

lazy val scalajsTools = OsgiProject(dir, "scalajs-tools", exports = Seq("scala.scalajs.*", "org.scalajs.core.tools.*", "org.scalajs.core.ir.*", "com.google.javascript.*", "com.google.common.*", "rhino_ast.java.com.google.javascript.rhino.*", "com.google.gson.*", "com.google.debugging.sourcemap.*", "org.json.*", "java7compat.nio.charset.*", "com.google.protobuf.*")) settings(
  libraryDependencies += "org.scala-js" %% "scalajs-tools" % scalajsVersion, version := scalajsVersion) settings(settings: _*)

lazy val scalaJS = OsgiProject(dir, "scalajs", exports = Seq("scala.scalajs.*"), imports = Seq("*")) settings (
  libraryDependencies += "org.scala-js" %% "scalajs-library" % scalajsVersion,
  version := scalajsVersion
  ) settings(settings: _*)

lazy val scalaTags = OsgiProject(dir, "com.scalatags", exports = Seq("scalatags.*")) settings(
  libraryDependencies ++= Seq("com.lihaoyi" %% "scalatags" % scalaTagsVersion),
  version := scalaTagsVersion) settings(settings: _*)

lazy val scalatexSite =
  OsgiProject(dir, "com.lihaoyi.scalatex-site", exports = Seq("scalatex.*", "ammonite.*", "fastparse.*"), privatePackages = Seq("META-INF.**", "pprint.*", "scalaj.*", "scalaparse.*", "geny.*"), imports = Seq("*")) settings (
    libraryDependencies += "com.lihaoyi" %% "scalatex-site" % "0.3.12",
    version := "0.3.12") settings(settings: _*)

lazy val upickle = OsgiProject(dir, "upickle", exports = Seq("upickle.*", "jawn.*", "derive.*", "sourcecode.*"), imports = Seq("*")) settings(
  libraryDependencies ++= Seq("com.lihaoyi" %% "upickle" % scalaUpickleVersion),
  version := scalaUpickleVersion) settings(settings: _*)

lazy val boopickle = OsgiProject(dir, "boopickle", exports = Seq("boopickle.*"), imports = Seq("*")) settings(
  libraryDependencies ++= Seq("io.suzaku" %% "boopickle" % scalaBoopickleVersion),
  version := scalaBoopickleVersion) settings(settings: _*)

lazy val autowire = OsgiProject(dir, "autowire", exports = Seq("autowire.*")) settings(
  libraryDependencies ++= Seq("com.lihaoyi" %% "autowire" % scalaAutowireVersion),
  version := scalaAutowireVersion) settings(settings: _*)

lazy val jsonSimpleVersion = "1.1.1"
lazy val jsonSimple = OsgiProject(dir, "json-simple", exports = Seq("org.json.simple.*")) settings(
  libraryDependencies += "com.googlecode.json-simple" % "json-simple" % jsonSimpleVersion, version := jsonSimpleVersion) settings(settings: _*)

lazy val closureCompilerVersion = "v20130603"
lazy val closureCompiler = OsgiProject(dir, "closure-compiler", exports = Seq("com.google.javascript.*")) settings(
  libraryDependencies += "com.google.javascript" % "closure-compiler" % closureCompilerVersion, version := closureCompilerVersion) settings(settings: _*)

lazy val catsVersion = "1.0.1"
lazy val cats =
  OsgiProject(dir, "cats") settings (
    libraryDependencies += "org.typelevel" %% "cats-core" % catsVersion,
    libraryDependencies += "org.typelevel" %% "cats-free" % catsVersion,
    version := catsVersion
  ) settings(settings: _*)

lazy val freedslVersion = "0.26"
lazy val squantsVersion = "1.3.0"

lazy val squants = 
  OsgiProject(dir, "squants") settings (
    libraryDependencies += "org.typelevel" %% "squants" % squantsVersion,
    version := squantsVersion
  ) settings(settings: _*)


lazy val freedsl =
  OsgiProject(dir, "freedsl", exports = Seq("freedsl.*", "freestyle.*", "mainecoon.*")) settings (
    libraryDependencies += "fr.iscpif.freedsl" %% "freedsl" % freedslVersion,
    libraryDependencies += "fr.iscpif.freedsl" %% "random" % freedslVersion,
    libraryDependencies += "fr.iscpif.freedsl" %% "system" % freedslVersion,
    libraryDependencies += "fr.iscpif.freedsl" %% "io" % freedslVersion,
    libraryDependencies += "fr.iscpif.freedsl" %% "filesystem" % freedslVersion,
    libraryDependencies += "fr.iscpif.freedsl" %% "errorhandler" % freedslVersion,
    libraryDependencies += "fr.iscpif.freedsl" %% "tool" % freedslVersion,
    libraryDependencies += "fr.iscpif.freedsl" %% "dsl" % freedslVersion,
    version := freedslVersion
  ) dependsOn(cats, squants) settings(settings: _*)

lazy val mgoVersion = "3.17"

lazy val mgo = OsgiProject(dir, "mgo", imports = Seq("!better.*", "*")) settings(
  libraryDependencies += "fr.iscpif" %% "mgo" % mgoVersion,
  version := mgoVersion) dependsOn(monocle, freedsl, math) settings(settings: _*)

/*lazy val familyVersion = "1.3"
lazy val family = OsgiProject(dir, "fr.iscpif.family") settings(
  libraryDependencies += "fr.iscpif" %% "family" % familyVersion,
  version := familyVersion
  ) settings(settings: _*)*/

lazy val opencsv = OsgiProject(dir, "au.com.bytecode.opencsv") settings(
  libraryDependencies += "net.sf.opencsv" % "opencsv" % "2.3",
  version := "2.3"
  ) settings(settings: _*)

lazy val arm = OsgiProject(dir, "com.jsuereth.scala-arm") settings(
  libraryDependencies += "com.jsuereth" %% "scala-arm" % "2.0",
  version := "2.0",
  exportPackage := Seq("resource.*")) settings(settings: _*)

lazy val scalajHttp = OsgiProject(dir, "org.scalaj.scalaj-http") settings(
  libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.3.0",
  version := "2.3.0",
  exportPackage := Seq("scalaj.http.*")
  ) settings(settings: _*)

lazy val scopt = OsgiProject(dir, "com.github.scopt", exports = Seq("scopt.*")) settings(
  libraryDependencies += "com.github.scopt" %% "scopt" % "3.5.0",
  version := "3.5.0"
  ) settings(settings: _*)

/*lazy val scalabc = OsgiProject(dir, "fr.iscpif.scalabc", privatePackages = Seq("!scala.*", "!junit.*", "*")) settings(
  libraryDependencies += "fr.iscpif" %% "scalabc" % "0.4",
  version := "0.4"
  ) settings(settings: _*)*/

lazy val async =
  OsgiProject(dir, "scala-async") settings (
    libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.9.6",
    version := "0.9.6",
    exportPackage := Seq("scala.async.*")) settings(settings: _*)

lazy val mathVersion = "3.6.1"
lazy val math = OsgiProject(dir, "org.apache.commons.math", exports = Seq("org.apache.commons.math3.*"), privatePackages = Seq("assets.*")) settings
  (libraryDependencies += "org.apache.commons" % "commons-math3" % mathVersion, version := mathVersion) settings(settings: _*)

lazy val exec = OsgiProject(dir, "org.apache.commons.exec") settings
  (libraryDependencies += "org.apache.commons" % "commons-exec" % "1.3", version := "1.3") settings(settings: _*)

lazy val log4j = OsgiProject(dir, "org.apache.log4j") settings
  (libraryDependencies += "log4j" % "log4j" % "1.2.17", version := "1.2.17") settings(settings: _*)
lazy val logging = OsgiProject(dir, "org.apache.commons.logging") settings
  (libraryDependencies += "commons-logging" % "commons-logging" % "1.2", version := "1.2") settings(settings: _*)

lazy val lang3 = OsgiProject(dir, "org.apache.commons.lang3") settings (
  libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.4", version := "3.4") settings(settings: _*)

lazy val ant = OsgiProject(dir, "org.apache.ant") settings
  (libraryDependencies += "org.apache.ant" % "ant" % "1.8.0", version := "1.8.0") settings(settings: _*)

lazy val codec = OsgiProject(dir, "org.apache.commons.codec") settings
  (libraryDependencies += "commons-codec" % "commons-codec" % "1.10", version := "1.10") settings(settings: _*)

lazy val collections = OsgiProject(dir, "org.apache.commons.collections", exports = Seq("org.apache.commons.collections4.*")) settings
  (libraryDependencies += "org.apache.commons" % "commons-collections4" % "4.1", version := "4.1") settings(settings: _*)

lazy val jgit = OsgiProject(dir, "org.eclipse.jgit", privatePackages = Seq("!scala.*", "!org.slf4j.*", "*"))  settings (
  libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "4.11.0.201803080745-r", version := "4.11" ) settings(settings: _*)

lazy val txtmark = OsgiProject(dir, "com.github.rjeschke.txtmark", privatePackages = Seq("!scala.*", "!org.slf4j.*", "*"))  settings (
  libraryDependencies += "com.github.rjeschke" % "txtmark" % "0.13", version := "0.13" ) settings(settings: _*)

lazy val clapperVersion = "1.1.2"
lazy val clapper = OsgiProject(dir, "org.clapper", exports = Seq("!scala.*","!grizzled.*","!jline.*","!org.fusesource.*","!org.slf4j.*","*")) settings (
  libraryDependencies += "org.clapper" %% "classutil" % clapperVersion, version := clapperVersion) settings(settings: _*)

val monocleVersion = "1.5.0"
lazy val monocle = OsgiProject(dir, "monocle",
  privatePackages = Seq("!scala.*", "!scalaz.*", "!shapeless.*", "*"),
  imports = Seq("scala.*", "shapeless.*", "scalaz.*")) settings(
  libraryDependencies ++= Seq (
    "com.github.julien-truffaut" %% "monocle-core",
    "com.github.julien-truffaut" %% "monocle-generic",
    "com.github.julien-truffaut" %% "monocle-macro"
  ).map(_ % monocleVersion),
  version := monocleVersion
  ) settings(settings: _*) dependsOn(shapeless)

val asmVersion = "5.1"

lazy val asm = OsgiProject(dir, "org.objectweb.asm") settings (
  libraryDependencies += "org.ow2.asm" % "asm" % asmVersion,
  version := asmVersion) settings(settings: _*)

lazy val config = OsgiProject(dir, "org.apache.commons.configuration2",
  privatePackages = Seq("!scala.*", "!org.apache.commons.logging.*","*"),
  imports = Seq("org.apache.commons.logging.*")) settings (
  libraryDependencies += "org.apache.commons" % "commons-configuration2" % "2.2",
  libraryDependencies += "commons-beanutils" % "commons-beanutils" % "1.9.2",
  version := "2.2") settings(settings: _*) dependsOn (logging)

lazy val sourceCode = OsgiProject(dir, "sourcecode") settings (
  libraryDependencies += "com.lihaoyi" %% "sourcecode" % "0.1.3",
  version := "0.1.3"
) settings(settings: _*)


def effectasideVersion = "0.2"
lazy val effectaside = OsgiProject(dir, "effectaside", imports = Seq("*")) settings (
  libraryDependencies += "fr.iscpif.effectaside" %% "effect"% effectasideVersion,
  version := effectasideVersion
)

def gridscaleVersion = "2.12"
lazy val gridscale = OsgiProject(dir, "gridscale", imports = Seq("*"), exports = Seq("gridscale.*", "enumeratum.*")) settings (
  libraryDependencies += "fr.iscpif.gridscale" %% "gridscale" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(effectaside)

lazy val gridscaleLocal = OsgiProject(dir, "gridscale.local", imports = Seq("*")) settings (
  libraryDependencies += "fr.iscpif.gridscale" %% "local" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale)

lazy val gridscaleHTTP = OsgiProject(dir, "gridscale.http", imports = Seq("*"), privatePackages = Seq("org.htmlparser.*")) settings (
  libraryDependencies += "fr.iscpif.gridscale" %% "http" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, codec)

lazy val gridscaleSSH = OsgiProject(dir, "gridscale.ssh", imports = Seq("*")) settings (
  libraryDependencies += "fr.iscpif.gridscale" %% "ssh" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(jzlib) dependsOn(gridscale)


lazy val gridscaleCluster = OsgiProject(dir, "gridscale.cluster", imports = Seq("*")) settings (
  libraryDependencies += "fr.iscpif.gridscale" %% "cluster" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscaleSSH)

lazy val gridscaleOAR = OsgiProject(dir, "gridscale.oar", imports = Seq("*")) settings (
  libraryDependencies += "fr.iscpif.gridscale" %% "oar" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, gridscaleCluster)

lazy val gridscalePBS = OsgiProject(dir, "gridscale.pbs", imports = Seq("*")) settings (
  libraryDependencies += "fr.iscpif.gridscale" %% "pbs" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, gridscaleCluster)

lazy val gridscaleSGE = OsgiProject(dir, "gridscale.sge", imports = Seq("*")) settings (
  libraryDependencies += "fr.iscpif.gridscale" %% "sge" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, gridscaleCluster)

lazy val gridscaleCondor = OsgiProject(dir, "gridscale.condor", imports = Seq("*")) settings (
  libraryDependencies += "fr.iscpif.gridscale" %% "condor" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, gridscaleCluster)

lazy val gridscaleSLURM = OsgiProject(dir, "gridscale.slurm", imports = Seq("*")) settings (
  libraryDependencies += "fr.iscpif.gridscale" %% "slurm" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, gridscaleCluster)


lazy val gridscaleEGI = OsgiProject(dir, "gridscale.egi", imports = Seq("*")) settings (
  libraryDependencies += "fr.iscpif.gridscale" %% "egi" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, gridscaleHTTP)

lazy val gridscaleDIRAC = OsgiProject(dir, "gridscale.dirac", imports = Seq("*"), privatePackages = Seq("gridscale.dirac.*", "org.apache.commons.compress.*", "org.brotli.*", "org.tukaani.*")) settings (
  libraryDependencies += "fr.iscpif.gridscale" %% "dirac" % gridscaleVersion,
  libraryDependencies += "org.brotli" % "dec" % "0.1.2",
  libraryDependencies += "org.tukaani" % "xz" % "1.6",
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, gridscaleHTTP)

lazy val gridscaleWebDAV = OsgiProject(dir, "gridscale.webdav", imports = Seq("*")) settings (
  libraryDependencies += "fr.iscpif.gridscale" %% "webdav" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, gridscaleHTTP)

lazy val jzlib = OsgiProject(dir, "com.jcraft.jzlib", imports = Seq("*")) settings (
  libraryDependencies += "com.jcraft" % "jzlib" % "1.1.3",
  version := "1.1.3"
) settings(settings: _*)
