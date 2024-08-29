import com.typesafe.sbt.osgi.OsgiKeys._
import org.openmole.buildsystem._

import openmole.common._

def dir = file("bundles")

def settings = Seq(
  resolvers += DefaultMavenRepository,
  resolvers += Resolver.sonatypeRepo("snapshots"),
  resolvers += Resolver.sonatypeRepo("releases"),
  resolvers += Resolver.sonatypeRepo("staging"),
  publishLocal / packageDoc / publishArtifact := false,
  publishLocal / packageSrc / publishArtifact := false,
  organization := "org.openmole.library",
  isSnapshot := true,
  scalaVersion := scala3VersionValue
)

lazy val json4s = OsgiProject(dir, "org.json4s",
  exports = Seq("org.json4s.*"),
  privatePackages = Seq("!scala.*", "!org.slf4j.*", "!com.fasterxml.jackson.*", "*"),
  imports = Seq("scala.*", "org.slf4j.*", "com.fasterxml.jackson.*")) settings (
  settings,
  libraryDependencies +=  "org.json4s" %% "json4s-jackson" % json4sVersion,
  version := json4sVersion) dependsOn(slf4j, jackson)

lazy val jackson = OsgiProject(dir, "com.fasterxml.jackson",
  privatePackages = Seq("!scala.*", "!org.slf4j.*", "*"),
  imports = Seq("scala.*", "org.slf4j.*")) settings (
  settings,
  libraryDependencies += "com.fasterxml.jackson.module" %% "jackson-module-scala" % jacksonVersion,
  version := jacksonVersion)

lazy val shapeless =  OsgiProject(dir, "org.typelevel.shapeless", exports = Seq("shapeless3.*")) settings (
  settings,
  libraryDependencies += "org.typelevel" %% "shapeless3-deriving" % shapelessVersion,
  libraryDependencies += "org.typelevel" %% "shapeless3-typeable" % shapelessVersion,

  libraryDependencies += "org.typelevel" %% sjs("shapeless3-deriving") % shapelessVersion,
  libraryDependencies += "org.typelevel" %% sjs("shapeless3-typeable") % shapelessVersion,

  version := shapelessVersion
)



lazy val circe = OsgiProject(dir, "io.circe",
  exports = Seq("io.circe.*", "org.latestbit.*", "!cats.*", "!scala.*", "!shapeless3.*"),
  privatePackages = Seq("org.typelevel.jawn.*", "org.yaml.*"),
  imports = Seq("scala.*", "cats.*", "shapeless3.*"))  settings (
  settings,
  libraryDependencies ++= Seq(
    "io.circe" %% "circe-core",
    "io.circe" %% "circe-generic",
    //"io.circe" %% "circe-extras",
    "io.circe" %% "circe-parser",
    "io.circe" %% sjs("circe-core"),
    "io.circe" %% sjs("circe-generic"),
    "io.circe" %% sjs("circe-parser")).map(_ % circeVersion),
  libraryDependencies += "io.circe" %% "circe-yaml" % circeYamlVersion,
  libraryDependencies += "org.latestbit" %% "circe-tagged-adt-codec" % "0.11.0",
  libraryDependencies += "org.latestbit" %% sjs("circe-tagged-adt-codec") % "0.11.0",
  version := circeVersion) //dependsOn(shapeless)

lazy val logback = OsgiProject(dir, "ch.qos.logback", exports = Seq("ch.qos.logback.*", "org.slf4j.impl"), dynamicImports = Seq("*")) settings(
  settings,
  libraryDependencies += "ch.qos.logback" % "logback-classic" % logbackVersion, version := logbackVersion)

lazy val h2 = OsgiProject(dir, "org.h2", dynamicImports = Seq("*"), privatePackages = Seq("META-INF.*")) settings(
  settings,
  libraryDependencies += "com.h2database" % "h2" % h2Version, version := h2Version)

/*lazy val bonecp = OsgiProject(dir, "com.jolbox.bonecp", dynamicImports = Seq("*")) settings
  (libraryDependencies += "com.jolbox" % "bonecp" % "0.8.0.RELEASE", version := "0.8.0.RELEASE") settings(settings: _*)*/

