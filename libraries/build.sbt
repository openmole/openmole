import com.typesafe.sbt.osgi.OsgiKeys._
import org.openmole.buildsystem._

def dir = file("bundles")

def settings = Seq(
  resolvers += DefaultMavenRepository,
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.sonatypeRepo("releases"),
  resolvers += Resolver.sonatypeRepo("staging"),
  scalaVersion in Global := "2.12.10",
  scalacOptions ++= Seq("-deprecation"),
  publishArtifact in (packageDoc in publishLocal) := false,
  publishArtifact in (packageSrc in publishLocal) := false,
  organization := "org.openmole.library",
  isSnapshot := true
)


lazy val scalatraVersion = "2.7.0-RC1"
lazy val jettyVersion = "9.4.24.v20191120"
//lazy val jettyVersion = "9.3.25.v20180904"

lazy val scalatra = OsgiProject(dir, "org.scalatra",
  exports = Seq("org.scalatra.*, org.fusesource.*", "grizzled.*", "org.eclipse.jetty.*", "javax.*"),
  privatePackages = Seq("!scala.*", "!org.slf4j.*", "**"),
  imports = Seq("scala.*", "org.slf4j.*"),
  global = true) settings(
  libraryDependencies += "org.scalatra" %% "scalatra" % scalatraVersion,
  libraryDependencies += "org.scalatra" %% "scalatra-auth" % scalatraVersion,
  libraryDependencies += "org.eclipse.jetty" % "jetty-webapp" % jettyVersion,
  libraryDependencies += "org.eclipse.jetty" % "jetty-server" % jettyVersion,
  version := scalatraVersion) settings(settings: _*)

lazy val json4s = OsgiProject(dir, "org.json4s",
  exports = Seq("org.json4s.*"),
  privatePackages = Seq("!scala.*", "!org.slf4j.*", "!com.thoughtworks.paranamer.*", "*"),
  imports = Seq("scala.*", "org.slf4j.*", "com.thoughtworks.paranamer.*", "")) settings (
  libraryDependencies +=  "org.json4s" %% "json4s-jackson" % "3.6.7",
  version := "3.6.7") settings(settings: _*)

lazy val shapelessVersion = "2.3.3"
lazy val shapeless =  OsgiProject(dir, "com.chuusai.shapeless", exports = Seq("shapeless.*")) settings (
  libraryDependencies += "com.chuusai" %% "shapeless" % shapelessVersion,
  version := shapelessVersion
) settings(settings: _*)

lazy val circeVersion = "0.12.3"
lazy val circe = OsgiProject(dir, "io.circe",
  exports = Seq("io.circe.*", "!cats.*", "!scala.*", "!shapeless.*"),
  privatePackages = Seq("org.typelevel.jawn.*"),
  imports = Seq("scala.*", "cats.*", "shapeless.*")) settings (
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    "io.circe" %% "circe-parser"
  ).map(_ % circeVersion),
  version := circeVersion) settings(settings: _*) dependsOn(shapeless)

lazy val logback = OsgiProject(dir, "ch.qos.logback", exports = Seq("ch.qos.logback.*", "org.slf4j.impl"), dynamicImports = Seq("*")) settings
  (libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.2.3", version := "1.2.3") settings(settings: _*)

lazy val h2Version = "1.4.200"
lazy val h2 = OsgiProject(dir, "org.h2", dynamicImports = Seq("*"), privatePackages = Seq("META-INF.*")) settings
  (libraryDependencies += "com.h2database" % "h2" % h2Version, version := h2Version) settings(settings: _*)

lazy val bonecp = OsgiProject(dir, "com.jolbox.bonecp", dynamicImports = Seq("*")) settings
  (libraryDependencies += "com.jolbox" % "bonecp" % "0.8.0.RELEASE", version := "0.8.0.RELEASE") settings(settings: _*)

lazy val slickVersion = "3.3.2"
lazy val slick = OsgiProject(dir,"com.typesafe.slick", exports = Seq("slick.*"), privatePackages = Seq("org.reactivestreams.*")) settings
  (libraryDependencies += "com.typesafe.slick" %% "slick" % slickVersion, version := slickVersion) settings(settings: _*)

lazy val slf4j = OsgiProject(dir,"org.slf4j") settings(
  libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.30",
  version := "1.7.30") settings(settings: _*)

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
    "!com.sun.xml.*",
    "!com.ibm.xml.*",
    "!javax.*",
    "*"),
  privatePackages = Seq("!scala.*", "META-INF.services.*", "*")) settings(
  libraryDependencies ++= Seq("com.thoughtworks.xstream" % "xstream" % "1.4.11.1", "net.sf.kxml" % "kxml2" % "2.3.0"),
  version := "1.4.11.1") settings(settings: _*)

