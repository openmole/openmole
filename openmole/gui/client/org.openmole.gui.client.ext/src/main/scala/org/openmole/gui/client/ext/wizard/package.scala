package org.openmole.gui.client.ext.wizard

import org.openmole.gui.client.ext.*
import org.openmole.gui.shared.data.*

import scala.concurrent.Future


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

object ModelMetadata:
 extension (mmd: ModelMetadata)
   def commandValue = mmd.command.getOrElse("")
   def quotedCommandValue =
     def toMultiLine(s: String) =
       val lines = s.linesIterator.toSeq
       if lines.size > 1
       then
         "\"\"\"" + lines.head + "\n" + lines.tail.map(l => s"|   |$l").mkString("\n") + "\"\"\".stripMargin"
       else s"\"\"\"$s\"\"\""
     toMultiLine(mmd.commandValue)

object PrototypeData:
  sealed trait Type(val name: String, val scalaString: String)
  case object Int extends Type("Integer", "Int")
  case object Double extends Type("Double", "Double")
  case object Long extends Type("Long", "Long")
  case object Boolean extends Type("Boolean", "Boolean")
  case object String extends Type("String", "String")
  case object File extends Type("File", "File")
  case object Char extends Type("Char", "Char")
  case object Short extends Type("Short", "Short")
  case object Byte extends Type("Byte", "Byte")
  case class Any(override val name: String, override val scalaString: String) extends Type(name, scalaString)

case class PrototypeData(name: String, `type`: PrototypeData.Type, default: String = "", mapping: Option[String] = None)

case class ModelMetadata(
  inputs: Seq[PrototypeData] = Seq(),
  outputs: Seq[PrototypeData] = Seq(),
  command: Option[String] = None)

case class GeneratedModel(
 content: String,
 name: Option[String] = None,
 directory: Option[String] = None)

enum FindLevel:
  case SingleFile, MultipleFile, Directory

case class AcceptedModel(extension: String, level: FindLevel, file: List[(RelativePath, SafePath)])

trait WizardPluginFactory extends GUIPluginFactory:
 def name: String
 def editable: Seq[FileContentType] = Seq()
 def accept(uploaded: Seq[(RelativePath, SafePath)])(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[Seq[AcceptedModel]]
 def parse(uploaded: Seq[(RelativePath, SafePath)], accepted: AcceptedModel)(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[ModelMetadata]
 def content(uploaded: Seq[(RelativePath, SafePath)], accepted: AcceptedModel, modelMetadata: ModelMetadata)(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[GeneratedModel]

