///*
// * Copyright (C) 2012 reuillon
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//package org.openmole.core.compiler
//
//import org.openmole.core.pluginmanager.{ BundleDecorator, PluginManager }
//import org.osgi.framework.Bundle
//
//import scala.reflect.io.ZipArchive
//import scala.tools.nsc.Global
//import scala.tools.nsc.Settings
//import scala.tools.nsc.interpreter.ReplGlobal
//import scala.tools.nsc.io.AbstractFile
//import scala.tools.nsc.reporters.ConsoleReporter
//import scala.tools.nsc.reporters.Reporter
//import scala.tools.nsc.util._
//import collection.mutable.ListBuffer
//import java.io.{ File, PrintWriter, StringWriter }
//import java.net.URL
//
//object OSGiScalaCompiler {
//
//  def classPath(priorityBundles: Seq[Bundle], jars: Seq[File]) = {
//    def toPath(b: Bundle) = new URL(b.getLocation).getPath
//
//    priorityBundles.map(toPath) ++
//      jars.map(_.getCanonicalPath) ++
//      PluginManager.bundlesForClass(OSGiScalaCompiler.getClass).map(toPath) ++
//      Activator.bundleContext.get.getBundles.filter(!_.isSystem).map(toPath)
//  }.distinct
//
//  def createSettings(settings: Settings, priorityBundles: Seq[Bundle], jars: Seq[File], classDirectory: File) = {
//    val newSettings =
//      if (!Activator.osgi) {
//        val newSettings = settings.copy()
//        case class Plop()
//        newSettings.embeddedDefaults[Plop]
//        //settings.usejavacp.value = true
//        newSettings
//      }
//      else {
//        //def dependencies(b: Bundle) = PluginManager.allDependingBundles(b, _=> true).map(toPath)
//
//        def bundles: Seq[String] = classPath(priorityBundles, jars)
//
//        val newSettings = settings.copy()
//
//        classDirectory.mkdirs()
//
//        newSettings.outputDirs.setSingleOutput(AbstractFile.getDirectory(classDirectory))
//        newSettings.Yreploutdir.value = classDirectory.getAbsolutePath
//        newSettings.classpath.prepend(classDirectory.getCanonicalPath)
//        bundles.foreach(newSettings.classpath.append)
//
//        newSettings.nowarn.value = true
//
//        newSettings
//      }
//
//    newSettings.language.add(newSettings.languageFeatures.postfixOps.name)
//    newSettings.Ydelambdafy.value = "inline"
//    newSettings.YtastyReader.value = true
//
//    //newSettings.maxClassfileName.value = 100
//    newSettings
//  }
//
//  def apply(settings: Settings, reporter: Reporter) =
//    new OSGiScalaCompiler(settings, reporter)
//
//}
//
//class OSGiScalaCompiler private (settings: Settings, reporter: Reporter) extends Global(settings, reporter) with ReplGlobal