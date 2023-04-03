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


case class ModelMetadata(
  inputs: Seq[PrototypePair] = Seq(),
  outputs: Seq[PrototypePair] = Seq(),
  command: Option[String] = None)

case class GeneratedModel(
 content: String,
 name: Option[String] = None)

enum FindLevel:
  case SingleRoot, Root, Level1

case class AcceptedModel(extension: String, level: FindLevel, file: (RelativePath, SafePath))

trait WizardPluginFactory extends GUIPluginFactory:
 def name: String
 def editable: Seq[FileContentType] = Seq()
 def accept(uploaded: Seq[(RelativePath, SafePath)])(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[Seq[AcceptedModel]]
 def parse(uploaded: Seq[(RelativePath, SafePath)], accepted: AcceptedModel)(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[ModelMetadata]
 def content(uploaded: Seq[(RelativePath, SafePath)], acceptedModel: AcceptedModel, modelMetadata: ModelMetadata)(using api: ServerAPI, basePath: BasePath, notificationAPI: NotificationService): Future[GeneratedModel]

