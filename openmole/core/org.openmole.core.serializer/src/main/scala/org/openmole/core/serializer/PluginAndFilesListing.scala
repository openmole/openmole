/*
 * Copyright (C) 02/10/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.serializer

import java.io.File
import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.converters.reflection.ReflectionConverter
import org.openmole.core.pluginmanager.PluginManager
import org.openmole.core.serializer.file.{FileConverterNotifier, FileWithGCConverter}
import org.openmole.core.serializer.plugin.{PluginClassConverter, PluginConverter}
import org.openmole.core.serializer.structure.PluginClassAndFiles
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.file.*
import org.openmole.tool.stream.NullOutputStream

import scala.collection.immutable.{HashSet, TreeSet}

object PluginAndFilesListing:
  def looksLikeREPLClassName(p: String) = p.startsWith("$line")

class FilesListing(xStream: XStream):
  private var listedFiles: TreeSet[File] = null
  xStream.registerConverter(new FileConverterNotifier(fileUsed))

  def fileUsed(file: File) = listedFiles += file

  def list(obj: Any) = synchronized:
    listedFiles = TreeSet[File]()(fileOrdering)
    xStream.toXML(obj, new NullOutputStream())
    val retFile = listedFiles
    listedFiles = null
    retFile.toVector


class PluginAndFilesListing(xStream: XStream):

  lazy val reflectionConverter: ReflectionConverter =
    new ReflectionConverter(xStream.getMapper, xStream.getReflectionProvider)

  private var plugins: TreeSet[File] = null
  private var listedFiles: TreeSet[File] = null
  private var seenClasses: HashSet[Class[?]] = null
  private var replClasses: HashSet[Class[?]] = null

  xStream.registerConverter(new FileConverterNotifier(fileUsed))
  xStream.registerConverter(new PluginConverter(this, reflectionConverter))
  xStream.registerConverter(new PluginClassConverter(this))

  def classUsed(c: Class[?]) =
    if !seenClasses.contains(c)
    then
      PluginManager.pluginsForClass(c).foreach(pluginUsed)

      if Option(c.getName).map(PluginAndFilesListing.looksLikeREPLClassName).getOrElse(false) && !PluginManager.bundleForClass(c).isDefined
      then replClasses += c

      seenClasses += c

  def pluginUsed(f: File) = plugins += f

  def fileUsed(file: File) = listedFiles += file

  def list(obj: Any) = synchronized:
    plugins = TreeSet[File]()(fileOrdering)
    listedFiles = TreeSet[File]()(fileOrdering)
    seenClasses = HashSet()
    replClasses = HashSet()
    xStream.toXML(obj, new NullOutputStream())
    val retPlugins = plugins
    val retFile = listedFiles
    val retReplClasses = replClasses
    seenClasses = null
    plugins = null
    listedFiles = null
    replClasses = null
    PluginClassAndFiles(retFile.toVector, retPlugins.toVector, retReplClasses.toVector)


