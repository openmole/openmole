package root

import org.openmole.buildsystem.OMKeys._

import com.typesafe.sbt.osgi.OsgiKeys._

import Libraries._
import libraries.Apache
import ThirdParties._
import sbt._
import Keys._

import com.typesafe.sbt.SbtNativePackager._
import NativePackagerKeys._

object Application extends Defaults {
  override val org = "org.openmole.ui"
  val dir = file("application")

  private val equinoxDependencies = libraryDependencies ++= Seq(
    "org.eclipse.core" % "org.eclipse.equinox.app" % "1.3.100.v20120522-1841" intransitive (),
    "org.eclipse.core" % "org.eclipse.core.contenttype" % "3.4.200.v20120523-2004" intransitive (),
    "org.eclipse.core" % "org.eclipse.core.jobs" % "3.5.300.v20120912-155018" intransitive (),
    "org.eclipse.core" % "org.eclipse.core.runtime" % "3.8.0.v20120912-155025" intransitive (),
    "org.eclipse.core" % "org.eclipse.equinox.common" % "3.6.100.v20120522-1841" intransitive (),
    "org.eclipse.core" % "org.eclipse.equinox.launcher" % "1.3.0.v20120522-1813" intransitive (),
    "org.eclipse.core" % "org.eclipse.equinox.registry" % "3.5.200.v20120522-1841" intransitive (),
    "org.eclipse.core" % "org.eclipse.equinox.preferences" % "3.5.1.v20121031-182809" intransitive (),
    "org.eclipse.core" % "org.eclipse.osgi" % "3.8.2.v20130124-134944" intransitive ()
  )

  lazy val openmoleui = OsgiProject("org.openmole.ui", singleton = true, buddyPolicy = Some("global")) settings
    (equinoxDependencies, bundleType := Set("core")) dependsOn
    (base.Misc.workspace, base.Misc.replication, base.Misc.exception, base.Misc.tools, base.Misc.eventDispatcher,
      base.Misc.pluginManager, jodaTime, scalaLang, jasypt, Apache.config, objenesis, base.Core.implementation, robustIt,
      scopt, base.Core.batch, gui.Core.implementation, base.Misc.sftpserver, base.Misc.logging, jline, Apache.logging,
      Apache.ant, Web.core, base.Misc.console, base.Core.convenience) /*


  lazy val rpm = AssemblyProject("package", "packages") settings (packagerSettings: _*) settings (
    maintainer in Debian := "Romain Reuillon <romain@reuillon.org>",
    maintainer in Rpm <<= maintainer in Debian,
    packageSummary in Linux := "Open MOdeL Experiment workflow engine",
    packageDescription in Rpm := """This package contains the OpenMole executable, an easy to use system for massively parrelel computation.""",
    packageDescription in Debian <<= packageDescription in Rpm,
    linuxPackageMappings <+= (target in Linux) map { (ct: File) ⇒
      val src = ct / "assembly"
      val dest = "/opt/openmole"
      packageMapping(
        (for {
          path ← (src ***).get
        } yield path -> path.toString.replaceFirst(src.toString, dest)): _*
      ) withUser "root" withGroup "root" withPerms "0755"
    },
    name in Rpm := "OpenMOLE",
    rpmRelease := "1",
    rpmVendor := "iscpif",
    rpmUrl := Some("http://www.openmole.org/"),
    rpmLicense := Some("AGPL3"),
    version in Rpm <<= (version) { v ⇒ v.replace("-", ".") },
    debianPackageDependencies := Seq("openjdk-7-jdk"),
    rpmPrerequisites := Seq("java-1.7.0-openjdk"),
    name in Debian := "OpenMOLE",
    version in Debian <<= (version) { v ⇒ v.replace("-", ".") }
  )*/
}
