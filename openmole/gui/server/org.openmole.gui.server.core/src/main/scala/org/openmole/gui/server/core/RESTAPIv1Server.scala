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
import org.http4s
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.*
import org.http4s.implicits.*
import org.http4s.multipart.Multipart
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.core.fileservice.FileServiceCache
import org.openmole.core.omr.OMR
import org.openmole.core.project.*
import org.openmole.core.workflow.mole.MoleServices

import java.io.PrintStream

import scala.util.{Failure, Success, Try}
import java.util.UUID
import java.util.zip.GZIPInputStream
import org.openmole.tool.stream.*
import org.openmole.tool.archive.*
import org.openmole.gui.server.ext.utils.{HTTP, fileToSafePath}
import io.circe.generic.auto.*
object RESTAPIv1Server:
  object FileEntry:
    case class File() extends FileEntry
    case class Directory() extends FileEntry 
    
  sealed trait FileEntry
    

class RESTAPIv1Server(impl: ApiImpl):
  import impl.services.*
  import org.openmole.gui.shared.data.ServerFileSystemContext.Project

  implicit class ToJsonDecorator[T: io.circe.Encoder](x: T):
    def toJson =
      import io.circe.*
      import io.circe.syntax.*
      x.asJson.deepDropNullValues.spaces2

  val routes: HttpRoutes[IO] =
    HttpRoutes.of:
      case req @ GET -> "list" /: rest =>
        val path = rest.segments.drop(1).map(_.decoded()).mkString("/")
        Ok(impl.listFiles(fileToSafePath(path)).toJson)