lazy val slf4j = OsgiProject(dir,"org.slf4j", privatePackages = Seq("!scala.*", "META-INF.services.*", "*")) settings(
  settings,
  libraryDependencies += "org.slf4j" % "slf4j-api" % slf4jVersion,
  libraryDependencies += "org.slf4j" % "slf4j-jdk14" % slf4jVersion,
  version := slf4jVersion)

lazy val xstream = OsgiProject(
  dir,
  "com.thoughtworks.xstream",
  imports = Seq(
    "!com.bea.xml.stream.*",
    "!com.ctc.wstx.stax.*",
    "!net.sf.cglib.*",
    "!nu.xom.*",
    "!org.dom4j.*",
    "!org.jdom.*",
    "!org.jdom2.*",
    "!org.w3c.*",
    "!org.xml.sax.*",
    "!sun.misc.*",
    "!org.joda.time.*",
    "!com.sun.xml.*",
    "!com.ibm.xml.*",
    "!org.xmlpull.*",
    "!javax.*",
    "*"),
  privatePackages = Seq("!scala.*", "META-INF.services.*", "*")) settings(
  settings,
  libraryDependencies ++= Seq("com.thoughtworks.xstream" % "xstream" % xstreamVersion, "net.sf.kxml" % "kxml2" % "2.3.0", "org.codehaus.jettison" % "jettison" % "1.4.1"),
  version := xstreamVersion)


lazy val scalaLang = OsgiProject(
  dir,
  "org.scala-lang.scala-library",
  global = true,
  exports = Seq("com.typesafe.*", "scala.*", "dotty.*", "scalax.*" /*"jline.*"*/),
  privatePackages = Seq("!org.jline.*", "**", "META-INF.native.**"),
  imports = Seq("org.jline.*" /*"!org.apache.sshd.*", "!org.mozilla.*", "!org.apache.tools.ant.*", "!sun.misc.*", "!javax.annotation.*", "!scala.*", "*"*/)) settings (
  settings,
  libraryDependencies ++= Seq(
    "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
    "org.scala-lang.modules" %% "scala-java8-compat" % "1.0.2",
    //"org.scala-lang" % "scala-library" % scalaVersion.value,
    //"org.scala-lang" % "scala-reflect" % scalaVersion.value,
    //"org.scala-lang" % "scalap" % scalaVersion.value ,
    //"jline" % "jline" % "2.12.1",
    //"com.typesafe" % "config" % "1.2.1",
    "org.scala-lang" %% "scala3-tasty-inspector"% scalaVersion.value,
    "org.scala-lang" %% "scala3-library" % scalaVersion.value,
    "org.scala-lang" %% "scala3-compiler" % scalaVersion.value,
    //"org.scala-lang.modules" %% "scala-collection-compat" % "2.8.0",
    "org.scalameta" %% "scalameta" % scalaMetaVersion cross(CrossVersion.for3Use2_13) excludeAll(
      ExclusionRule(organization = "com.lihaoyi"),
      ExclusionRule(organization = "org.scala-lang.modules"),
      ExclusionRule(organization = "org.scala-lang")
    ),
    "org.scala-lang" %% sjs("scala3-library") % scalaVersion.value,
    //"org.scala-lang.modules" %% sjs("scala-collection-compat") % "2.1.6"
    ),
//  excludeDependencies ++= Seq(
//    ExclusionRule(organization = "com.lihaoyi"),
//    ExclusionRule("org.scala-lang.modules", "scala-collection-compat_2.13"),
//    ExclusionRule(organization = "org.scala-lang", "scala-compiler")
//  ),
  version := scalaVersion.value
) dependsOn(jline)


