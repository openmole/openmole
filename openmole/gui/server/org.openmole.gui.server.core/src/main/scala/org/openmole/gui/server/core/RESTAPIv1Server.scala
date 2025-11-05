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
import cats.effect.unsafe.IORuntime
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*

import scala.util.{Failure, Success, Try}
import org.openmole.gui.server.ext.utils.{HTTP, fileToSafePath}
import io.circe.derivation
import io.circe.generic.semiauto.*
import org.openmole.gui.shared.data
import org.http4s
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.http4s.multipart.Multipart

object RESTAPIv1Server:
  implicit val circeDefault: _root_.io.circe.derivation.Configuration =
    _root_.io.circe.derivation.Configuration.default.withKebabCaseMemberNames.withDefaults.withDiscriminator("type").withTransformConstructorNames(_.toLowerCase)

  object FileEntry:
    def fromTreeNodeData(t: data.TreeNodeData) =
      t.directory match
        case None =>
          FileEntry.File(
            name = t.name,
            size = t.size.getOrElse(0L),
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


  object Execution:
    object State:
      import _root_.io.circe.*
      given Encoder[State] = v => Encoder.encodeString(v.toString)
      given Decoder[State] = Decoder.decodeString.map(State.valueOf)

    enum State:
      case failed, running, finished, canceled, preparing

  case class Execution(id: String, state: Execution.State, duration: Long) derives derivation.ConfiguredCodec

  implicit class ToJsonDecorator[T: _root_.io.circe.Encoder](x: T):
    def toJson =
      import _root_.io.circe.*
      import _root_.io.circe.syntax.*
      x.asJson.deepDropNullValues.spaces2


  object MultiPath extends OptionalMultiQueryParamDecoderMatcher[String]("path")
  object PathParam extends OptionalQueryParamDecoderMatcher[String]("path")
  object ListParam extends FlagQueryParamMatcher("list")

  object IdParam extends QueryParamDecoderMatcher[String]("id")
  object RunParam extends FlagQueryParamMatcher("run")

class RESTAPIv1Server(impl: ApiImpl):
  import impl.services.*
  import RESTAPIv1Server.*
  import org.openmole.gui.shared.data.ServerFileSystemContext.Project
  given IORuntime = impl.services.threadProvider.ioRuntime

  val routes: HttpRoutes[IO] =
    HttpRoutes.of:
      case req @ GET -> root / "files" :? PathParam(path) +& ListParam(list) if list =>
        def listing = impl.listFiles(fileToSafePath(path.getOrElse(""))).data.map(FileEntry.fromTreeNodeData)
        Ok(listing.toJson)

      case req @ GET -> root / "files" :? PathParam(path) =>
        // wget --content-disposition  localhost:46857/rest/v1/files@path=/trempoline
        val sp = fileToSafePath(path.getOrElse(""))
        CoreAPIServer.download(req, Seq(sp))

      case req @ POST -> root / "files" :? PathParam(path) =>
        // curl  -F "test.txt=@/tmp/test.txt" localhost:46857/rest/v1/files?path=/trempoline/
        req.decode[Multipart[IO]] { parts =>
          def fileParts = parts.parts.filter(_.filename.isDefined)
          val destDirectory = org.openmole.gui.server.ext.utils.projectsDirectory / path.getOrElse("")

          for file <- fileParts
          do HTTP.recieveFile(file, destDirectory)
          Ok()
        }

      case req @ DELETE -> root / "files" :? MultiPath(paths) =>
        // curl -X DELETE "localhost:46857/rest/v1/files?path=/trempoline/test.txt"
        paths match
          case cats.data.Validated.Invalid(e) =>
            import _root_.io.circe.generic.auto.*
            ExpectationFailed(e.toJson)
          case cats.data.Validated.Valid(ps) =>
            val safePaths = ps.map((p: String) => data.SafePath(p.split("/"), data.ServerFileSystemContext.Project))
            impl.deleteFiles(safePaths)
            Ok()

      case req @ GET -> root / "executions" :? PathParam(path) +& RunParam(run) if run =>
        // curl "localhost:46857/rest/v1/executions?path=/test/Pi%20Computation/Pi.oms&run"
        import _root_.io.circe.generic.auto.*
        val sp = fileToSafePath(path.getOrElse(""))
        Ok(impl.launchScript(sp, validateScript = true).toJson)

      case req@GET -> root / "executions" =>
        // curl "localhost:46857/rest/v1/executions"
        import _root_.io.circe.generic.auto.*

        def toExecution(d: data.ExecutionData) =
          def state =
            d.state match
              case _: data.ExecutionState.Failed => Execution.State.failed
              case _: data.ExecutionState.Running => Execution.State.running
              case _: data.ExecutionState.Finished => Execution.State.finished
              case _: data.ExecutionState.Canceled => Execution.State.canceled
              case _: data.ExecutionState.Preparing => Execution.State.preparing
          Execution(d.id.id, state, d.duration)

        Ok(impl.executionData(Seq()).map(toExecution).toJson)

      case req @ DELETE -> root / "executions" :? IdParam(id) =>
        // curl -X DELETE "localhost:46857/rest/v1/executions?id=ghvWZXZmKv"
        impl.removeExecution(data.ExecutionId(id))
        Ok()