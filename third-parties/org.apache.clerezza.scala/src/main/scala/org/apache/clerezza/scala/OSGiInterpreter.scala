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
//package org.apache.clerezza.scala
//
//import scala.tools.nsc.interpreter._
//import scala.tools.nsc.util._
//import scala.reflect.runtime._
//import scala.tools.util._
//import org.osgi.framework._
//
//object OSGiInterpreter {
//
//  def init(interpreter: IMain) = {
//
//  }
//
//  def initClassPath(interpreter: IMain) = {
//    val classPathOrig = new PathResolver(interpreter.settings).result
//    var bundles: Array[Bundle] = Activator.bundleContext.getBundles
//    val classPathAbstractFiles =
//      for (bundle ← bundles; val url = bundle.getResource("/"); if url != null) yield {
//        if ("file".equals(url.getProtocol())) new PlainFile(new File(url.toURI()))
//        else BundleFS.create(bundle);
//      }
//    val classPaths: List[ClassPath[AbstractFile]] =
//      (for (abstractFile ← classPathAbstractFiles) yield {
//        new DirectoryClassPath(abstractFile, classPathOrig.context)
//      }) toList
//
//    new MergedClassPath[AbstractFile](classPathOrig :: classPaths, classPathOrig.context)
//  }
//
//}