lazy val jline = OsgiProject(dir, "org.jline.jline", exports = Seq("org.jline.*"), privatePackages = Seq("!scala.*", "META-INF.**", "**")) settings (
  settings,
  libraryDependencies += "org.jline" % "jline" % jlineVersion, 
  libraryDependencies += "org.jline" % "jline-reader" % jlineVersion, 
  libraryDependencies += "org.jline" % "jline-builtins" % jlineVersion, 
  libraryDependencies += "org.jline" % "jline-terminal" % jlineVersion, 
  libraryDependencies += "org.jline" % "jline-terminal-jna" % jlineVersion,
  libraryDependencies += "org.jline" % "jline-style" % jlineVersion,

  version := jlineVersion)


/*lazy val scalaMeta =
  OsgiProject(
    dir,
    "org.scalameta",
    exports = Seq("scala.meta.*"),
    privatePackages = Seq("!scala.*", "scala.meta.*", "*") ,
    imports = Seq("scala.*")
  ) settings(
    libraryDependencies += "org.scalameta" %% "scalameta" % scalaMetaVersion cross(CrossVersion.for3Use2_13),
    version := scalaMetaVersion
  ) settings(settings: _*) settings(settings: _*)*/


lazy val scalaSTM =
  OsgiProject(
    dir,
    "org.scala-stm",
    exports = Seq("scala.concurrent.stm.*"),
    privatePackages = Seq("scala.concurrent.stm.*", "!scala.*", "*") ,
    imports = Seq("!scala.concurrent.stm.*", "scala.*")
  ) settings(
    settings,
    libraryDependencies += "org.scala-stm" %% "scala-stm" % scalaSTMVersion,
    version := scalaSTMVersion
  )

lazy val scalaXML = OsgiProject(
  dir,
  "org.scala-lang.modules.xml",
  exports = Seq("scala.xml.*"),
  privatePackages = Seq("scala.xml.*", "!scala.*", "*"),
  imports = Seq("scala.*")
) settings(
  settings,
  libraryDependencies += "org.scala-lang.modules" %% "scala-xml" % scalaXMLVersion,
  version := scalaXMLVersion
)

lazy val jasypt = OsgiProject(dir, "org.jasypt.encryption", exports = Seq("org.jasypt.*")) settings(
  settings,
  libraryDependencies += "org.jasypt" % "jasypt" % jasyptVersion,
  version := jasyptVersion)


lazy val netlogo5 = OsgiProject(
  dir,
  "ccl.northwestern.edu.netlogo5",
  exports = Seq("org.nlogo.*"),
  privatePackages = Seq("**"),
  imports = Seq("!*")) settings(settings) settings(
  libraryDependencies ++= Seq(
    "ccl.northwestern.edu" % "netlogo" % netLogo5Version % "provided" from s"https://github.com/NetLogo/NetLogo/releases/download/$netLogo5Version/NetLogo.jar",
    "org.scala-lang" % "scala-library" % "2.9.2" % "provided",
    "asm" % "asm-all" % "3.3.1" % "provided",
    "org.picocontainer" % "picocontainer" % "2.13.6" % "provided"),
  scalaVersion := "2.9.2",
  version := netLogo5Version,
  crossPaths := false)

lazy val netlogo6 = OsgiProject(
  dir,
  "ccl.northwestern.edu.netlogo6",
  exports = Seq("org.nlogo.*"),
  privatePackages = Seq("**"),
  imports = Seq("empty;resolution:=optional")) settings(settings) settings (
  //resolvers += Resolver.bintrayRepo("netlogo", "NetLogo-JVM"),
  resolvers += "Netlogo" at "https://mvnrepository.com/artifact/org.nlogo/netlogo",
  scalaVersion := "2.12.8",
  resolvers += "netlogo" at "https://dl.cloudsmith.io/public/netlogo/netlogo/maven/",
  libraryDependencies += "org.nlogo" % "netlogo" % netLogo6Version % "provided" exclude("org.jogamp.jogl", "jogl-all") exclude("org.jogamp.gluegen", "gluegen-rt"),
  version := netLogo6Version,
  crossPaths := false)

