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
import java.net.URL
import org.apache.commons.collections15.bidimap.DualHashBidiMap
import org.openmole.misc.exception.InternalProcessingError
import org.openmole.misc.pluginmanager.internal.Activator
import org.osgi.framework.Bundle
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import scala.collection.mutable.ListBuffer
import org.osgi.framework.BundleEvent
import org.osgi.framework.BundleException
import org.osgi.framework.BundleListener
import scala.collection.JavaConversions._

object PluginManager {

  val OpenMOLEscope = "OpenMOLE-scope"
  private implicit def bundleDecorator(b: Bundle) = new {
    
    def isSystem = b.getLocation.toLowerCase.contains("system bundle")
    
    def isProvided = b.getHeaders.get(OpenMOLEscope) match {
      case null => false
      case s: String => s.toLowerCase == "provided"
      case _ => false
    }
    
    def file = {
      val location = if(b.getLocation.startsWith("reference:")) 
        b.getLocation.substring("reference:".length)
      else  if(b.getLocation.startsWith("initial@reference:")) b.getLocation.substring("initial@reference:".length)
      else b.getLocation
      
      new File(new URL(if(location.endsWith("/")) location.substring(0, location.length - 1) else location).getFile)
    }
  }
  
  {
    //Activator.packageAdmin.resolveBundles(null)
    updateDependencies
    
    /*Activator.contextOrException.getBundles.foreach {
     b => if(!b.isSystem) println(b.file.getAbsolutePath + " " + b.isProvided)
     }*/
    
    Activator.contextOrException.addBundleListener(new BundleListener {
        override def bundleChanged(event: BundleEvent) = {
          val b = event.getBundle
          if(event.getType == BundleEvent.RESOLVED || event.getType == BundleEvent.UNRESOLVED || event.getType == BundleEvent.UPDATED) {
            updateDependencies
            //println("RESOLVED " + b.file.getAbsolutePath + " " + b.isProvided)
          }
        } 
      })
  }
  
  private val defaultPatern = ".*\\.jar";

  private var files = Map.empty[File, Bundle]
  private var resolvedDirectDependencies = HashMap.empty[Bundle, ListBuffer[Bundle]]
  private var resolvedPluginDependenciesCache = HashMap.empty[Bundle, Iterable[Bundle]]
  private var providedDependencies = Set.empty[Bundle]

  def isClassProvidedByAPlugin(c: Class[_]) = {
    val b = Activator.packageAdmin.getBundle(c)
    if(b != null) !providedDependencies.contains(b)
    else false
  }

  def pluginsForClass(c: Class[_]): Iterable[File] = synchronized {
    allPluginDependencies(Activator.packageAdmin.getBundle(c)).map{_.file}
  }

  def load(path: File): Unit = synchronized { installBundle(path).foreach{_.start} }

  def loadDir(path: String): Unit = loadDir(new File(path))
  def loadDir(path: File): Unit = loadDir(path, defaultPatern)
  def loadDir(path: File, pattern: String): Unit = {
    loadDir(path, new FileFilter {
        override def accept(file: File): Boolean = {
          file.isFile && file.exists && file.getName().matches(pattern)
        }
      })
  }

  def loadDir(path: File, fiter: FileFilter): Unit = synchronized {
    if (path.exists && path.isDirectory) {
      val bundles = path.listFiles(fiter).map{f => installBundle(f)}.toList
      bundles.foreach{_.foreach{_.start}}
    }
  }

  def bundle(file: File) = files.get(file.getAbsoluteFile)
  
  private def dependencies(bundles: Iterable[Bundle]): Iterable[Bundle] = {
    //val filtredBundles =  bundles.filter(predicate)
    val ret = HashSet.empty[Bundle]
    val filtred = HashSet.empty[Bundle]
    var toProceed = ListBuffer.empty[Bundle] ++ bundles
    
    while(!toProceed.isEmpty) {
      val cur = toProceed.remove(0)
      ret += cur
      toProceed ++= resolvedDirectDependencies.getOrElse(cur, Iterable.empty).filter(b => !ret.contains(b))
    }
    
    ret
  }

  private def allPluginDependencies(b: Bundle) = synchronized {
    resolvedPluginDependenciesCache.getOrElseUpdate(b,dependencies(List(b)).filter(b => !providedDependencies.contains(b)))
  }

  private def installBundle(f: File) = {
    val file = f.getAbsoluteFile

    if (!files.contains(file)) {
      val ret = Activator.contextOrException.installBundle(file.toURI.toString)
      files += file -> ret
      Some(ret)
    } else None
  }

  private def updateDependencies = synchronized {
    val bundles = Activator.contextOrException.getBundles.filter(!_.isSystem)
    
    val resolvedDirectDependenciesTmp = new HashMap[Bundle, ListBuffer[Bundle]]
    bundles.foreach {
      b => dependingBundles(b).foreach {
        db => {
          //println(db + " depend on " + b)
          resolvedDirectDependenciesTmp.getOrElseUpdate(db, new ListBuffer[Bundle]) += b
        }
      }
    }
    
    resolvedDirectDependencies = resolvedDirectDependenciesTmp
    resolvedPluginDependenciesCache = HashMap.empty[Bundle, Iterable[Bundle]]
    
    providedDependencies = dependencies(bundles.filter(b => b.isProvided)).toSet
    files = bundles.map(b => b.file.getAbsoluteFile -> b).toMap
  }
  
  private def dependingBundles(b: Bundle): Iterable[Bundle] = {
    val exportedPackages = Activator.packageAdmin.getExportedPackages(b)

    if (exportedPackages != null) {
      for (exportedPackage <- exportedPackages ; ib <- exportedPackage.getImportingBundles) yield ib 
    } else Iterable.empty
  }
  
  def load(path: String): Unit = load(new File(path))

}
