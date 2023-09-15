package org.openmole.gui.client.ext

import org.openmole.gui.shared.data.SafePath

/*
 * Copyright (C) 2023 Romain Reuillon
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


object FileExtension:
  def apply(fileName: String): FileExtension = fileName.dropWhile(_ != '.').drop(1)
  def apply(path: SafePath): FileExtension = FileExtension(path.name)

  extension (e: FileExtension)
    def value: String = e

opaque type FileExtension = String

object FileContentType:
  val OpenMOLEScript = ReadableFileType(Seq("oms"), text = true)
  val OpenMOLEResult = ReadableFileType(Seq("omr"))
  val MDScript = ReadableFileType(Seq("md"), text = true)
  val SVGExtension = ReadableFileType(Seq("svg"))
  val TarGz = ReadableFileType(Seq("tgz", "tar.gz"))
  val TarXz = ReadableFileType(Seq("txz", "tar.xz"))
  val Tar = ReadableFileType(Seq("tar"))
  val Zip = ReadableFileType(Seq("zip"))
  val Jar = ReadableFileType(Seq("jar"))
  val CSV = ReadableFileType(Seq("csv"), text = true)
  val Text = ReadableFileType(Seq("txt"), text = true)
  val Scala = ReadableFileType(Seq("scala"), text = true)

  val Gaml = ReadableFileType(Seq("gaml"), text = true)
  val Scilab = ReadableFileType(Seq("sce"), text = true)
  val Julia = ReadableFileType(Seq("jl"), text = true)
  val Shell = ReadableFileType(Seq("sh"), text = true)
  val Python = ReadableFileType(Seq("py"), text = true)

  def all(using plugins: GUIPlugins): Seq[ReadableFileType] =
    Seq(OpenMOLEScript, OpenMOLEResult, MDScript, SVGExtension, TarGz, TarXz, Tar, Zip, Jar, CSV, Gaml, Text, Scala, Shell, Python, Scilab, Julia) ++
      plugins.wizardFactories.flatMap(_.editable.collect { case r: ReadableFileType => r})


  def apply(path: SafePath)(using plugins: GUIPlugins): FileContentType = apply(FileExtension(path))

  def apply(e: FileExtension)(using plugins: GUIPlugins) =
    all.find(_.extension.contains(e.value)).getOrElse(UnknownFileType)

  def isText(e: ReadableFileType) = e.text

sealed trait FileContentType

object UnknownFileType extends FileContentType
case class ReadableFileType(extension: Seq[String], text: Boolean = false, highlight: Option[String] = None) extends FileContentType