/*lazy val scalajsTools = OsgiProject(dir, "scalajs-tools", exports = Seq("scala.scalajs.*", "org.scalajs.core.tools.*", "org.scalajs.core.ir.*", "com.google.javascript.*", "com.google.common.*", "rhino_ast.java.com.google.javascript.rhino.*", "com.google.gson.*", "com.google.debugging.sourcemap.*", "org.json.*", "java7compat.nio.charset.*", "com.google.protobuf.*")) settings(
  libraryDependencies += "org.scala-js" %% "scalajs-tools" % scalajsVersion, version := scalajsVersion) settings(settings: _*)*/

lazy val scalajsLinker = OsgiProject(dir, "scalajs-linker", exports = Seq("org.scalajs.linker.*", "org.scalajs.ir.*", "com.google.javascript.*", "com.google.common.*", "rhino_ast.java.com.google.javascript.rhino.*", "com.google.gson.*", "com.google.debugging.sourcemap.*", "org.json.*", "java7compat.nio.charset.*", "com.google.protobuf.*")) settings(
  settings,
  libraryDependencies += "org.scala-js" %% "scalajs-linker" % scalajsVersion cross CrossVersion.for3Use2_13,
////  libraryDependencies += "org.scala-js" %% "scalajs-logging" % scalajsVersion,
//    //"org.scala-js" %% "scalajs-linker-interface" % scalajsVersion),
    version := scalajsVersion)
//

lazy val scalajsLogging = OsgiProject(dir, "scalajs-logging", exports = Seq("org.scalajs.logging.*")) settings(
  settings,
  libraryDependencies += "org.scala-js" %% "scalajs-logging" % scalajsLoggingVersion cross CrossVersion.for3Use2_13,
  version := scalajsLoggingVersion)
  
lazy val scalaJS = OsgiProject(dir, "scalajs", exports = Seq("scala.scalajs.*"), imports = Seq("*"), privatePackages = Seq("org.scalajs.*")) settings (
  settings,
  libraryDependencies += "org.scala-js" %% "scalajs-library" % scalajsVersion cross CrossVersion.for3Use2_13,
  libraryDependencies += "org.scala-lang" %% sjs("scala3-library") % scala3VersionValue,
  version := scalajsVersion
  )

lazy val scalaTags = OsgiProject(dir, "com.scalatags", exports = Seq("scalatags.*"), privatePackages = Seq("geny.*")) settings(
  settings,
  libraryDependencies ++= Seq(
    "com.lihaoyi" %% "scalatags" % scalaTagsVersion,
    "com.lihaoyi" %% sjs("scalatags") % scalaTagsVersion,
  ),
  version := scalaTagsVersion)

//lazy val boopicklexAutowireVersion) settings(settings: _*)

//lazy val jsonSimple = OsgiProject(dir, "json-simple", exports = Seq("org.json.simple.*")) settings(
//  libraryDependencies += "com.googlecode.json-simple" % "json-simple" % jsonSimpleVersion, version := jsonSimpleVersion) settings(settings: _*)

//lazy val closureCompilerVersion = "v20130603"
//lazy val closureCompiler = OsgiProject(dir, "closure-compiler", exports = Seq("com.google.javascript.*")) settings(
//  libraryDependencies += "com.google.javascript" % "closure-compiler" % closureCompilerVersion, version := closureCompilerVersion) settings(settings: _*)

lazy val cats =
  OsgiProject(dir, "cats") settings (
    settings,
    libraryDependencies += "org.typelevel" %% "cats-core" % catsVersion,
    libraryDependencies += "org.typelevel" %% "cats-free" % catsVersion,
    libraryDependencies += "org.typelevel" %% "cats-parse" % catsParseVersion,
    libraryDependencies += "org.typelevel" %% "cats-effect" % catsEffectVersion,
    libraryDependencies += "org.typelevel" %% sjs("cats-core") % catsVersion,
    libraryDependencies += "org.typelevel" %% sjs("cats-free") % catsVersion,
    libraryDependencies += "org.typelevel" %% sjs("cats-parse") % catsParseVersion,
    libraryDependencies += "org.typelevel" %% sjs("cats-effect") % catsEffectVersion,
    version := catsVersion
  )

