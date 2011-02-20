/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.misc.pluginmanager

import java.io.File
import java.io.FileFilter
import org.apache.commons.collections15.bidimap.DualHashBidiMap
import org.openmole.commons.exception.InternalProcessingError
import org.openmole.misc.pluginmanager.internal.Activator
import org.osgi.framework.Bundle
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import org.osgi.framework.BundleException
import scala.collection.JavaConversions._

object PluginManager {
  private val defaultPatern = ".*\\.jar";
    
  private val files = new DualHashBidiMap[Bundle, File]
  private val resolvedBundlesDirectDependencies = new HashMap[Bundle, Iterable[Bundle]]
  private val resolvedBundlesAllDependencies = new HashMap[Bundle, Iterable[Bundle]]

  def load(path: File): Bundle = synchronized {
    try {
      val b = installBundle(path)

      b match {
        case Some(bundle) =>
          if (!resolvedBundlesDirectDependencies.containsKey(bundle)) {
            bundle.start
            resolvedBundlesDirectDependencies += bundle -> getDirectDependencieBundles(bundle)
          }
          bundle
        case None => getBundleForFile(path)
      }
    } catch {
      case ex: BundleException => throw new InternalProcessingError(ex, "Installing " + path.getAbsolutePath() + " " + ex.getLocalizedMessage());
    }
  }

  def loadDir(path: File, pattern: String): Iterable[Bundle] = {
    loadDir(path, new FileFilter {
        override def accept(file: File): Boolean = {
          file.isFile && file.exists && file.getName().matches(pattern)
        }
      })
  }

  def loadDir(path: File, fiter: FileFilter): Iterable[Bundle] = synchronized {
    if (!path.exists || !path.isDirectory) return Iterable.empty

    val bundlesRet = new ListBuffer[Bundle]
    val bundles = new ListBuffer[Bundle]

    for (f <- path.listFiles(fiter)) {
      var b = installBundle(f)

      if (b.isDefined && !resolvedBundlesDirectDependencies.containsKey(b.get)) {
        bundles.add(b.get)
        resolvedBundlesDirectDependencies.put(b.get, Iterable.empty)
      }
                
      if (b.isDefined) bundlesRet.add(b.get)
      else bundlesRet.add(getBundleForFile(f))
    }

    for (b <- bundles) {
      try b.start
      catch {
        case ex: BundleException => throw new InternalProcessingError(ex, "Installing " + b.getLocation() + " " + ex.getLocalizedMessage());
      }
    }

    for (b <- bundles) {
      resolvedBundlesDirectDependencies.put(b, getDirectDependencieBundles(b))
    }

    bundlesRet
  }

  private def getDirectDependencieBundles(b: Bundle): Set[Bundle] = {
    //TODO find a way to do that faster
    (for (bundle <- resolvedBundlesDirectDependencies.keySet; ib <- getDependingBundles(bundle) ; if (ib.equals(b))) yield bundle).toSet
  }

  def getResolvedDirectDependencies(b: Bundle): Iterable[Bundle] = synchronized {
    resolvedBundlesDirectDependencies.getOrElse(b, Iterable.empty)
  }

  def getAllDependencies(b: Bundle): Iterable[Bundle] = synchronized {
    resolvedBundlesAllDependencies.getOrElseUpdate(b, {
        val deps = new HashSet[Bundle]
        val toProced = new ListBuffer[Bundle]

        toProced += b

        while (!toProced.isEmpty) {
          val cur = toProced.remove(0)

          for (dep <- getResolvedDirectDependencies(cur)) {
            if (!deps.contains(dep)) {
              deps.add(dep)
              toProced += dep
            }
          }
        }
        deps
      })
    
  }

  def getPluginForClass(c: Class[_]): File = synchronized {
    val b = Activator.packageAdmin.getBundle(c)
    files.get(b)
  }

  def getPluginAndDependanciesForClass(c: Class[_]): Iterable[File] = synchronized {
    val b = Activator.packageAdmin.getBundle(c)
    if (b == null) return Iterable.empty

    val ret = new ListBuffer[File]

    val plugin = files.get(b)
    if (plugin != null) ret += plugin

    for (dep <- getAllDependencies(b)) {
      val depPlugin = files.get(dep);
      if (depPlugin != null) ret += depPlugin
    }

    ret
  }

  def load(path: String): Bundle = load(new File(path))

  def isClassProvidedByAPlugin(c: Class[_]): Boolean = resolvedBundlesDirectDependencies.containsKey(Activator.packageAdmin.getBundle(c))

  def unload(path: File) = synchronized {
    val b = getBundle(path)
    for (db <- getDependingBundles(b)) unloadBundle(db);

    unloadBundle(b)
  }

  private def unloadBundle(bundle: Bundle ) = {
    if (bundle != null) {
      bundle.uninstall
      files.remove(bundle)
      resolvedBundlesDirectDependencies.remove(bundle)
    }
  }

  def getBundle(path: File): Bundle = synchronized(files.getKey(path))

  private def getDependingBundles(b: Bundle): Iterable[Bundle] = {
    val exportedPackages = Activator.packageAdmin.getExportedPackages(b)

    if (exportedPackages != null) {
      for (exportedPackage <- exportedPackages ; ib <- exportedPackage.getImportingBundles) yield ib 
    } else Iterable.empty
  }

  def unload(path: String): Unit = unload(new File(path))

  def loadDir(path: File): Iterable[Bundle] = loadDir(path, defaultPatern)

  def loadDir(path: String): Iterable[Bundle] = loadDir(new File(path))
    
  private def getBundleForFile(f: File): Bundle = files.getKey(f.getAbsoluteFile)
    
  private def installBundle(f: File): Option[Bundle] = {
    val file = f.getAbsoluteFile
        
    if (!files.containsValue(file)) {
      val ret = Activator.context.getOrElse(throw new InternalProcessingError("Context has not been initialized")).installBundle(file.toURI.toString)
      files.put(ret, file);
      Some(ret)
    } else None
  }
}
