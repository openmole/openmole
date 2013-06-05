package org.openmole.buildsystem

import sbt._
import Keys._
import scala.util.matching.Regex
import OMKeys._

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 6/5/13
 * Time: 3:42 PM
 */
trait Assembly { self: BuildSystemDefaults ⇒

  //To add zipping to project, add zipProject to its settings
  lazy val zipProject: Seq[Project.Setting[_]] = Seq(
    zipFiles <++= (copyDependencies) map { rA ⇒ Seq(rA) },
    zip <<= (zipFiles, streams) map zipImpl,

    assemble <<= assemble dependsOn zip
  )

  lazy val urlDownloadProject: Seq[Project.Setting[_]] = Seq(
    zipFiles <+= (downloadUrls) map { f ⇒ f }, //Adds the result of the url download task to what should be zipped.
    downloadUrls <<= (urls) map urlDownloader
  )

  lazy val copyResProject: Seq[Project.Setting[_]] = Seq(
    copyResTask,
    zipFiles <+= (resourceAssemble) map { f ⇒ f }
  )

  lazy val copyResTask = resourceAssemble <<= (resourceDirectory, outDir, assemblyPath, resourceOutDir) map { //TODO: Find a natural way to do this
    (rT, outD, cT, rOD) ⇒
      {
        val destPath = rOD map (cT / _) getOrElse (cT / outD)
        IO.copyDirectory(rT, destPath)
        destPath
      }
  }

  private def copyDepTask(updateReport: UpdateReport, version: String, out: File,
                          scalaVer: String, subDir: String,
                          depMap: Map[Regex, String ⇒ String], depFilter: DependencyFilter) = {
    updateReport matching depFilter map { f ⇒
      depMap.keys.find(_.findFirstIn(f.getName).isDefined).map(depMap(_)).getOrElse { a: String ⇒ a } -> f
    } foreach {
      case (lambda, srcPath) ⇒
        val destPath = out / subDir / lambda(srcPath.getName)
        IO.copyFile(srcPath, destPath, preserveLastModified = true)
    }
    out / subDir
  }

  def AssemblyProject(base: String,
                      outputDir: String = "lib",
                      settings: Seq[Project.Setting[_]] = Nil,
                      depNameMap: Map[Regex, String ⇒ String] = Map.empty[Regex, String ⇒ String]) = {
    val projBase = dir / base
    val s = settings
    Project(base + "-" + outputDir.replace('/', '_'), projBase, settings = Project.defaultSettings ++ Seq(
      assemble := false,
      assemble <<= assemble dependsOn (copyDependencies tag (Tags.Disk)),
      assemblyPath <<= target / "assemble",
      install := true,
      installRemote := true,
      zipFiles := Nil,
      outDir := outputDir,
      resourceOutDir := None,
      dependencyNameMap := depNameMap,
      dependencyFilter := moduleFilter(),
      copyDependencies <<= (update, version, assemblyPath, scalaVersion, outDir, dependencyNameMap, dependencyFilter) map copyDepTask
    ) ++ s ++ scalariformDefaults)
  }

  //I'll implement this

  def zipImpl(targetFolder: Seq[File], s: TaskStreams): File = {
    s.log.info("Zipping:\n\t" + targetFolder.mkString(",\n\t"))
    targetFolder.head
  }

  def urlDownloader(urls: Seq[URL]): File = {
    dir
  }
}