lazy val squants =
  OsgiProject(dir, "squants") settings (
    settings,
    libraryDependencies += "org.typelevel" %% "squants" % squantsVersion,
    libraryDependencies ~= { _.map(_.exclude("com.lihaoyi", "sourcecode")) },
    version := squantsVersion
  )

val noReflectScala2 = Seq("!scala.reflect.api", "!scala.reflect.macros", "!scala.reflect.macros.*")

lazy val mgo = OsgiProject(
  dir,
  "mgo",
  exports = Seq("mgo.*", "ppse.*"),
  imports = noReflectScala2 ++ Seq("!scala.collection.compat.*", "scala.*", "monocle.*", "cats.*", "squants.*", "!com.oracle.svm.*", "!*"), //Seq("!better.*", "!javax.xml.*", "!scala.meta.*", "!sun.misc.*", "*"),
  privatePackages = Seq("!scala.*", "!monocle.*", "!squants.*", "!cats.*", "*") /*Seq("!scala.*", "!monocle.*", "!org.apache.commons.math3.*", "!cats.*", "!squants.*", "!scalaz.*", "*")*/) settings(
  settings,
  libraryDependencies += "org.openmole" %% "mgo" % mgoVersion,
  excludeDependencies += ExclusionRule(organization = "org.typelevel", name = "cats-kernel_2.13"),
  version := mgoVersion) dependsOn(monocle, cats, squants)

lazy val container = OsgiProject(
  dir,
  "container",
  exports = Seq("container.*"),
  imports = noReflectScala2 ++ Seq( "scala.*", "squants.*", "monocle.*", "cats.*", "io.circe.*", "!com.oracle.svm.*", "!org.graalvm.*", "!*"),
  privatePackages = Seq("!scala.*", "!monocle.*", "!squants.*", "!cats.*", "!io.circe.*" ,"*")) settings(
  settings,
  libraryDependencies += "org.openmole" %% "container" % containerVersion,
  //libraryDependencies += "com.github.luben" % "zstd-jni" % "1.4.3-1",
  version := containerVersion) dependsOn(cats, squants, monocle, circe)

lazy val spatialdata = OsgiProject(dir, "org.openmole.spatialsampling",
  exports = Seq("org.openmole.spatialsampling.*"),
  privatePackages = Seq("!scala.*","!org.apache.commons.math3.*","*")
) settings(
  settings,
  //resolvers += "osgeo" at  "https://repo.osgeo.org/repository/release/",
  libraryDependencies += "org.openmole" %% "spatialsampling" % spatialsamplingVersion cross CrossVersion.for3Use2_13,
  version := spatialsamplingVersion)

lazy val opencsv = OsgiProject(dir, "au.com.bytecode.opencsv") settings(
  settings,
  libraryDependencies += "net.sf.opencsv" % "opencsv" % "2.3",
  version := "2.3")

lazy val scopt = OsgiProject(dir, "com.github.scopt", exports = Seq("scopt.*")) settings(
  settings,
  libraryDependencies += "com.github.scopt" %% "scopt" % scoptVersion,
  version := scoptVersion
  )

lazy val math = OsgiProject(dir, "org.apache.commons.math", exports = Seq("org.apache.commons.math3.*"), privatePackages = Seq("assets.*")) settings(
  settings,
  libraryDependencies += "org.apache.commons" % "commons-math3" % mathVersion, version := mathVersion)

lazy val exec = OsgiProject(dir, "org.apache.commons.exec") settings(
  settings,
  libraryDependencies += "org.apache.commons" % "commons-exec" % execVersion, version := execVersion)

lazy val log4j = OsgiProject(dir, "org.apache.log4j") settings(
  settings,
  libraryDependencies += "log4j" % "log4j" % "1.2.17", version := "1.2.17")

lazy val logging = OsgiProject(dir, "org.apache.commons.logging") settings(
  settings,
  libraryDependencies += "commons-logging" % "commons-logging" % "1.2", version := "1.2")

lazy val lang3 = OsgiProject(dir, "org.apache.commons.lang3") settings(
  libraryDependencies += "org.apache.commons" % "commons-lang3" % lang3Version, version := lang3Version) settings(settings: _*) settings(settings: _*)

