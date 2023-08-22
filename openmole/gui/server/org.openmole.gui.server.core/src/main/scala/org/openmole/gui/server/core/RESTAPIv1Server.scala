package org.openmole.gui.server.core

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


import cats.effect.IO

import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

import scala.util.{Failure, Success, Try}
import org.openmole.gui.server.ext.utils.{HTTP, fileToSafePath}
import io.circe.derivation
import io.circe.generic.semiauto.*
import org.openmole.gui.shared.data.TreeNodeData

import org.http4s
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*


object RESTAPIv1Server:
  implicit val circeDefault: _root_.io.circe.derivation.Configuration =
    _root_.io.circe.derivation.Configuration.default.withKebabCaseMemberNames.withDefaults.withDiscriminator("type").withTransformConstructorNames(_.toLowerCase)

  object FileEntry:
    def fromTreeNodeData(t: TreeNodeData) =
      t.directory match
        case None =>
          FileEntry.File(
            name = t.name,
            size = t.size,
            modified = t.time
          )
        case Some(_) =>
          FileEntry.Directory(
            name = t.name,
            modified = t.time
          )

    case class File(name: String, size: Long, modified: Long) extends FileEntry
    case class Directory(name: String, modified: Long) extends FileEntry
    
  sealed trait FileEntry derives derivation.ConfiguredCodec

  implicit class ToJsonDecorator[T: _root_.io.circe.Encoder](x: T):
    def toJson =
      import _root_.io.circe.*
      import _root_.io.circe.syntax.*
      x.asJson.deepDropNullValues.spaces2


  object PathParam extends OptionalQueryParamDecoderMatcher[String]("path")
  object ListParam extends FlagQueryParamMatcher("list")

class RESTAPIv1Server(impl: ApiImpl):
  import impl.services.*
  import RESTAPIv1Server.*
  import org.openmole.gui.shared.data.ServerFileSystemContext.Project

  val routes: HttpRoutes[IO] =
    HttpRoutes.of:
      case req @ GET -> root / "files" :? PathParam(path) +& ListParam(list) if list =>
        def listing = impl.listFiles(fileToSafePath(path.getOrElse(""))).data.map(FileEntry.fromTreeNodeData)
        Ok(listing.toJson)

