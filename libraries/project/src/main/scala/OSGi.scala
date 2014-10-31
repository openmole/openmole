package root

import sbt._
import Keys._
import com.typesafe.sbt.osgi.OsgiKeys
import OsgiKeys._
import root.libraries._
import org.openmole.buildsystem.OMKeys._
import scala.scalajs.sbtplugin.ScalaJSPlugin._

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 3/17/13
 * Time: 6:50 PM
 * To change this template use File | Settings | File Templates.
 */
object OSGi extends Defaults(Apache) {

  val dir = file("libraries")
  val bouncyCastleVersion = "1.50"

  lazy val bouncyCastle = "org.bouncycastle" % "bcpkix-jdk15on" % bouncyCastleVersion

  lazy val includeOsgi = libraryDependencies <+= (osgiVersion) { oV ⇒ "org.eclipse.core" % "org.eclipse.osgi" % oV}

  lazy val jetty = OsgiProject(
    "org.eclipse.jetty",
    exports = Seq("org.eclipse.jetty.*", "javax.*")) settings(
    libraryDependencies ++= Seq("org.eclipse.jetty" % "jetty-webapp" % "8.1.8.v20121106", "org.eclipse.jetty.orbit" % "javax.servlet" % "3.0.0.v201112011016"),
    version := "8.1.8.v20121106"
    )

  lazy val scalatraVersion = "2.3.0"

  lazy val scalatra = OsgiProject("org.scalatra",
    buddyPolicy = Some("global"),
    exports = Seq("org.scalatra.*, org.fusesource.*"),
    privatePackages = Seq("!scala.*", "!org.slf4j.*", "!org.json4s", "*")) settings
    (libraryDependencies ++= Seq("org.scalatra" %% "scalatra" % scalatraVersion,
      "org.scalatra" %% "scalatra-json" % scalatraVersion), version := scalatraVersion) dependsOn (slf4j)

  lazy val scalate = OsgiProject("scalate", exports = Seq("org.scalatra.*")) settings
    (libraryDependencies += "org.scalatra" %% "scalatra-scalate" % scalatraVersion, version := scalatraVersion)

  lazy val jacksonJson = OsgiProject("org.json4s") settings(
    libraryDependencies += "org.json4s" %% "json4s-jackson" % "3.2.9",
    exportPackage += "com.fasterxml.*",
    version := "3.2.9"
    )

  lazy val logback = OsgiProject("ch.qos.logback", exports = Seq("ch.qos.logback.*", "org.slf4j.impl")) settings
    (libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.0.9", version := "1.0.9")

  lazy val h2Version = "1.3.176"
  lazy val h2 = OsgiProject("org.h2", buddyPolicy = Some("global"), privatePackages = Seq("META-INF.*")) settings
    (libraryDependencies += "com.h2database" % "h2" % h2Version, version := h2Version)

  lazy val bonecp = OsgiProject("com.jolbox.bonecp", buddyPolicy = Some("global")) settings
    (libraryDependencies += "com.jolbox" % "bonecp" % "0.8.0-rc1", version := "0.8.0-rc1")

  lazy val slickVersion = "2.1.0"
  lazy val slick = OsgiProject("com.typesafe.slick", exports = Seq("scala.slick.*")) settings
    (libraryDependencies += "com.typesafe.slick" %% "slick" % slickVersion, version := slickVersion)

  lazy val slf4j = OsgiProject("org.slf4j") settings(
    libraryDependencies += "org.slf4j" % "slf4j-api" % "1.7.2",
    version := "1.7.2"
    )


  lazy val xstream = OsgiProject(
    "com.thoughtworks.xstream",
    buddyPolicy = Some("global"),
    privatePackages = Seq("!scala.*", "*")) settings(
    libraryDependencies ++= Seq("com.thoughtworks.xstream" % "xstream" % "1.4.7", "net.sf.kxml" % "kxml2" % "2.3.0"),
    version := "1.4.7",
    bundleType += "dbserver")

  lazy val groovy = OsgiProject(
    "org.codehaus.groovy",
    buddyPolicy = Some("global"),
    exports = Seq("groovy.*", "org.codehaus.*"),
    privatePackages = Seq("!scala.*,*")) settings(
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
    }, bundleType += "dbserver", version := scalaVersion.value)

  //  lazy val scalaCompiler = OsgiProject("org.scala-lang.scala-compiler", exports = Seq("scala.tools.*", "scala.reflect.macros.*"),
  //    privatePackages = Seq("!scala.*", "*"), buddyPolicy = Some("global")) settings (libraryDependencies <<= scalaVersion { s ⇒ Seq("org.scala-lang" % "scala-compiler" % s) })