//lazy val ant = OsgiProject(dir, "org.apache.ant") settings
//  (libraryDependencies += "org.apache.ant" % "ant" % "1.10.7", version := "1.10.7") settings(settings: _*)

lazy val codec = OsgiProject(dir, "org.apache.commons.codec") settings(
  settings,
  libraryDependencies += "commons-codec" % "commons-codec" % codecVersion, version := codecVersion)

lazy val collections = OsgiProject(dir, "org.apache.commons.collections", exports = Seq("org.apache.commons.collections4.*")) settings(
  settings,
  libraryDependencies += "org.apache.commons" % "commons-collections4" % "4.4", version := "4.4")

//lazy val jgit = OsgiProject(dir, "org.eclipse.jgit", privatePackages = Seq("!scala.*", "!org.slf4j.*", "*"))  settings (
//  scala2Settings,
//  libraryDependencies += "org.eclipse.jgit" % "org.eclipse.jgit" % "5.6.0.201912101111-r", version := "4.6.0" )

lazy val txtmark = OsgiProject(dir, "com.github.rjeschke.txtmark", privatePackages = Seq("!scala.*", "!org.slf4j.*", "*"))  settings (
  settings,
  libraryDependencies += "com.github.rjeschke" % "txtmark" % txtmarkVersion, version := txtmarkVersion)

//lazy val scalaz = OsgiProject(dir, "org.scalaz", exports = Seq("!scala.*","*")) settings (
//  settings,
//  libraryDependencies += "org.scalaz" %% "scalaz-core" % scalazVersion cross CrossVersion.for3Use2_13, version := scalazVersion)

lazy val monocle = OsgiProject(dir, "monocle",
  privatePackages = Seq("!scala.*", "!cats.*", "*"),
  imports = Seq("scala.*", "cats.*")) settings(
  settings,
  libraryDependencies ++= Seq (
    "dev.optics" %% "monocle-core",
    //"dev.optics" %% "monocle-generic",
    "dev.optics" %% "monocle-macro"
  ).map(_ % monocleVersion cross CrossVersion.for2_13Use3),
  version := monocleVersion) dependsOn(cats)

lazy val asm = OsgiProject(dir, "org.objectweb.asm") settings (
  settings,
  libraryDependencies += "org.ow2.asm" % "asm" % asmVersion,
  version := asmVersion)

lazy val config = OsgiProject(dir, "org.apache.commons.configuration2",
  privatePackages = Seq("!scala.*", "!org.apache.commons.logging.*","*"),
  imports = Seq("org.apache.commons.logging.*")) settings (
  settings,
  libraryDependencies += "org.apache.commons" % "commons-configuration2" % configuration2Version,
  libraryDependencies += "commons-beanutils" % "commons-beanutils" % "1.9.4",
  version := configuration2Version) dependsOn (logging)

lazy val compress = OsgiProject(dir, "org.apache.commons.compress",
  privatePackages = Seq("!scala.*", "!org.apache.commons.logging.*","*"),
  imports = Seq("org.apache.commons.logging.*")) settings (
  settings,
  libraryDependencies += "org.apache.commons" % "commons-compress" % compressVersion,
  version := compressVersion) dependsOn logging

lazy val sourceCode = OsgiProject(dir, "sourcecode") settings (
  settings,
  libraryDependencies += "com.lihaoyi" %% "sourcecode" % sourcecodeVersion,
  libraryDependencies += "com.lihaoyi" %% sjs("sourcecode") % sourcecodeVersion,
  version := sourcecodeVersion
)

lazy val gridscale = OsgiProject(dir, "gridscale", imports = Seq("*"), exports = Seq("gridscale.*", "enumeratum.*")) settings (
  settings,
  libraryDependencies += "org.openmole.gridscale" %% "gridscale" % gridscaleVersion,
  version := gridscaleVersion
)

lazy val gridscaleLocal = OsgiProject(dir, "gridscale.local", imports = Seq("*")) settings (
  settings,
  libraryDependencies += "org.openmole.gridscale" %% "local" % gridscaleVersion,
  version := gridscaleVersion
) dependsOn(gridscale)

