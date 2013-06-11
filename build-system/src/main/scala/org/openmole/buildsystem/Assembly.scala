package org.openmole.buildsystem

import sbt._
import Keys._
import scala.util.matching.Regex
import OMKeys._
import java.util.zip.GZIPOutputStream
import org.kamranzafar.jtar.{ TarEntry, TarOutputStream }
import resource._
import java.io.{ BufferedOutputStream, BufferedInputStream, FileOutputStream }
import scala.io.Source
import sbt.Path._

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 6/5/13
 * Time: 3:42 PM
 */
trait Assembly { self: BuildSystemDefaults ⇒

  //To add zipping to project, add zipProject to its settings
  lazy val zipProject: Seq[Project.Setting[_]] = Seq(
    zipFiles <+= copyDependencies map { f ⇒ f },
    zip <<= (zipFiles, streams, target, tarGZName) map zipImpl,
    tarGZName := None,

    assemble <<= assemble dependsOn zip
  )

  lazy val urlDownloadProject: Seq[Project.Setting[_]] = Seq(
    urls := Nil,
    downloadUrls <<= (urls, streams, target) map urlDownloader,
    assemble <<= assemble dependsOn downloadUrls
  )

  lazy val copyResProject: Seq[Project.Setting[_]] = Seq(
    copyResTask,
    zipFiles <+= resourceAssemble map { f ⇒ f }
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
      assemble <<= assemble dependsOn (copyDependencies tag Tags.Disk),
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

  def zipImpl(targetFolders: Seq[File], s: TaskStreams, t: File, name: Option[String]): File = {
    val out = t / ((name getOrElse "assembly") + ".tar.gz")

    val tgzOS = managed(new TarOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(out)))))

    def findFiles(f: File): Set[File] = if (f.isDirectory) (f.listFiles map findFiles flatten).toSet else Set(f)

    def findLeastCommonPath(f1: File, f2: File): File = file(f1.getCanonicalPath zip f2.getCanonicalPath takeWhile { case (a, b) ⇒ a == b } map (_._1) mkString)

    val files: Set[File] = (targetFolders map findFiles flatten).toSet

    val fn = FileFunction.cached(t / "zip-cache", FilesInfo.full, FilesInfo.lastModified) {
      fileSet ⇒
        s.log.info("Zipping:\n\t")

        val lCP = targetFolders reduceLeft findLeastCommonPath

        s.log.info(lCP.getAbsolutePath)
        s.log.info(targetFolders.last.relativeTo(lCP).get.getPath)

        for {
          os ← tgzOS
          file ← fileSet
          is ← managed(Source.fromFile(file)(scala.io.Codec.ISO8859))
        } {
          val relativeFile = (file relativeTo lCP).get.getPath
          s.log.info("\t - " + relativeFile)
          os.putNextEntry(new TarEntry(file, relativeFile))

          for (c ← is.iter) {
            os.write(c.toByte)
          }

          os.flush()
        }
        Set(out)
    }

    fn(files).head
  }

  def urlDownloader(urls: Seq[(URL, File)], s: TaskStreams, targetDir: File) = {
    val cache = targetDir / "url-cache"

    val cacheInput = managed(Source.fromFile(cache)(io.Codec.ISO8859))
    val cacheOutput = managed(new BufferedOutputStream(new FileOutputStream(cache)))

    val hashes = (urls map { case (url, _) ⇒ url.toString } flatten).toIterator

    val alreadyCached = if (cache.exists) {
      ((cacheInput map { _.iter zip hashes forall { case (n, cached) ⇒ n == cached } }).opt getOrElse false) && (urls forall (_._2.exists))
    }
    else {
      false
    }

    if (alreadyCached) {
      Seq.empty
    }
    else {
      for { os ← cacheOutput; hash ← hashes } { os.write(hash) }
      urls.map {
        case (url, file) ⇒
          s.log.info("Downloading " + url + " to " + file)
          val os = managed(new BufferedOutputStream(new FileOutputStream(file)))
          os.foreach(BasicIO.transferFully(url.openStream, _))
          file
      }
    }
  }
}