lazy val scalaLang = OsgiProject(
  dir,
  "org.scala-lang.scala-library",
  global = true,
  exports = Seq("akka.*", "com.typesafe.*", "scala.*", "scalax.*", "jline.*"),
  privatePackages = Seq("*", "META-INF.native.**"), imports = Seq("!org.apache.tools.ant.*", "!sun.misc.*" ,"*")) settings
  (libraryDependencies ++= {
    Seq(
      "org.scala-lang.modules" %% "scala-xml" % "1.2.0",
      "org.scala-lang" % "scala-library" % scalaVersion.value,
      "org.scala-lang" % "scala-reflect" % scalaVersion.value,
      "org.scala-lang" % "scalap" % scalaVersion.value,
      "jline" % "jline" % "2.12.1",
      "org.scala-stm" %% "scala-stm" % "0.9.1",
      "com.typesafe" % "config" % "1.2.1",
      "org.scalameta" %% "scalameta" % "4.3.0",
      "org.scala-lang" % "scala-compiler" % scalaVersion.value
    )
  }, version := scalaVersion.value) settings(settings: _*)

lazy val jasyptVersion = "1.9.3"
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

lazy val netLogo6Version = "6.1.1"

lazy val netlogo6 = OsgiProject(
  dir,
  "ccl.northwestern.edu.netlogo6",
  exports = Seq("org.nlogo.*"),
  privatePackages = Seq("**"),
  imports = Seq("empty;resolution:=optional")) settings (
  //resolvers += Resolver.bintrayRepo("netlogo", "NetLogo-JVM"),
  libraryDependencies ++= Seq(
    "org.nlogo" % "netlogo" % netLogo6Version % "provided" from s"https://dl.bintray.com/netlogo/NetLogo-JVM/org/nlogo/netlogo/$netLogo6Version/netlogo-$netLogo6Version.jar",
    "org.scala-lang" % "scala-library" % "2.12.8" % "provided",
    //"org.scala-lang" % "scala-reflect" % "2.12.8" % "provided",
    //"org.scala-lang" % "scala-compiler" % "2.12.8" % "provided",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.0.5" % "provided",
    "commons-codec" % "commons-codec" % "1.10" % "provided",
    "org.ow2.asm" % "asm-all" % "5.0.4" % "provided",
    "org.picocontainer" % "picocontainer" % "2.13.6" % "provided",
    "org.parboiled" %% "parboiled" % "2.1.3" % "provided",
    "com.typesafe" % "config" % "1.3.1" % "provided",
    "net.lingala.zip4j" % "zip4j" % "1.3.2" % "provided"
  ), version := netLogo6Version, scalaVersion := "2.12.8", crossPaths := false) settings(settings: _*) 

lazy val scalaTagsVersion = "0.8.3"
lazy val scalaRxVersion = "0.4.0"
lazy val scalaDomVersion = "0.9.3"
//lazy val scalaUpickleVersion = "0.9.6"
lazy val scalaBoopickleVersion = "1.3.1"
lazy val scalaAutowireVersion = "0.2.6"
lazy val scalajsVersion = "0.6.31"

lazy val rx = OsgiProject(dir, "rx", exports = Seq("rx.*")) settings(
  libraryDependencies ++= Seq("com.lihaoyi" %% "scalarx" % scalaRxVersion),
  version := scalaRxVersion) settings(settings: _*)

lazy val scalajsTools = OsgiProject(dir, "scalajs-tools", exports = Seq("scala.scalajs.*", "org.scalajs.core.tools.*", "org.scalajs.core.ir.*", "com.google.javascript.*", "com.google.common.*", "rhino_ast.java.com.google.javascript.rhino.*", "com.google.gson.*", "com.google.debugging.sourcemap.*", "org.json.*", "java7compat.nio.charset.*", "com.google.protobuf.*")) settings(
  libraryDependencies += "org.scala-js" %% "scalajs-tools" % scalajsVersion, version := scalajsVersion) settings(settings: _*)

