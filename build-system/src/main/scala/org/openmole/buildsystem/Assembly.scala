package org.openmole.buildsystem

import org.apache.commons.compress.archivers.tar.{ TarArchiveEntry, TarArchiveOutputStream }
import sbt._
import Keys._
import scala.util.matching.Regex
import OMKeys._
import java.util.zip.GZIPOutputStream
import resource._
import java.io.{ BufferedOutputStream, FileOutputStream }
import scala.io.Source
import com.typesafe.sbt.osgi.OsgiKeys._

/**
 * Created with IntelliJ IDEA.
 * User: luft
 * Date: 6/5/13
 * Time: 3:42 PM
 */
trait Assembly { self: BuildSystemDefaults ⇒

  lazy val tarProject: Seq[Setting[_]] = Seq(
    Tar.name := "assemble.tar.gz",
    Tar.innerFolder := "",
    Tar.tar <<= (assemble, streams, target, Tar.name, Tar.innerFolder) map tarImpl
  )

  lazy val urlDownloadProject: Seq[Setting[_]] = Seq(
    downloads := Nil,
    assemble <<= assemble dependsOn {
      (downloads, assemblyPath, target, streams) map urlDownloader
    }
  )

  private def copyFileTask(from: File, destinationDir: File, streams: TaskStreams) = {
    streams.log.info(s"Copy file $from to $destinationDir ")
    val to =
      if (from.isDirectory) {
        destinationDir.getParentFile.mkdirs()
        IO.copyDirectory(from, destinationDir, preserveLastModified = true)
        destinationDir
      }
      else {
        destinationDir.mkdirs
        val to = destinationDir / from.getName
        IO.copyFile(from, to, preserveLastModified = true)
        to
      }
    from -> to
  }

  private def rename(srcPath: File, depMap: Map[Regex, String ⇒ String]) =
    depMap.keys.find(
      _.findFirstIn(srcPath.getName).isDefined
    ).map(k ⇒ depMap(k)(srcPath.getName)).getOrElse { srcPath.getName }

  private def copyLibraryDependencies(
    cp: Seq[Attributed[File]],
    out: File,
    subDir: String,
    depMap: Map[Regex, String ⇒ String],
    depFilter: ModuleID ⇒ Boolean,
    streams: TaskStreams) = {

    cp.flatMap { attributed ⇒
      attributed.get(Keys.moduleID.key) match {
        case Some(moduleId) ⇒
          if (depFilter(moduleId)) Some(attributed.data) else None
        case None ⇒ None
      }
    }.map { srcPath ⇒
      val name = rename(srcPath, depMap)
      val destPath = out / subDir / name
      streams.log.info(s"Copy dependency $srcPath to $destPath")
      destPath.getParentFile.mkdirs
      IO.copyFile(srcPath, destPath, preserveLastModified = true)
      srcPath -> destPath
    }
  }

  def assemblySettings = Seq(
    resourcesAssemble := Seq.empty,
    setExecutable := Seq.empty,
    outputDir := "",
    assemble <<=
      (assemblyPath, setExecutable) map {
        (path, files) ⇒
          files.foreach(f ⇒ new File(path, f).setExecutable(true))
          path
      } dependsOn (copyResources),
    assemblyPath <<= target / "assemble",
    bundleProj := false,
    install := true,
    installRemote := true,
    dependencyNameMap := Map.empty[Regex, String ⇒ String],
    dependencyFilter := { _ ⇒ true },
    copyResources <<=
      (resourcesAssemble, assemblyPath, streams) map {
        case (resources, a, s) ⇒
          resources.toSeq.map { case (from, to) ⇒ copyFileTask(from, a / to, s) }
      },
    copyResources <++= (externalDependencyClasspath in Compile, assemblyPath, outputDir, dependencyNameMap, dependencyFilter, streams) map copyLibraryDependencies
  )

