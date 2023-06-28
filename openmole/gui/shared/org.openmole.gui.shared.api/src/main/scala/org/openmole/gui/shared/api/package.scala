package org.openmole.gui.shared.api

/*
 * Copyright (C) 2022 Romain Reuillon
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

import org.openmole.gui.shared.data.*

val connectionRoute = "connection"
val shutdownRoute = "shutdown"
val restartRoute = "restart"

val appRoute = "app"

val downloadFileRoute = "downloadFile"
val uploadFilesRoute = "uploadFiles"
val resetPasswordRoute = "resetPassword"

val convertOMRRoute = "file/omr/convert"

val fileTypeParam = "fileType"
val pathParam = "path"
val hashParam = "hash"

def hashHeader = "Content-Hash"

def downloadFile(uri: String, fileType: ServerFileSystemContext, hash: Boolean = false) =
  s"$downloadFileRoute?$pathParam=$uri&$hashParam=$hash&$fileTypeParam=${fileType.typeName}"

def convertOMR(uri: String, fileType: ServerFileSystemContext) =
  s"$convertOMRRoute?$pathParam=$uri&$fileTypeParam=${fileType.typeName}"


trait RESTAPI extends endpoints4s.algebra.Endpoints with endpoints4s.algebra.circe.JsonEntitiesFromCodecs with endpoints4s.circe.JsonSchemas:
   export io.circe.generic.auto.*
   type ErrorEndpoint[I, O] = Endpoint[I, Either[ErrorData, O]]
   def errorEndpoint[A, B](request: Request[A], r: Response[B], docs: EndpointDocs = EndpointDocs()) =
     endpoint(request, response(InternalServerError, jsonResponse[ErrorData]).orElse(r), docs)