lazy val scalaJS = OsgiProject(dir, "scalajs", exports = Seq("scala.scalajs.*"), imports = Seq("*")) settings (
  libraryDependencies += "org.scala-js" %% "scalajs-library" % scalajsVersion,
  version := scalajsVersion
  ) settings(settings: _*)

lazy val scalaTags = OsgiProject(dir, "com.scalatags", exports = Seq("scalatags.*"), privatePackages = Seq("geny.*")) settings(
  libraryDependencies ++= Seq("com.lihaoyi" %% "scalatags" % scalaTagsVersion),
  version := scalaTagsVersion) settings(settings: _*)

lazy val scalatexSite =
  OsgiProject(dir, "com.lihaoyi.scalatex-site", exports = Seq("scalatex.*", "ammonite.*", "fastparse.*"), privatePackages = Seq("META-INF.**", "pprint.*", "scalaj.*", "scalaparse.*", "geny.*"), imports = Seq("*")) settings (
    libraryDependencies += "com.lihaoyi" %% "scalatex-site" % "0.3.12",
    version := "0.3.12") settings(settings: _*)

/*lazy val upickle = OsgiProject(dir, "upickle", exports = Seq("upickle.*", "jawn.*", "derive.*"), imports = Seq("*")) settings(
  libraryDependencies ++= Seq("com.lihaoyi" %% "upickle" % scalaUpickleVersion),
  version := scalaUpickleVersion) settings(settings: _*) dependsOn(sourceCode)*/ 

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

lazy val catsVersion = "2.1.0"
lazy val cats =
  OsgiProject(dir, "cats") settings (
    libraryDependencies += "org.typelevel" %% "cats-core" % catsVersion,
    libraryDependencies += "org.typelevel" %% "cats-free" % catsVersion,
    version := catsVersion
  ) settings(settings: _*)

lazy val squantsVersion = "1.6.0"

lazy val squants =
  OsgiProject(dir, "squants") settings (
    libraryDependencies += "org.typelevel" %% "squants" % squantsVersion,
    version := squantsVersion
  ) settings(settings: _*)


lazy val mgoVersion = "3.42"

lazy val mgo = OsgiProject(dir, "mgo", exports = Seq("mgo.*", "freestyle.*"), imports = Seq("!better.*", "!javax.xml.*", "!scala.meta.*", "!sun.misc.*", "*"), privatePackages = Seq("!scala.*", "!monocle.*", "!org.apache.commons.math3.*", "!cats.*", "!squants.*", "!scalaz.*", "*")) settings(
  libraryDependencies += "org.openmole" %% "mgo" % mgoVersion,
  version := mgoVersion) dependsOn(monocle, math, cats, squants) settings(settings: _*)


lazy val containerVersion = "1.5"

lazy val container = OsgiProject(dir, "container", exports = Seq("container.*"), imports = Seq("!better.*", "!javax.xml.*", "!scala.meta.*", "!sun.misc.*", "!com.github.luben.*", "!org.apache.avalon.*", "!org.apache.log.*", "!org.brotli.dec.*", "!javax.*","*"), privatePackages = Seq("!scala.*", "!monocle.*", "!org.apache.commons.math3.*", "!cats.*", "!squants.*", "!scalaz.*", "!io.circe.*", "!shapeless.*", "*")) settings(
  libraryDependencies += "org.openmole" %% "container" % containerVersion,
  //libraryDependencies += "com.github.luben" % "zstd-jni" % "1.4.3-1",
  version := containerVersion) dependsOn(cats, squants, circe, shapeless) settings(settings: _*)

lazy val spatialdataVersion = "0.2"

lazy val spatialdata = OsgiProject(dir, "org.openmole.spatialdata",
  exports = Seq("org.openmole.spatialdata.*"),
  privatePackages = Seq("!scala.*","!org.apache.commons.math3.*","*")
) settings(
  resolvers += "osgeo" at  "https://download.osgeo.org/webdav/geotools/",
  libraryDependencies += "org.openmole.library" %% "spatialdata" % spatialdataVersion,
  version := spatialdataVersion//,
  //embeddedJars := (Keys.externalDependencyClasspath in Compile).value map (_.data) filter (f=> (f.getName startsWith "gt-")) // embed geotools jars
) settings(settings: _*)

lazy val opencsv = OsgiProject(dir, "au.com.bytecode.opencsv") settings(
  libraryDependencies += "net.sf.opencsv" % "opencsv" % "2.3",
  version := "2.3"
  ) settings(settings: _*)