  def tarImpl(folder: File, s: TaskStreams, t: File, name: String, innerFolder: String): File = {
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

        val lCP = folder //targetFolders reduceLeft findLeastCommonPath

        // s.log.info(lCP.getAbsolutePath)
        // s.log.info(targetFolders.last.relativeTo(lCP).get.getPath)

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

          for (c ← is.iter) { os.write(c.toByte) }

          os.closeArchiveEntry()
        }
        Set(out)
    }

    fn(files).head
  }

  def urlDownloader(urls: Seq[(URL, String)], assembleDir: File, targetDir: File, s: TaskStreams) = {
    def cache(url: URL) = targetDir / s"url-cache-${Hash.toHex(Hash(url.toString))}"

    targetDir.mkdirs

    for {
      (url, file) ← urls
    } yield {
      val cacheFile = cache(url)
      val destFile = new File(assembleDir, file)
      destFile.getParentFile.mkdirs
      if (!cacheFile.exists) {
        s.log.info("Downloading " + url + " to " + destFile)
        val os = managed(new BufferedOutputStream(new FileOutputStream(destFile)))
        os.foreach(BasicIO.transferFully(url.openStream, _))
        cacheFile.createNewFile()
      }
      file
    }
  }

}

object Assembly {
  //checks to see if settingkey key exists for project p in Seq s. If it does, applies the filter function to key's value, and if that returns true, the project stays in the seq.
  def projFilter[T](s: Seq[ProjectReference], key: SettingKey[T], filter: T ⇒ Boolean, intransitive: Boolean): Def.Initialize[Seq[ProjectReference]] = {
    // (key in p) ? returns Initialize[Option[T]]
    // Project.Initialize.join takes a Seq[Initialize[_]] and gives back an Initialize[Seq[_]]
    val ret = Def.Initialize.join(s map { p ⇒ (key in p).?(i ⇒ i -> p) })(_ filter {
      case (None, _)    ⇒ false
      case (Some(v), _) ⇒ filter(v)
    })(_ map { _._2 })

    lazy val ret2 = Def.bind(ret) { r ⇒
      val x = r.map(expandToDependencies)
      val y = Def.Initialize.join(x)
      y { _.flatten.toSet.toSeq } //make sure all references are unique
    }

    if (intransitive) ret else ret2
  }

  //recursively explores the dependency tree of pr and adds all dependencies to the list of projects to be copied
  def expandToDependencies(pr: ProjectReference): Def.Initialize[Seq[ProjectReference]] = {
    val r = (thisProject in pr) { _.dependencies.map(_.project) }
    val r3 = Def.bind(Def.bind(r) { ret ⇒ Def.Initialize.join(ret map expandToDependencies) }) { ret ⇒ r(first ⇒ pr +: ret.flatten) }
    r3
  }

  implicit def ProjRefs2RichProjectSeq(s: Seq[ProjectReference]) = new RichProjectSeq(Def.value(s))

  implicit def InitProjRefs2RichProjectSeq(s: Def.Initialize[Seq[ProjectReference]]) = new RichProjectSeq(s)

  class RichProjectSeq(s: Def.Initialize[Seq[ProjectReference]]) {
    def keyFilter[T](key: SettingKey[T], filter: (T) ⇒ Boolean, intransitive: Boolean = false) = projFilter(s, key, filter, intransitive)
    def sendTo(to: String) = sendBundles(s, to) //TODO: This function is specific to OSGI bundled projects. Make it less specific?
  }

  def projFilter[T](s: Def.Initialize[Seq[ProjectReference]], key: SettingKey[T], filter: T ⇒ Boolean, intransitive: Boolean): Project.Initialize[Seq[ProjectReference]] = {
    Def.bind(s)(j ⇒ projFilter(j, key, filter, intransitive))
  }

  //TODO: New API makes this much simpler
  //val bundles: Seq[FIle] = bundle.all( ScopeFilter( inDependencies(ref) ) ).value

  def sendBundles(bundles: Def.Initialize[Seq[ProjectReference]], to: String): Def.Initialize[Task[Seq[(File, String)]]] = Def.bind(bundles) { projs ⇒
    require(projs.nonEmpty)
    val seqOTasks: Def.Initialize[Seq[Task[Seq[(File, String)]]]] = Def.Initialize.join(projs.map(p ⇒ bundle in p map { f ⇒
      Seq(f -> to)
    }))
    seqOTasks { seq ⇒ seq.reduceLeft[Task[Seq[(File, String)]]] { case (a, b) ⇒ a flatMap { i ⇒ b map { _ ++ i } } } }
  }
}