lazy val gridscaleHTTP = OsgiProject(dir, "gridscale.http", imports = Seq("*"), privatePackages = Seq("org.htmlparser.*")) settings (
  settings,
  libraryDependencies += "org.openmole.gridscale" %% "http" % gridscaleVersion,
  version := gridscaleVersion
) dependsOn(gridscale, codec)

lazy val gridscaleSSH = OsgiProject(dir, "gridscale.ssh", imports = Seq("*")) settings (
  settings,
  libraryDependencies += "org.openmole.gridscale" %% "ssh" % gridscaleVersion,
  version := gridscaleVersion
) dependsOn(sshj) dependsOn(gridscale)

lazy val sshj = OsgiProject(dir, "com.hierynomus.sshj", imports = Seq("!sun.security.*", "*"), exports = Seq("com.hierynomus.*", "net.schmizz.*"), privatePackages = Seq("!scala.*", "!org.bouncycastle.*", "!org.slf4j.*", "**"), dynamicImports = Seq("org.bouncycastle.*")) settings (
  settings,
  libraryDependencies += "com.hierynomus" % "sshj" % sshjVersion,
  version := sshjVersion
) dependsOn(slf4j)

lazy val gridscaleCluster = OsgiProject(dir, "gridscale.cluster", imports = Seq("*")) settings (
  settings,
  libraryDependencies += "org.openmole.gridscale" %% "cluster" % gridscaleVersion,
  version := gridscaleVersion
) dependsOn(gridscaleSSH)

lazy val gridscaleOAR = OsgiProject(dir, "gridscale.oar", imports = Seq("*")) settings (
  settings,
  libraryDependencies += "org.openmole.gridscale" %% "oar" % gridscaleVersion,
  version := gridscaleVersion
) dependsOn(gridscale, gridscaleCluster)

lazy val gridscalePBS = OsgiProject(dir, "gridscale.pbs", imports = Seq("*")) settings (
  settings,
  libraryDependencies += "org.openmole.gridscale" %% "pbs" % gridscaleVersion,
  version := gridscaleVersion
) dependsOn(gridscale, gridscaleCluster)

lazy val gridscaleSGE = OsgiProject(dir, "gridscale.sge", imports = Seq("*")) settings (
  settings,
  libraryDependencies += "org.openmole.gridscale" %% "sge" % gridscaleVersion,
  version := gridscaleVersion
) dependsOn(gridscale, gridscaleCluster)

lazy val gridscaleCondor = OsgiProject(dir, "gridscale.condor", imports = Seq("*")) settings (
  settings,
  libraryDependencies += "org.openmole.gridscale" %% "condor" % gridscaleVersion,
  version := gridscaleVersion
) dependsOn(gridscale, gridscaleCluster)

lazy val gridscaleSLURM = OsgiProject(dir, "gridscale.slurm", imports = Seq("*")) settings (
  settings,
  libraryDependencies += "org.openmole.gridscale" %% "slurm" % gridscaleVersion,
  version := gridscaleVersion
) dependsOn(gridscale, gridscaleCluster)

lazy val gridscaleEGI = OsgiProject(dir, "gridscale.egi", imports = Seq("*")) settings (
  settings,
  libraryDependencies += "org.openmole.gridscale" %% "egi" % gridscaleVersion,
  version := gridscaleVersion
) dependsOn(gridscale, gridscaleHTTP)

lazy val gridscaleDIRAC = OsgiProject(dir, "gridscale.dirac", imports = Seq("*"), privatePackages = Seq("gridscale.dirac.*", "org.apache.commons.compress.*", "org.brotli.*", "org.tukaani.*", "com.github.luben.*")) settings (
  settings,
  libraryDependencies += "org.openmole.gridscale" %% "dirac" % gridscaleVersion,
  libraryDependencies += "org.brotli" % "dec" % "0.1.2",
  libraryDependencies += "org.tukaani" % "xz" % "1.9",
  libraryDependencies += "com.github.luben" % "zstd-jni" % "1.4.4-3",
  version := gridscaleVersion
) dependsOn(gridscale, gridscaleHTTP)

