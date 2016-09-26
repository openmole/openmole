package org.openmole.buildsystem

import org.apache.commons.compress.archivers.tar.{ TarArchiveEntry, TarArchiveOutputStream }
import sbt._
import Keys._

import scala.util.matching.Regex
import OMKeys._
import java.util.zip.GZIPOutputStream

import resource._
import java.io.{ BufferedOutputStream, FileOutputStream }

import com.typesafe.sbt.osgi.OsgiKeys

import scala.io.Source
import com.typesafe.sbt.osgi.OsgiKeys._
import org.json4s.jsonwritable

object Assembly {

  lazy val tarProject: Seq[Setting[_]] = Seq(
    Tar.name := "assemble.tar.gz",
    Tar.innerFolder := "",
    Tar.tar <<= (Tar.folder, streams, target, Tar.name, Tar.innerFolder, streams) map tarImpl
  )

  private def recursiveCopy(from: File, to: File, streams: TaskStreams): Unit = {
    if (from.isDirectory) {
      to.mkdirs()
      for {
        f ← from.listFiles()
      } recursiveCopy(f, new File(to, f.getName), streams)
    }
    else copy(from, to, streams)
  }

  private def copy(from: File, to: File, streams: TaskStreams) =
    if (!to.exists() || from.lastModified() > to.lastModified) {
      streams.log.info(s"Copy file $from to $to ")
      from.getParentFile.mkdirs
      IO.copyFile(from, to, preserveLastModified = true)
    }

  private def copyFileTask(from: File, to: File, streams: TaskStreams) = {
    if (from.isDirectory) recursiveCopy(from, to, streams)
    else copy(from, to, streams)
    from → to
  }

  private def rename(srcPath: File, depMap: Map[Regex, String ⇒ String]) =
    depMap.keys.find(
      _.findFirstIn(srcPath.getName).isDefined
    ).map(k ⇒ depMap(k)(srcPath.getName)).getOrElse {
      srcPath.getName
    }

  private def copyLibraryDependencies(
    externalDependencies: Seq[Attributed[File]],
    out:                  File,
    rename:               ModuleID ⇒ String,
    depFilter:            (ModuleID, Artifact) ⇒ Boolean,
    streams:              TaskStreams
  ) = {
    (externalDependencies).distinct.flatMap { attributed ⇒
      (attributed.get(Keys.moduleID.key), attributed.get(Keys.artifact.key)) match {
        case (Some(moduleId), Some(artifact)) ⇒
          if (depFilter(moduleId, artifact)) Some(moduleId → attributed.data) else None
        case _ ⇒ None
      }
    }.map {
      case (module, srcPath) ⇒
        val name = rename(module)
        val to = new File(out, name)
        copyFileTask(srcPath, to, streams)
    }
  }

  def assemblySettings = Seq(
    downloads := Nil,
    resourcesAssemble := Seq.empty,
    setExecutable := Seq.empty,
    assemblyPath := target.value / "assemble",
    assemblyDependenciesPath := assemblyPath.value,
    assemble <<=
      (assemblyPath, setExecutable) map {
        (path, files) ⇒
          files.foreach(f ⇒ new File(path, f).setExecutable(true))
          path
      } dependsOn (copyResources in assemble, (downloads, assemblyPath, ivyPaths, streams) map urlDownloader),
    Tar.folder <<= assemble,
    dependencyName := { (_: ModuleID).name + ".jar" },
    dependencyFilter := { (_, _) ⇒ true },
    (copyResources in assemble) <<=
      (resourcesAssemble, streams) map {
        case (resources, s) ⇒
          resources.map { case (from, to) ⇒ copyFileTask(from, to, s) }
      },
    (copyResources in assemble) <++= ( /*dependencyClasspath in Compile,*/ externalDependencyClasspath in Compile, assemblyDependenciesPath, dependencyName, dependencyFilter, streams) map copyLibraryDependencies
  )

  def tarImpl(folder: File, s: TaskStreams, t: File, name: String, innerFolder: String, streams: TaskStreams): File = {
    val out = t / name

    val tgzOS = managed {
      val tos = new TarArchiveOutputStream(new BufferedOutputStream(new GZIPOutputStream(new FileOutputStream(out))))
      tos.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU)
      tos
    }

    def findFiles(f: File): Set[File] = if (f.isDirectory) (f.listFiles map findFiles flatten).toSet else Set(f)

    val files: Set[File] = findFiles(folder).toSet

    val fn = FileFunction.cached(t / "zip-cache", FilesInfo.lastModified, FilesInfo.exists) {
      fileSet ⇒
        s.log.info("Zipping:\n\t")

        val lCP = folder

        for {
          os ← tgzOS
          file ← fileSet
          is ← managed(Source.fromFile(file)(scala.io.Codec.ISO8859))
        } {
          val relativeFile = innerFolder + "/" + (file relativeTo lCP).get.getPath
          s.log.info("\t - " + relativeFile)

          val entry = new TarArchiveEntry(file, relativeFile)
          entry.setSize(file.length)
          if (file.canExecute) entry.setMode(TarArchiveEntry.DEFAULT_FILE_MODE | 111)

          os.putArchiveEntry(entry)

          for (c ← is.iter) {
            os.write(c.toByte)
          }

          os.closeArchiveEntry()
        }
        Set(out)
    }

    fn(files).head
  }

  def urlDownloader(urls: Seq[(URL, String)], assembleDir: File, ivyPaths: IvyPaths, s: TaskStreams) = {
    val targetDir = ivyPaths.ivyHome.get / "cache" / "url"

    def hash(url: URL) = Hash.toHex(Hash(url.toString))

    targetDir.mkdirs

    for {
      (url, file) ← urls
    } yield {
      val tmpFile = new File(targetDir, hash(url) + "-tmp")
      val cacheFile = new File(targetDir, "url-cache-" + hash(url))

      if (!cacheFile.exists) {
        s.log.info("Downloading " + url + " to " + tmpFile)
        val os = managed(new BufferedOutputStream(new FileOutputStream(tmpFile)))
        os.foreach(BasicIO.transferFully(url.openStream, _))
        tmpFile.renameTo(cacheFile)
      }

      val destFile = new File(assembleDir, file)
      s.log.info(s"Copy $cacheFile to $destFile")
      destFile.getParentFile.mkdirs
      IO.copyFile(cacheFile, destFile)

      file
    }
  }

}

