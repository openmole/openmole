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
    Tar.tar <<= (Tar.folder, streams, target, Tar.name, Tar.innerFolder, streams) map tarImpl
  )

  private def recursiveCopy(from: File, to: File, streams: TaskStreams): Unit = {
    if (from.isDirectory) {
      to.mkdirs()
      for {
        f ← from.listFiles()
      } recursiveCopy(f, new File(to, f.getName), streams)
    }
    else if (!to.exists() || from.lastModified() > to.lastModified) {
      streams.log.info(s"Copy file $from to $to ")
      from.getParentFile.mkdirs
      IO.copyFile(from, to, preserveLastModified = true)
    }
  }

  private def copyFileTask(from: File, destinationDir: File, streams: TaskStreams, name: Option[String] = None) = {
    val to: File = if (from.isDirectory) destinationDir else destinationDir / name.getOrElse(from.getName)
    recursiveCopy(from, to, streams)
    from -> to
  }

  private def rename(srcPath: File, depMap: Map[Regex, String ⇒ String]) =
    depMap.keys.find(
      _.findFirstIn(srcPath.getName).isDefined
    ).map(k ⇒ depMap(k)(srcPath.getName)).getOrElse { srcPath.getName }

  private def copyLibraryDependencies(
    cp: Seq[Attributed[File]],
    out: File,
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
      copyFileTask(srcPath, out, streams, name = Some(name))
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
      } dependsOn (copyResources, (downloads, assemblyPath, target, streams) map urlDownloader),
    Tar.folder <<= assemble,
    bundleProj := false,
    install := true,
    installRemote := true,
    dependencyNameMap := Map.empty[Regex, String ⇒ String],
    dependencyFilter := { _ ⇒ true },
    copyResources <<=
      (resourcesAssemble, streams) map {
        case (resources, s) ⇒
          resources.toSeq.map { case (from, to) ⇒ copyFileTask(from, to, s) }
      },
    copyResources <++= (externalDependencyClasspath in Compile, assemblyDependenciesPath, dependencyNameMap, dependencyFilter, streams) map copyLibraryDependencies
  )

  def generateConfigImpl(plugins: File, header: String, config: File, startLevels: Seq[(String, Int)]): File = {
    def line(file: File) = {
      val name = file.getName
      val level = startLevels.find { case (s, _) ⇒ name.contains(s) }.map { case (_, l) ⇒ l }
      level match {
        case None    ⇒ name
        case Some(l) ⇒ s"$name@$l:start"
      }
    }
    def content =
      s"""
        |$header
        |osgi.bundles=${plugins.listFiles().filter(!_.getName.startsWith("org.eclipse.osgi")).map(line).mkString(",")}
      """.stripMargin
    config.getParentFile.mkdirs
    IO.write(config, content)
    config
  }

  import OMKeys.OSGiApplication._

  def osgiApplicationSettings =
    Seq(
      startLevels := Seq.empty,
      assemble <<= assemble dependsOn {
        (pluginsDirectory, header, config, startLevels) map generateConfigImpl dependsOn (copyResources)
      }
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
    def sendTo(to: Def.Initialize[File]) = sendBundles(s zip to) //TODO: This function is specific to OSGI bundled projects. Make it less specific?
  }

  def projFilter[T](s: Def.Initialize[Seq[ProjectReference]], key: SettingKey[T], filter: T ⇒ Boolean, intransitive: Boolean): Def.Initialize[Seq[ProjectReference]] = {
    Def.bind(s)(j ⇒ projFilter(j, key, filter, intransitive))
  }

  //TODO: New API makes this much simpler
  //val bundles: Seq[FIle] = bundle.all( ScopeFilter( inDependencies(ref) ) ).value

  def sendBundles(bundles: Def.Initialize[(Seq[ProjectReference], File)]): Def.Initialize[Task[Seq[(File, File)]]] = Def.bind(bundles) {
    case (projs, to) ⇒
      require(projs.nonEmpty)
      val seqOTasks: Def.Initialize[Seq[Task[Seq[(File, File)]]]] = Def.Initialize.join(projs.map(p ⇒ (bundle in p) map {
        f ⇒ Seq(f -> to)
      }))
      seqOTasks { seq ⇒ seq.reduceLeft[Task[Seq[(File, File)]]] { case (a, b) ⇒ a flatMap { i ⇒ b map { _ ++ i } } } }
  }
}
