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
import org.openmole.gui.shared.data.GUIOMRContent.ExportFormat

val connectionRoute = "connection"
val shutdownRoute = "shutdown"
val restartRoute = "restart"

val appRoute = "app"

val downloadFileRoute = "downloadFile"
val uploadFilesRoute = "uploadFiles"
val resetPasswordRoute = "resetPassword"

val convertOMRRoute = "file/omr/csv"

val fileTypeParam = "fileType"
val pathParam = "path"
val formatParam = "format"
val historyParam = "history"

object Download:
  val hashParam = "hash"
  val fileNameParam = "name"
  val topDirectoryParam = "topDirectory"

def hashHeader = "Content-Hash"

object Util:
  def toURI(path: Seq[String]): String = new java.net.URI(null, null, path.mkString("/"), null).toString

  def encode(s: String): String =
    val char =
      for
        c ← s
      yield
        c match
          case '+' => "%2b"
          case ':' => "%3a"
          case '/' => "%2f"
          case '?' => "%3f"
          case '#' => "%23"
          case '[' => "%5b"
          case ']' => "%5d"
          case '@' => "%40"
          case '!' => "%21"
          case '$' => "%24"
          case '&' => "%26"
          case '\'' => "%27"
          case '(' => "%29"
          case ')' => "%28"
          case '*' => "%2a"
          case ',' => "%2c"
          case ';' => "%3b"
          case '=' => "%3d"
          case _ => c
    char.mkString("")

def safePathToURLParams(sp: SafePath) =
  import Download.*
  val uri = Util.toURI(sp.path.value.map(Util.encode))
  val fileType = sp.context
    Seq(
      s"$pathParam=$uri",
      s"$fileTypeParam=${fileType.typeName}")

def downloadFiles(sp: Seq[SafePath], name: Option[String] = None) =
  import Download.*
  val params = sp.flatMap(safePathToURLParams) ++ name.map(n => s"$fileNameParam=$n")
  s"$downloadFileRoute?${params.mkString("&")}"

def downloadFile(sp: SafePath, hash: Boolean = false, name: Option[String] = None, includeTopDirectoryInArchive: Option[Boolean] = None) =
  import Download.*
  val params = safePathToURLParams(sp) ++ includeTopDirectoryInArchive.map(d => s"$topDirectoryParam=$d") ++ name.map(n => s"$fileNameParam=$n") ++ Seq(s"$hashParam=$hash")
  s"$downloadFileRoute?${params.mkString("&")}"

def convertOMR(sp: SafePath, format: GUIOMRContent.ExportFormat, history: Boolean = false) =
  def params = safePathToURLParams(sp) ++ Seq(s"$formatParam=${GUIOMRContent.ExportFormat.toString(format)}", s"$historyParam=$history")
  s"$convertOMRRoute?${params.mkString("&")}"

trait RESTAPI extends endpoints4s.algebra.Endpoints with endpoints4s.algebra.circe.JsonEntitiesFromCodecs with endpoints4s.circe.JsonSchemas:
   export io.circe.generic.auto.*
   type ErrorEndpoint[I, O] = Endpoint[I, Either[ErrorData, O]]
   def errorEndpoint[A, B](request: Request[A], r: Response[B], docs: EndpointDocs = EndpointDocs()) =
     endpoint(request, response(InternalServerError, jsonResponse[ErrorData]).orElse(r), docs)