lazy val arm = OsgiProject(dir, "com.jsuereth.scala-arm") settings(
  libraryDependencies += "com.jsuereth" %% "scala-arm" % "2.0",
  version := "2.0",
  exportPackage := Seq("resource.*")) settings(settings: _*)

lazy val scalajHttp = OsgiProject(dir, "org.scalaj.scalaj-http") settings(
  libraryDependencies += "org.scalaj" %% "scalaj-http" % "2.4.2",
  version := "2.4.2",
  exportPackage := Seq("scalaj.http.*")
  ) settings(settings: _*)

lazy val scopt = OsgiProject(dir, "com.github.scopt", exports = Seq("scopt.*")) settings(
  libraryDependencies += "com.github.scopt" %% "scopt" % "3.7.1",
  version := "3.7.1"
  ) settings(settings: _*)

lazy val async =
  OsgiProject(dir, "scala-async") settings (
    libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.10.0",
    version := "0.10.0",
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
  libraryDependencies += "org.apache.commons" % "commons-lang3" % "3.9", version := "3.9") settings(settings: _*)

lazy val ant = OsgiProject(dir, "org.apache.ant") settings
  (libraryDependencies += "org.apache.ant" % "ant" % "1.10.7", version := "1.10.7") settings(settings: _*)

lazy val codec = OsgiProject(dir, "org.apache.commons.codec") settings
  (libraryDependencies += "commons-codec" % "commons-codec" % "1.14", version := "1.14") settings(settings: _*)

lazy val collections = OsgiProject(dir, "org.apache.commons.collections", exports = Seq("org.apache.commons.collections4.*")) settings
  (libraryDependencies += "org.apache.commons" % "commons-collections4" % "4.4", version := "4.4") settings(settings: _*)

lazy val jgit = OsgiProject(dir, "org.eclipse.jgit", privatePackages = Seq("!scala.*", "!org.slf4j.*", "*"))  settings (
  libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "5.6.0.201912101111-r", version := "4.6.0" ) settings(settings: _*)

lazy val txtmark = OsgiProject(dir, "com.github.rjeschke.txtmark", privatePackages = Seq("!scala.*", "!org.slf4j.*", "*"))  settings (
  libraryDependencies += "com.github.rjeschke" % "txtmark" % "0.13", version := "0.13" ) settings(settings: _*)

lazy val clapperVersion = "1.5.1"
lazy val clapper = OsgiProject(dir, "org.clapper", exports = Seq("!scala.*","!grizzled.*","!jline.*","!org.fusesource.*","!org.slf4j.*","*")) settings (
  libraryDependencies += "org.clapper" %% "classutil" % clapperVersion, version := clapperVersion) settings(settings: _*)

val scalazVersion = "7.2.30"
lazy val scalaz = OsgiProject(dir, "org.scalaz", exports = Seq("!scala.*","*")) settings (
  libraryDependencies += "org.scalaz" %% "scalaz-core" % scalazVersion, version := scalazVersion) settings(settings: _*)

val monocleVersion = "2.0.0"
lazy val monocle = OsgiProject(dir, "monocle",
  privatePackages = Seq("!scala.*", "!scalaz.*", "!shapeless.*", "!cats.*", "*"),
  imports = Seq("scala.*", "shapeless.*", "scalaz.*", "cats.*")) settings(
  libraryDependencies ++= Seq (
    "com.github.julien-truffaut" %% "monocle-core",
    "com.github.julien-truffaut" %% "monocle-generic",
    "com.github.julien-truffaut" %% "monocle-macro"
  ).map(_ % monocleVersion),
  version := monocleVersion) settings(settings: _*) dependsOn(shapeless, scalaz, cats)

val asmVersion = "7.2"

lazy val asm = OsgiProject(dir, "org.objectweb.asm") settings (
  libraryDependencies += "org.ow2.asm" % "asm" % asmVersion,
  version := asmVersion) settings(settings: _*)

lazy val config = OsgiProject(dir, "org.apache.commons.configuration2",
  privatePackages = Seq("!scala.*", "!org.apache.commons.logging.*","*"),
  imports = Seq("org.apache.commons.logging.*")) settings (
  libraryDependencies += "org.apache.commons" % "commons-configuration2" % "2.6",
  libraryDependencies += "commons-beanutils" % "commons-beanutils" % "1.9.4",
  version := "2.6") settings(settings: _*) dependsOn (logging)