lazy val gridscaleWebDAV = OsgiProject(dir, "gridscale.webdav", imports = Seq("*")) settings (
  settings,
  libraryDependencies += "org.openmole.gridscale" %% "webdav" % gridscaleVersion,
  version := gridscaleVersion
) dependsOn(gridscale, gridscaleHTTP)

lazy val xzJava = OsgiProject(dir, "xzjava", imports = Seq("*"), exports = Seq("org.tukaani.*")) settings (
  settings,
  libraryDependencies +=  "org.tukaani" % "xz" % xzVersion,
  version := xzVersion
)


lazy val guava = OsgiProject(dir, "com.google.guava", imports = Seq("!com.google.appengine.*", "!com.google.apphosting.*", "*"), exports = Seq("com.google.guava.*", "com.google.common.*"), privatePackages = Seq("!scala.*", "*")) settings (
  settings,
  libraryDependencies += "com.google.guava" % "guava" % guavaVersion,
  version := guavaVersion
)

//lazy val endpoint4SAPI = OsgiProject(dir, "org.endpoints4s.api", imports = Seq("!sun.security.*", "*"), exports = Seq("endpoint4s.algebra.*", "endpoint4s.circe.*"), privatePackages = Seq("!scala.*", "endpoint4s.*")) enablePlugins(ScalaJSPlugin) settings (
//  libraryDependencies += "org.openmole.endpoints4s" %%% "json-schema-circe" % endpoint4SCirceSchemaVersion,
//  libraryDependencies += "org.openmole.endpoints4s" %%% "algebra" % endpoints4SVersion,
//  version := endpoints4SVersion
//) settings(settings: _*) settings(settings: _*) dependsOn(circe)

lazy val endpoint4s = OsgiProject(dir, "org.endpoints4s", imports = Seq("!sun.security.*", "!scalajs.*", "!org.scalajs.*", "*"), exports = Seq("endpoints4s.*"), privatePackages = Seq("ujson.*", "geny.*", "upickle.*", "org.objectweb.asm.*")) settings (
  settings,
  libraryDependencies += "org.endpoints4s" %% "http4s-server" % endpoint4SHttp4SVersion,
  libraryDependencies += "com.github.jnr" % "jnr-unixsocket" % "0.38.22",
  libraryDependencies += "org.endpoints4s" %% "http4s-server" % endpoint4SHttp4SVersion,

  libraryDependencies += "org.endpoints4s" %% "json-schema-circe" % endpoint4SCirceSchemaVersion,
  libraryDependencies += "org.endpoints4s" %% "algebra" % endpoints4SVersion,

  libraryDependencies += "org.endpoints4s" %% sjs("json-schema-circe") % endpoint4SCirceSchemaVersion,
  libraryDependencies += "org.endpoints4s" %% sjs("algebra") % endpoints4SVersion,
  
  libraryDependencies += "org.endpoints4s" %% sjs("fetch-client-circe") % endpoint4SFetchClientCirceVersion,

  version := endpoints4SVersion
) dependsOn(cats, circe, http4s)


lazy val http4s = OsgiProject(dir, "org.http4s", imports = Seq("!sun.security.*", "!scalajs.*", "!org.scalajs.*", "!sun.nio.ch.*", "*"), exports = Seq("org.http4s.*", "fs2.*", "org.typelevel.ci.*", "org.typelevel.vault.*", "org.typelevel.log4cats.*"), privatePackages = Seq("com.comcast.ip4s.*", "com.twitter.hpack.*", "jnr.*", "com.kenai.*", "org.log4s.*", "org.typelevel.literally.*", "scodec.*", "org.objectweb.asm.*")) settings (
  settings,
  libraryDependencies += "org.http4s" %% "http4s-blaze-server" % http4sVersion,
  libraryDependencies += "org.http4s" %% "http4s-dsl" % http4sVersion,
  libraryDependencies += "com.github.jnr" % "jnr-unixsocket" % "0.38.22",
  version := http4sVersion
) dependsOn(cats, slf4j)



