/*
 * Copyright (C) 2011 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.pluginmanager

import java.io.File
import java.util.concurrent.Semaphore
import java.util.zip.ZipFile

import org.openmole.tool.file._
import org.openmole.tool.logger.JavaLogger
import org.openmole.tool.hash.*
import org.osgi.framework._
import org.osgi.framework.wiring.{ BundleWiring, FrameworkWiring }

import scala.collection.immutable.{ HashMap, HashSet }
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters._
import scala.util.{ Failure, Success, Try }

case class BundlesInfo(
  files:                Map[File, (Long, Long)],
  providedDependencies: Set[Long]
):
  lazy val hashes = files.keys.map(f => f -> Hash.file(f)).toMap


object PluginManager extends JavaLogger {

  import Log._

  private var bundlesInfo: Option[BundlesInfo] = None
  private val resolvedPluginDependenciesCache = mutable.Map[Long, Iterable[Long]]()

  private[pluginmanager] def clearCaches() = PluginManager.synchronized:
    bundlesInfo = None
    resolvedPluginDependenciesCache.clear()

  def allPluginDependencies(b: Bundle) = PluginManager.synchronized:
    resolvedPluginDependenciesCache.
      getOrElseUpdate(b.getBundleId, dependencies(List(b)).map(_.getBundleId)).
      filter(isPlugin).map(l => Activator.contextOrException.getBundle(l))

  private def installBundle(f: File) = PluginManager.synchronized:
    val bundle = Activator.contextOrException.installBundle(f.toURI.toString)
    bundlesInfo = None
    bundle

  private def infos: BundlesInfo = PluginManager.synchronized:
    bundlesInfo match
      case None =>
        val bs = bundles
        val providedDependencies = dependencies(bs.filter(b => b.isProvided)).map(_.getBundleId).toSet
        val files = bs.map(b => b.file.getCanonicalFile → ((b.getBundleId, b.file.lastModification))).toMap
        val info = BundlesInfo(files, providedDependencies)
        bundlesInfo = Some(info)
        info
      case Some(bundlesInfo) => bundlesInfo


  def updateBundles(bundles: Option[Seq[Bundle]] = None) = {
    val listener = new FrameworkListener {
      val lock = new Semaphore(0)

      override def frameworkEvent(event: FrameworkEvent): Unit = {
        if (event.getType == FrameworkEvent.PACKAGES_REFRESHED) lock.release()
      }
    }

    val wiring = Activator.contextOrException.getBundle(0).adapt(classOf[FrameworkWiring])

    bundles match {
      case Some(s) => wiring.refreshBundles(s.asJava, listener)
      case None    => wiring.refreshBundles(null, listener)
    }

    // FIX: The listener is not called by the framework, enable at some point
    // listener.lock.acquire()
  }

  def bundles = Activator.contextOrException.getBundles.filter(!_.isSystem).toSeq.sortBy(_.file.getName)
  def bundleFiles = infos.files.keys

  def dependencies(file: File): Option[Iterable[File]] =
    infos.files.get(file).map { case (id, _) => allPluginDependencies(Activator.contextOrException.getBundle(id)).map { _.file } }

  def isClassProvidedByAPlugin(c: Class[?]) = 
    val b = FrameworkUtil.getBundle(c)
    if (b != null) !infos.providedDependencies.contains(b.getBundleId())
    else false

  def fileProviding(c: Class[?]) =
    bundleForClass(c).map(b => Activator.contextOrException.getBundle(b.getBundleId).file.getCanonicalFile)

  def bundleForClass(c: Class[?]): Option[Bundle] =
    Option(FrameworkUtil.getBundle(c))

  def bundlesForClass(c: Class[?]): Iterable[Bundle] = {
    val bundle = bundleForClass(c).toSeq
    bundle.flatMap(allDependencies)
  }

  def pluginsForClass(c: Class[?]): Iterable[File] = {
    val bundle = bundleForClass(c)
    bundle.toSeq.flatMap(allPluginDependencies).map(_.file)
  }

  def allDepending(file: File, filter: Bundle => Boolean): Iterable[File] =
    bundle(file) match {
      case Some(b) => allDependingBundles(b, filter).map { _.file }
      case None    => Iterable.empty
    }

  def isBundle(file: File): Boolean =
    import scala.util.Using
    def isJar(file: File): Boolean =
      if file.getName.endsWith(".jar")
      then
        Using(new ZipFile(file)) { zip =>
          val manifest = zip.getEntry("META-INF/MANIFEST.MF")
          if manifest != null
          then
            Using(scala.io.Source.fromInputStream(zip.getInputStream(manifest))) { src =>
              src.getLines().exists(_.trim.startsWith("Bundle"))
            }.get
          else false
        }.getOrElse(false)
      else false

    def isDirectoryPlugin(file: File) =
      val manifest = file./("META-INF")./("MANIFEST.MF")
      file.isDirectory && manifest.exists && manifest.lines.exists(_.trim.startsWith("Bundle"))
    isDirectoryPlugin(file) || (!file.isDirectory && isJar(file))

  def listBundles(path: File): Iterable[File] =
    if (isBundle(path)) List(path)
    else if (path.isDirectory) path.listFilesSafe.filter(isBundle)
    else Nil

  def tryLoad(files: Iterable[File]): Iterable[(File, Throwable)] = synchronized:
    val bundleFiles = files.flatMap { listBundles }
    val loaded = bundleFiles.map { b => b → Try(installBundle(b)) }
    def bundles = loaded.collect { case (f, Success(b)) => f → b }
    def loadError = loaded.collect { case (f, Failure(e)) => f → e }
    loadError ++ bundles.flatMap:
      case (f, b) =>
        Try(b.start) match
          case Success(_) => None
          case Failure(e) =>
            b.uninstall()
            Some(f → e)

  def load(files: Iterable[File]) = synchronized {
    val bundles = files.flatMap { listBundles }.map { installBundle }.toList
    bundles.foreach {
      b =>
        logger.fine(s"Stating bundle ${b.getLocation}")
        b.start
    }
    bundles
  }

  def loadIfNotAlreadyLoaded(plugins: Iterable[File]) = synchronized:
    val bundles = plugins.filterNot(f => infos.files.contains(f)).map(installBundle).toList
    bundles.foreach { _.start }

  def load(path: File): Unit = load(List(path))

  def loadDir(path: File): Unit =
    if (path.exists && path.isDirectory) load(listBundles(path))

  def bundle(file: File) = infos.files.get(file.getCanonicalFile).map { id => Activator.contextOrException.getBundle(id._1) }

  private def allDependencies(b: Bundle) = dependencies(List(b))

  def isPlugin(b: Bundle): Boolean = isPlugin(b.getBundleId)
  def isPlugin(id: Long): Boolean = !infos.providedDependencies.contains(id)

  def isOSGI(file: File) = {
    Try {
      val zip = new ZipFile(file)
      val hasSymbolicName =
        try {
          val manifest = zip.getEntry("META-INF/MANIFEST.MF")
          if (manifest != null) {
            val content = scala.io.Source.fromInputStream(zip.getInputStream(manifest)).getLines.mkString("\n")
            content.contains(Constants.BUNDLE_SYMBOLICNAME)
          }
          else false
        }
        finally zip.close
      hasSymbolicName
    }.getOrElse(false)
  }

  def remove(b: Bundle) = synchronized {
    val additionalBundles = Seq(b) ++ allDependingBundles(b, b => !b.isProvided)
    additionalBundles.foreach(b => if (b.getState == Bundle.ACTIVE) b.uninstall())
    updateBundles()
  }

  private def getBundle(f: File) =
    Option(Activator.contextOrException.getBundle(f.toURI.toString))

  private def dependencies(bundles: Iterable[Bundle]): Iterable[Bundle] = {
    val seen = mutable.Set[Bundle]() ++ bundles
    var toProceed = new ListBuffer[Bundle] ++ bundles

    while (!toProceed.isEmpty) {
      val current = toProceed.remove(0)
      for {
        b ← directDependencies(current)
      } if (!seen(b)) {
        seen += b
        toProceed += b
      }
    }
    seen.toList
  }

  def allDependingBundles(b: Bundle, filter: Bundle => Boolean): Iterable[Bundle] = {
    val seen = mutable.Set[Bundle]()
    val toProcess = ListBuffer[Bundle]()

    toProcess += b
    seen += b

    while (!toProcess.isEmpty) {
      val current = toProcess.remove(0)
      for {
        b ← directDependingBundles(current).filter(filter)
      } if (!seen(b)) {
        seen += b
        toProcess += b
      }
    }

    seen.toList
  }

  def directDependencies(b: Bundle) =
    if (b.isFullDynamic) List.empty
    else {
      val bundles =
        for {
          wires ← Option(b.adapt(classOf[BundleWiring]))
          requiered ← Option(wires.getRequiredWires(null).asScala).map(_.filter(_ != null))
          bundles = requiered.flatMap(w => Option(w.getProvider)).flatMap(p => Option(p.getBundle))
        } yield bundles.filter(_.getBundleId != Constants.SYSTEM_BUNDLE_ID).distinct

      bundles.map(_.toSeq).getOrElse(Seq.empty)
    }

  def directDependingBundles(b: Bundle) =
    b.adapt(classOf[BundleWiring]).
      getProvidedWires(null).asScala.
      map(_.getRequirer.getBundle).
      filter(b => b.getBundleId != Constants.SYSTEM_BUNDLE_ID && !b.isFullDynamic).
      distinct

  def startAll: Seq[(Bundle, Throwable)] =
    Activator.contextOrException.getBundles.filter {
      _.getState match {
        case Bundle.INSTALLED | Bundle.RESOLVED | Bundle.STARTING => true
        case _ => false
      }
    }.map {
      b => b → Try(b.start)
    }.collect { case (b: Bundle, Failure(e)) => b → e }

  def bundleHashes = infos.hashes.values

  /* For debugging purposes */
  def printBundles = println(Activator.contextOrException.getBundles.mkString("\n"))
  def printDirectDepending(b: Long) = println(directDependingBundles(Activator.contextOrException.getBundle(b)).mkString("\n"))
  def printDirectDependencies(b: Long) = println(directDependencies(Activator.contextOrException.getBundle(b)).mkString("\n"))
  def printIsPlugin(b: Long) = println(isPlugin(b))
  def printPluginsForClass(c: Class[?]) = println(pluginsForClass(c).mkString("\n"))

}
