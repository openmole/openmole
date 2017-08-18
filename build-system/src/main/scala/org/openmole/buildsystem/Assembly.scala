package org.openmole.buildsystem

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
      _.findFirstIn(srcPath.getName).isDefined).map(k ⇒ depMap(k)(srcPath.getName)).getOrElse {
        srcPath.getName
      }

  private def copyLibraryDependencies(
    externalDependencies: Seq[Attributed[File]],
    out:                  File,
    rename:               ModuleID ⇒ String,
    depFilter:            (ModuleID, Artifact) ⇒ Boolean,
    streams:              TaskStreams) = {
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
    assemble := {
      (copyResources in assemble).value
      urlDownloader(downloads.value, assemblyPath.value, ivyPaths.value, streams.value)
      setExecutable.value.foreach(f ⇒ new File(assemblyPath.value, f).setExecutable(true))
      assemblyPath.value
    },
    TarPlugin.autoImport.tarFolder := assemble.value,
    dependencyName := { (_: ModuleID).name + ".jar" },
    dependencyFilter := { (_, _) ⇒ true },
    (copyResources in assemble) := resourcesAssemble.value.map { case (from, to) ⇒ copyFileTask(from, to, streams.value) },
    (copyResources in assemble) ++=
      copyLibraryDependencies(
        (externalDependencyClasspath in Compile).value,
        assemblyDependenciesPath.value,
        dependencyName.value,
        dependencyFilter.value,
        streams.value))

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

