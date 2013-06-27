package root

import sbt._
import Keys._
import org.clapper.sbt.izpack.IzPack
import org.clapper.sbt.izpack.IzPackSection
import IzPack.IzPack._
import org.openmole.buildsystem.OMKeys._
import Application.{ openmolePlugins, openmoleRuntime }

object Installer extends Defaults {
  val dir = file(".")
  val all = Aggregator("installer-all") aggregate (installer)

  lazy val openmoleSettings: Seq[Setting[_]] = openmolePlugins.settings

  lazy val openmoleAssemblyDir: Setting[File] = openmoleSettings.find(_.key == assemblyPath.scopedKey) map (_.asInstanceOf[Setting[File]]) get

  lazy val t = target in openmolePlugins

  lazy val installer = AssemblyProject("installer", "installer", settings = IzPack.izPackSettings ++ resAssemblyProject) settings (
    assemble := false,
    packageBin := file("."),
    createInstaller in IzPack.IzPack.Config <<= (createInstaller in IzPack.IzPack.Config) dependsOn resourceAssemble,
    variables in Config <++= version { v ⇒ Seq(("version", v), "home" -> "$USER_HOME") },
    installSourceDir in Config <<= assemblyPath,
    configFile in Config <<= assemblyPath { _ / "resources/install.yml" },
    resourceSets <<= (assemblyPath in openmolePlugins, target in openmoleRuntime, tarGZName in openmoleRuntime, baseDirectory) map { (assembly, target, tarGz, bD) ⇒
      Set(
        assembly -> "openmole",
        bD / "resources" -> "resources"
      ) ++ (Set(tarGz getOrElse "assembly", "jvm-386", "jvm-x64") map (n ⇒ target / (n + ".tar.gz") -> "runtime"))
    }
  )
}