  lazy val jodaTime = OsgiProject("org.joda.time") settings(
    libraryDependencies += "joda-time" % "joda-time" % "1.6",
    version := "1.6"
    )

  lazy val jasyptVersion = "1.9.2"
  lazy val jasypt = OsgiProject("org.jasypt.encryption", exports = Seq("org.jasypt.*")) settings(
    libraryDependencies += "org.jasypt" % "jasypt" % jasyptVersion,
    version := jasyptVersion
    )

  lazy val robustIt = OsgiProject("uk.com.robustit.cloning", exports = Seq("com.rits.*"), privatePackages = Seq("org.objenesis.*")) settings(
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


  lazy val netlogo4 = OsgiProject("ccl.northwestern.edu.netlogo4", exports = Seq("org.nlogo.*"),
    privatePackages = Seq("*")) settings
    (libraryDependencies ++=
      Seq("ccl.northwestern.edu" % "netlogo" % "4.1.3",
        "org.picocontainer" % "picocontainer" % "2.8",
        "org.objectweb" % "asm" % "3.1",
        "org.objectweb" % "asm-commons" % "3.1"), version := "4.1.3", scalaVersion := "2.8.0", crossPaths := false, bundleType := Set("plugin"))

  lazy val netLogo5Version = "5.1.0"
  lazy val netlogo5_noscala = OsgiProject("ccl.northwestern.edu.netlogo5.noscala", exports = Seq("org.nlogo.*"),
    privatePackages = Seq("!scala.*", "*")) settings
    (libraryDependencies ++=
      Seq("ccl.northwestern.edu" % "netlogo" % netLogo5Version,
        "org.objectweb" % "asm-all" % "3.3.1",
        "org.picocontainer" % "picocontainer" % "2.13.6"), version := netLogo5Version, autoScalaLibrary := false, bundleType := Set("all"), scalaVersion := "2.9.2", crossPaths := false,
      ivyScala ~= { (is: Option[IvyScala]) ⇒ //See netlogo4_noscala
        for (i ← is) yield i.copy(checkExplicit = false)
      })

  lazy val netlogo5 = OsgiProject("ccl.northwestern.edu.netlogo5", exports = Seq("org.nlogo.*"),
    privatePackages = Seq("*")) settings
    (libraryDependencies ++= Seq("ccl.northwestern.edu" % "netlogo" % netLogo5Version,
      "org.scala-lang" % "scala-library" % "2.9.2",
      "org.objectweb" % "asm-all" % "3.3.1",
      "org.picocontainer" % "picocontainer" % "2.13.6"), version := netLogo5Version, scalaVersion := "2.9.2", crossPaths := false, bundleType := Set("plugin"))

  lazy val guava = OsgiProject("com.google.guava",
    exports = Seq("com.google.common.*"), privatePackages = Seq("!scala.*", "*")) settings(libraryDependencies ++=
    Seq("com.google.guava" % "guava" % "16.0.1", "com.google.code.findbugs" % "jsr305" % "1.3.9"),
    version := "16.0.1"
    )

  lazy val scalaTagsVersion = "0.4.2"
  lazy val scalaRxVersion = "0.2.6"
  lazy val scalaUpickleVersion = "0.2.5"
  lazy val scalaAutowireVersion = "0.2.3"
  lazy val jsSuffix = "_sjs0.5"

  lazy val scalajsDom = OsgiProject("scalajs-dom", exports = Seq("org.scalajs.dom.*")) settings(
    libraryDependencies += "org.scala-lang.modules.scalajs" %%% "scalajs-dom_sjs0.5" % "0.6", version := "0.6")

  lazy val scalajsVersion = "0.5.5"
  lazy val scalajsTools = OsgiProject("scalajs-tools", exports = Seq("scala.scalajs.tools.*","scala.scalajs.ir.*","com.google.javascript.*","com.google.common.*","rhino_ast.java.com.google.javascript.rhino.*","org.json.*")) settings(
    libraryDependencies += "org.scala-lang.modules.scalajs" %% "scalajs-tools" % scalajsVersion, version := scalajsVersion)

  lazy val scalajsLibrary = OsgiProject("scalajs-library", exports = Seq("scala.scalajs.*","*.sjsir")) settings(
    libraryDependencies += "org.scala-lang.modules.scalajs" %% "scalajs-library" % scalajsVersion, version := scalajsVersion)

  lazy val scalaTagsJS = OsgiProject("com.scalatags.js", exports = Seq("scalatags.*","*.sjsir")) settings(
    libraryDependencies += "com.scalatags" %%% ("scalatags" + jsSuffix) % scalaTagsVersion, version := scalaTagsVersion)

  lazy val scalaRxJS = OsgiProject("com.scalarx.js", exports = Seq("rx.*","*.sjsir"))  settings(
    libraryDependencies += "com.scalarx" %%% ("scalarx" + jsSuffix) % scalaRxVersion, version := scalaRxVersion)

  lazy val upickleJS = OsgiProject("upickle.js", exports = Seq("upickle.*","*.sjsir")) settings(
    libraryDependencies += "com.lihaoyi" %%% ("upickle" + jsSuffix) % scalaUpickleVersion, version := scalaUpickleVersion)

  lazy val autowireJS = OsgiProject("autowire.js", exports = Seq("autowire.*","*.sjsir")) settings(
    libraryDependencies += "com.lihaoyi" %%% ("autowire" + jsSuffix) % scalaAutowireVersion, version := scalaAutowireVersion)

  lazy val scalaTagsJVM = OsgiProject("com.scalatags.jvm", exports = Seq("scalatags.*")) settings(
    libraryDependencies += "com.scalatags" %% "scalatags" % scalaTagsVersion, version := scalaTagsVersion)

  lazy val scalaRxJVM = OsgiProject("com.scalarx.jvm", exports = Seq("rx.*")) settings(
    libraryDependencies += "com.scalarx" %% "scalarx" % scalaRxVersion, version := scalaRxVersion)

  lazy val upickleJVM = OsgiProject("upickle.jvm", exports = Seq("upickle.*")) settings(
    libraryDependencies += "com.lihaoyi" %% "upickle" % scalaUpickleVersion, version := scalaUpickleVersion)

  lazy val autowireJVM = OsgiProject("autowire.jvm", exports = Seq("autowire.*")) settings(
    libraryDependencies += "com.lihaoyi" %% "autowire" % scalaAutowireVersion, version := scalaAutowireVersion)

  lazy val scaladget = OsgiProject("scaladget", exports = Seq("fr.iscpif.scaladget.*")) settings(
    libraryDependencies += "fr.iscpif" %%% "scaladget_sjs0.5" % "0.1.0", version := "0.1.0")

  lazy val jsonSimpleVersion = "1.1.1"
  lazy val jsonSimple = OsgiProject("json-simple", exports = Seq("org.json.simple.*")) settings(
    libraryDependencies += "com.googlecode.json-simple" % "json-simple" % jsonSimpleVersion, version := jsonSimpleVersion)

  lazy val closureCompilerVersion = "v20130603"
  lazy val closureCompiler = OsgiProject("closure-compiler", exports = Seq("com.google.javascript.*")) settings(
    libraryDependencies += "com.google.javascript" % "closure-compiler" % closureCompilerVersion, version := closureCompilerVersion)

  lazy val mgoVersion = "1.78"

  lazy val mgo = OsgiProject("fr.iscpif.mgo") settings(
    libraryDependencies += "fr.iscpif" %% "mgo" % mgoVersion,
    bundleType := Set("plugin"),
    version := mgoVersion
    )

  val monocleVersion = "0.5.0"

  lazy val monocle = OsgiProject("monocle", privatePackages = Seq("!scala.*", "*")) settings(
    libraryDependencies += "com.github.julien-truffaut" %% "monocle-core" % monocleVersion,
    libraryDependencies += "com.github.julien-truffaut" %% "monocle-generic" % monocleVersion,
    libraryDependencies += "com.github.julien-truffaut" %% "monocle-macro" % monocleVersion,
    bundleType := Set("plugin"),
    version := monocleVersion
    )

  lazy val opencsv = OsgiProject("au.com.bytecode.opencsv") settings(
    libraryDependencies += "net.sf.opencsv" % "opencsv" % "2.0",
    version := "2.0",
    bundleType := Set("plugin")
    )

  lazy val jline = OsgiProject("net.sourceforge.jline") settings(
    libraryDependencies += "jline" % "jline" % "0.9.94",
    version := "0.9.94",
    exportPackage := Seq("jline.*"))

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

  lazy val scalabcVersion = "0.4"
  lazy val scalabc = OsgiProject("fr.iscpif.scalabc", privatePackages = Seq("!scala.*", "!junit.*", "*")) settings(
    libraryDependencies += "fr.iscpif" %% "scalabc" % scalabcVersion,
    bundleType := Set("plugin"),
    version := scalabcVersion
    )

  override def OsgiSettings = super.OsgiSettings ++ Seq(bundleType := Set("core")) //TODO make library defaults
}
