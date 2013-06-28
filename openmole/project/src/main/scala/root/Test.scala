package root

import sbt._
import Keys._

import org.clapper.sbt.izpack.IzPack
import IzPack.IzPack._
import org.openmole.buildsystem.OMKeys._
import org.openmole.buildsystem._
import Libraries._
import com.typesafe.sbt.osgi.OsgiKeys._

object Test extends Defaults(Base, Gui, Libraries, ThirdParties, Web, Application) {
  val dir = file("Test")

  private val equinoxDependencies = libraryDependencies ++= Seq(
    "org.eclipse.core" % "org.eclipse.equinox.app" % "1.3.100.v20120522-1841" intransitive () extra ("project-name" -> "eclipse"),
    "org.eclipse.core" % "org.eclipse.core.contenttype" % "3.4.200.v20120523-2004" intransitive (),
    "org.eclipse.core" % "org.eclipse.core.jobs" % "3.5.300.v20120912-155018" intransitive (),
    "org.eclipse.core" % "org.eclipse.core.runtime" % "3.8.0.v20120912-155025" intransitive (),
    "org.eclipse.core" % "org.eclipse.equinox.common" % "3.6.100.v20120522-1841" intransitive (),
    "org.eclipse.core" % "org.eclipse.equinox.launcher" % "1.3.0.v20120522-1813" intransitive (),
    "org.eclipse.core" % "org.eclipse.equinox.registry" % "3.5.200.v20120522-1841" intransitive (),
    "org.eclipse.core" % "org.eclipse.equinox.preferences" % "3.5.1.v20121031-182809" intransitive (),
    "org.eclipse.core" % "org.eclipse.osgi" % "3.8.2.v20130124-134944" intransitive ()
  )

  lazy val coreBundles = Assembly.projFilter(Assembly.projFilter(subProjects, bundleProj, (b: Boolean) ⇒ b), bundleType, (_: String) == "core")

  lazy val uiProjects: Seq[Setting[_]] = resourceSets <<= Assembly.sendBundles(coreBundles, "plugins")

  lazy val openmoleTest = AssemblyProject("openmole-test", "plugins", settings = resAssemblyProject ++ uiProjects, depNameMap =
    Map("""org\.eclipse\.equinox\.launcher.*\.jar""".r -> { s ⇒ "org.eclipse.equinox.launcher.jar" }, """org\.eclipse\.(core|equinox|osgi)""".r -> { s ⇒ s.replaceFirst("-", "_") })
  ) settings (
    equinoxDependencies, libraryDependencies += "fr.iscpif.gridscale.bundle" % "fr.iscpif.gridscale" % gridscaleVersion intransitive (),
    dependencyFilter := DependencyFilter.fnToModuleFilter { m ⇒ m.extraAttributes get ("project-name") map (_ == projectName) getOrElse (m.organization == "org.eclipse.core" || m.organization == "fr.iscpif.gridscale.bundle") }
  )
}
