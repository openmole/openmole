/*
 * Copyright (C) 2012 Romain Reuillon
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

package org.apache.clerezza.scala.scripting.util

import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.HashMap
import scala.tools.nsc.io.AbstractFile
import scala.tools.nsc.util.ClassPath
import scala.tools.nsc.util.ClassPath._
import scala.tools.nsc.io.File.pathSeparator
import scala.tools.nsc.util.MergedClassPath

class DynamicMergedClassPath[T](var mergeClassPath: MergedClassPath[T]) extends ClassPath[T] {

  def name = mergeClassPath.name
  def asURLs = mergeClassPath.asURLs
  def context = mergeClassPath.context
  def sourcepaths: IndexedSeq[AbstractFile] = mergeClassPath.sourcepaths

  override def origin = mergeClassPath.origin
  override def asClasspathString: String = mergeClassPath.asClasspathString

  def classes: IndexedSeq[AnyClassRep] = mergeClassPath.classes

  def packages: IndexedSeq[ClassPath[T]] = mergeClassPath.packages

  def show() {
    mergeClassPath.show
  }

  override def toString() = mergeClassPath.toString
}