def sourceCodeVersion = "0.1.9"
lazy val sourceCode = OsgiProject(dir, "sourcecode") settings (
  libraryDependencies += "com.lihaoyi" %% "sourcecode" % sourceCodeVersion,
  version := sourceCodeVersion
) settings(settings: _*)


def gridscaleVersion = "2.28"
lazy val gridscale = OsgiProject(dir, "gridscale", imports = Seq("*"), exports = Seq("gridscale.*", "enumeratum.*")) settings (
  libraryDependencies += "org.openmole.gridscale" %% "gridscale" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) 

lazy val gridscaleLocal = OsgiProject(dir, "gridscale.local", imports = Seq("*")) settings (
  libraryDependencies += "org.openmole.gridscale" %% "local" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale)

lazy val gridscaleHTTP = OsgiProject(dir, "gridscale.http", imports = Seq("*"), privatePackages = Seq("org.htmlparser.*")) settings (
  libraryDependencies += "org.openmole.gridscale" %% "http" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, codec)

lazy val gridscaleSSH = OsgiProject(dir, "gridscale.ssh", imports = Seq("*")) settings (
  libraryDependencies += "org.openmole.gridscale" %% "ssh" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(sshj) dependsOn(gridscale)

lazy val sshj = OsgiProject(dir, "com.hierynomus.sshj", imports = Seq("*"), exports = Seq("com.hierynomus.*", "net.schmizz.*"), privatePackages = Seq("!scala.*", "!org.bouncycastle.*", "!org.slf4j.*", "**"), dynamicImports = Seq("org.bouncycastle.*")) settings (
  libraryDependencies += "com.hierynomus" % "sshj" % "0.27.0",
  version := "0.27.0"
) settings(settings: _*)

lazy val gridscaleCluster = OsgiProject(dir, "gridscale.cluster", imports = Seq("*")) settings (
  libraryDependencies += "org.openmole.gridscale" %% "cluster" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscaleSSH)

lazy val gridscaleOAR = OsgiProject(dir, "gridscale.oar", imports = Seq("*")) settings (
  libraryDependencies += "org.openmole.gridscale" %% "oar" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, gridscaleCluster)

lazy val gridscalePBS = OsgiProject(dir, "gridscale.pbs", imports = Seq("*")) settings (
  libraryDependencies += "org.openmole.gridscale" %% "pbs" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, gridscaleCluster)

lazy val gridscaleSGE = OsgiProject(dir, "gridscale.sge", imports = Seq("*")) settings (
  libraryDependencies += "org.openmole.gridscale" %% "sge" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, gridscaleCluster)

lazy val gridscaleCondor = OsgiProject(dir, "gridscale.condor", imports = Seq("*")) settings (
  libraryDependencies += "org.openmole.gridscale" %% "condor" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, gridscaleCluster)

lazy val gridscaleSLURM = OsgiProject(dir, "gridscale.slurm", imports = Seq("*")) settings (
  libraryDependencies += "org.openmole.gridscale" %% "slurm" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, gridscaleCluster)

lazy val gridscaleEGI = OsgiProject(dir, "gridscale.egi", imports = Seq("*")) settings (
  libraryDependencies += "org.openmole.gridscale" %% "egi" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, gridscaleHTTP)

lazy val gridscaleDIRAC = OsgiProject(dir, "gridscale.dirac", imports = Seq("*"), privatePackages = Seq("gridscale.dirac.*", "org.apache.commons.compress.*", "org.brotli.*", "org.tukaani.*", "com.github.luben.*")) settings (
  libraryDependencies += "org.openmole.gridscale" %% "dirac" % gridscaleVersion,
  libraryDependencies += "org.brotli" % "dec" % "0.1.2",
  libraryDependencies += "org.tukaani" % "xz" % "1.8",
  libraryDependencies += "com.github.luben" % "zstd-jni" % "1.4.4-3",
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, gridscaleHTTP)

lazy val gridscaleWebDAV = OsgiProject(dir, "gridscale.webdav", imports = Seq("*")) settings (
  libraryDependencies += "org.openmole.gridscale" %% "webdav" % gridscaleVersion,
  version := gridscaleVersion
) settings(settings: _*) dependsOn(gridscale, gridscaleHTTP)

lazy val xzJava = OsgiProject(dir, "xzjava", imports = Seq("*"), exports = Seq("org.tukaani.*")) settings (
  libraryDependencies +=  "org.tukaani" % "xz" % "1.8",
  version := "1.8"
) settings(settings: _*)


