package projectRoot

import sbt._
import Keys._

trait Application extends Web with Libraries {
  private implicit val org = organization := "org.openmole.ui"
  private implicit val dir = file("application")
  lazy val application = Project("application", file("application")) aggregate(openMoleDB,plugins, openmoleui)

  val pluginDependencies = libraryDependencies <++= version {
    v =>
      Seq("org.openmole.core" % "org.openmole.core.model" % v,
        "org.openmole.core" % "org.openmole.core.implementation" % v,
        "org.openmole.core" % "org.openmole.core.batch" % v,
        "org.openmole.core" % "org.openmole.misc.workspace" % v,
        "org.openmole.core" % "org.openmole.misc.replication" % v,
        "org.openmole.core" % "org.openmole.misc.exception" % v,
        "org.openmole.core" % "org.openmole.misc.tools" % v,
        "org.openmole.core" % "org.openmole.misc.eventdispatcher" % v,
        "org.openmole.core" % "org.openmole.misc.pluginmanager" % v,
        "org.openmole.core" % "org.openmole.misc.logging" % v,
        "org.openmole.core" % "org.openmole.misc.sftpserver" % v,
        "org.eclipse.core" % "org.eclipse.equinox.app" % "1.3.100.v20120522-1841",
        "org.eclipse.equinox" % "org.eclipse.equinox.common" % "3.6.0.v20100503",
        "org.eclipse.core" % "org.eclipse.equinox.launcher" % "1.3.0.v20120522-1813",
        "org.eclipse.equinox" % "org.eclipse.equinox.registry" % "3.5.0.v20100503",
        "org.eclipse.equinox" % "org.eclipse.equinox.preferences" % "3.3.0.v20100503",
        "org.eclipse.core" % "org.eclipse.osgi" % "3.8.2.v20130124-134944",
        "org.openmole" % "org.apache.commons.logging" % v,
        "org.openmole" % "net.sourceforge.jline" % v,
        "org.openmole" % "org.apache.ant" % v,
        "org.openmole" % "uk.com.robustit.cloning" % v,
        "org.openmole" % "org.joda.time" % v,
        "org.openmole" % "org.scala-lang.scala-library" % v,
        "org.openmole" % "org.jasypt.encryption" % v,
        "org.openmole" % "org.apache.commons.configuration" % v,
        "org.openmole" % "org.objenesis" % v,
        "org.openmole" % "com.github.scopt" % v,
        "org.openmole.ide" % "org.openmole.ide.core.implementation" % v)}

  lazy val openmoleui = OsgiProject("org.openmole.ui") settings (pluginDependencies) dependsOn (webCore)

  lazy val plugins = AssemblyProject("package", "plugins") settings (pluginDependencies,
    libraryDependencies <++= (version) {v =>
      Seq("org.openmole.ui" %% "org.openmole.ui" % v,
        "org.openmole.web" %% "org.openmole.web.core" % v)
    })


  lazy val openMoleDB = AssemblyProject("Db-proj", "plugins") settings (libraryDependencies ++=
    Seq("org.openmole.core" % "org.openmole.runtime.dbserver" % "0.8.0-SNAPSHOT"))


}