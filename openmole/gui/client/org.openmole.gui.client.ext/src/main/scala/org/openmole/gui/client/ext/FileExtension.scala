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
  val OpenMOLEScript = ReadableFileType("oms")
  val OpenMOLEResult = ReadableFileType("omr")
  val MDScript = ReadableFileType("md")
  val SVGExtension = ReadableFileType("svg")
  val OpaqueFileType = org.openmole.gui.client.ext.OpaqueFileType
  val TarGz = ReadableFileType("tgz", "tar.gz")
  val TarXz = ReadableFileType("txz", "tar.xz")
  val Tar = ReadableFileType("tar")
  val Zip = ReadableFileType("zip")
  val Jar = ReadableFileType("jar")
  val CSV = ReadableFileType("csv")
  val NetLogo = ReadableFileType("nlogo", "nlogo3d", "nls")
  val Gaml = ReadableFileType("gaml")
  val R = ReadableFileType("r")
  val Text = ReadableFileType("txt")
  val Scala = ReadableFileType("scala")
  val Scilab = ReadableFileType("sce")
  val Julia = ReadableFileType("jl")
  val Shell = ReadableFileType("sh")
  val Python = ReadableFileType("py")

  def all = Seq(OpenMOLEScript, OpenMOLEResult, MDScript, SVGExtension, TarGz, TarXz, Tar, Zip, Jar, CSV, NetLogo, Gaml, R, Text, Scala, Shell, Python, Scilab, Julia)

  def apply(e: FileExtension) =
    all.find(_.extension.contains(e.value)).getOrElse(OpaqueFileType)

  def isDisplayable(e: FileContentType) =
    e match
      case OpaqueFileType | Jar | Tar | TarGz | Zip | TarXz => false
      case _ => true

  def isText(e: FileContentType) =
    e match
      case R | Text | CSV | Scala | Shell | Python | Gaml | NetLogo | OpenMOLEScript | MDScript | Scilab | Julia => true
      case _ => false


sealed trait FileContentType

object OpaqueFileType extends FileContentType

case class ReadableFileType(extension: String*) extends FileContentType
