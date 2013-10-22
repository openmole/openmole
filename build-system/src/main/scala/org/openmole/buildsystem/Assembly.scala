package org.openmole.buildsystem

import sbt._
import sbt.Def
import Keys._
import scala.util.matching.Regex
import OMKeys._
import java.util.zip.GZIPOutputStream
import org.kamranzafar.jtar.{ TarEntry, TarOutputStream }
import resource._
import java.io.{ BufferedOutputStream, FileOutputStream }
import scala.io.Source
import com.typesafe.sbt.osgi.OsgiKeys._
import scala.Some

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

  @deprecated
  lazy val copyResProject: Seq[Project.Setting[_]] = Seq(
    copyResTask,
    zipFiles <++= resourceAssemble map { f ⇒ f.toSeq }
  )

  lazy val resAssemblyProject: Seq[Project.Setting[_]] = Seq(
    resourceSets := Set.empty,
    resTask,
    zipFiles <++= resourceAssemble map { (f: Set[File]) ⇒ f.toSeq },
    assemble <<= assemble dependsOn resourceAssemble
  )

  lazy val resTask = resourceAssemble <<= (resourceSets, target, assemblyPath, streams, name) map { //TODO: Find a natural way to do this
    (rS, target, cT, s, name) ⇒
      {
        def expand(f: File, p: File, o: String): Array[(File, File)] = if (f.isDirectory) f.listFiles() flatMap (expand(_, p, o)) else {
          val dest = cT / o / (if (f != p) getDiff(f, p) else f.name)
          Array(f -> dest)
        }

        def isChildOf(f: File, oF: File): Boolean = f.getCanonicalPath.contains(oF.getCanonicalPath)
        def getDiff(f: File, oF: File): String = f.getCanonicalPath.takeRight(f.getCanonicalPath.length - oF.getCanonicalPath.length)

        val resourceMap = (rS flatMap { case (in, out) ⇒ expand(in, in, out) }).toMap

        val copyFunction = FileFunction.cached(target / ("resAssembleCache" + name), FilesInfo.lastModified, FilesInfo.exists) {
          _ map {
            rT ⇒
              /*val out = rS.filter(i ⇒ isChildOf(rT, i._1)).reduce { (a, b) ⇒ if (a._1.getAbsolutePath.length > b._1.getAbsolutePath.length) a else b }
              val dest = cT / out._2 / (if (out._1.isDirectory) getDiff(rT, out._1) else getDiff(rT, out._1.getParentFile))
              println(getDiff(rT, out._1.getParentFile))
              println(rT.getCanonicalPath)
              println(out._1.getParentFile.getCanonicalPath)*/
              val dest = resourceMap(rT)

              s.log.info("Copying file " + rT.getPath + " to: " + dest.getCanonicalPath)
              IO.copyFile(rT, dest)
              dest
          }
        }

        copyFunction(resourceMap.keySet)
      }
  }

  lazy val copyResTask = resourceAssemble <<= (resourceDirectory, outDir, assemblyPath, resourceOutDir) map { //TODO: Find a natural way to do this
    (rT, outD, cT, rOD) ⇒
      {
        val destPath = rOD map (cT / _) getOrElse (cT / outD)
        IO.copyDirectory(rT, destPath)
        Set(destPath)
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
                      baseDir: File = dir,
                      settings: Seq[Project.Setting[_]] = Nil,
                      depNameMap: Map[Regex, String ⇒ String] = Map.empty[Regex, String ⇒ String]) = {
    val projBase = baseDir / base
    val s = settings
    Project(base + "-" + outputDir.replace('/', '_'), projBase, settings = Project.defaultSettings ++ Seq(
      assemble := false,
      assemble <<= assemble dependsOn (copyDependencies tag Tags.Disk),
      assemblyPath <<= target / "assemble",
      bundleProj := false,
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

    val fn = FileFunction.cached(t / "zip-cache", FilesInfo.lastModified, FilesInfo.exists) {
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

    targetDir.mkdir() //makes sure target exists

    val cacheInput = managed(Source.fromFile(cache)(io.Codec.ISO8859))

    val hashes = (urls map { case (url, _) ⇒ url.toString } flatten).toIterator

    val alreadyCached = if (cache.exists) {
      ((cacheInput map { _.iter zip hashes forall { case (n, cached) ⇒ n == cached } }).opt getOrElse false) && (urls forall (_._2.exists))
    }
    else {
      cache.createNewFile()
      false
    }

    val cacheOutput = managed(new BufferedOutputStream(new FileOutputStream(cache)))

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

object Assembly {
  //checks to see if settingkey key exists for project p in Seq s. If it does, applies the filter function to key's value, and if that returns true, the project stays in the seq.
  def projFilter[T](s: Seq[ProjectReference], key: SettingKey[T], filter: T ⇒ Boolean): Def.Initialize[Seq[ProjectReference]] = {
    // (key in p) ? returns Initialize[Option[T]]
    // Project.Initialize.join takes a Seq[Initialize[_]] and gives back an Initialize[Seq[_]]
    val ret = Def.Initialize.join(s map { p ⇒ (key in p).?(i ⇒ i -> p) })(_ filter {
      case (None, _)    ⇒ false
      case (Some(v), _) ⇒ filter(v)
    })(_ map { _._2 })

    val ret2 = Def.bind(ret) { r ⇒
      val x = r.map(expandToDependencies)
      val y = Def.Initialize.join(x)
      y { _.flatten.toSet.toSeq } //make sure all references are unique
    }
    ret2
  }

  //recursively explores the dependency tree of pr and adds all dependencies to the list of projects to be copied
  def expandToDependencies(pr: ProjectReference): Def.Initialize[Seq[ProjectReference]] = {
    val r = (thisProject in pr) { _.dependencies.map(_.project) }
    val r2 = Def.bind(r) { ret ⇒ Def.Initialize.join(ret map expandToDependencies) }
    val r3 = Def.bind(r2) { ret ⇒ r(first ⇒ pr +: ret.flatten) }
    r3
  }

  implicit def ProjRefs2RichProjectSeq(s: Seq[ProjectReference]) = new RichProjectSeq(Def.value(s))

  implicit def InitProjRefs2RichProjectSeq(s: Def.Initialize[Seq[ProjectReference]]) = new RichProjectSeq(s)

  class RichProjectSeq(s: Def.Initialize[Seq[ProjectReference]]) {
    def keyFilter[T](key: SettingKey[T], filter: (T) ⇒ Boolean) = projFilter(s, key, filter)
    def sendTo(to: String) = sendBundles(s, to) //TODO: This function is specific to OSGI bundled projects. Make it less specific?
  }

  /*def projFilter[T](s: Seq[ProjectReference], keyFilter: (SettingKey[T], T ⇒ Boolean)*): Project.Initialize[Seq[ProjectReference]] = {
    require(keyFilter.size >= 1, "Need at least one key/filter pair for a project filter")
    val head = keyFilter.head
    (keyFilter.tail foldLeft projFilter(s, head._1, head._2)) { case (s, (key, filter)) ⇒ projFilter(s, key, filter) }
  }*/

  def projFilter[T](s: Def.Initialize[Seq[ProjectReference]], key: SettingKey[T], filter: T ⇒ Boolean): Project.Initialize[Seq[ProjectReference]] = {
    Def.bind(s)(j ⇒ projFilter(j, key, filter))
  }

  //TODO: New API makes this much simpler
  //val bundles: Seq[FIle] = bundle.all( ScopeFilter( inDependencies(ref) ) ).value

  def sendBundles(bundles: Def.Initialize[Seq[ProjectReference]], to: String): Def.Initialize[Task[Set[(File, String)]]] = Def.bind(bundles) { projs ⇒
    require(projs.nonEmpty)
    val seqOTasks: Def.Initialize[Seq[Task[Set[(File, String)]]]] = Def.Initialize.join(projs.map(p ⇒ bundle in p map { f ⇒
      Set(f -> to)
    }))
    seqOTasks { seq ⇒ seq.reduceLeft[Task[Set[(File, String)]]] { case (a, b) ⇒ a flatMap { i ⇒ b map { _ ++ i } } } }
  }
}
