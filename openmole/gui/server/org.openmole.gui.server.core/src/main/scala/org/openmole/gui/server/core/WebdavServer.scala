package org.openmole.gui.server.core

/*
 * Copyright (C) 2024 Romain Reuillon
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


import org.http4s
import org.http4s.*
import org.http4s.dsl.*
import org.http4s.dsl.io.*
import org.typelevel.ci.*
import cats.effect.IO

import org.openmole.tool.file.*

import java.io.OutputStreamWriter

object WebdavServer:
  import scala.xml.*

  object FileResource:
    def httpDate(time: Long): String =
      val formatter =
        val df = new java.text.SimpleDateFormat("EEE, dd MMM yyyy hh:mm:ss z")
        df.setTimeZone(java.util.TimeZone.getTimeZone("GMT"))
        df
      formatter.format(new java.util.Date(time))

    def property(file: java.io.File, prop: Node): Option[Node] =
      def node(value: Node): Option[Node] =
        prop match
          case Elem(p, l, at, sc) => Some(Elem(p, l, at, sc, true, value))

      def nodeFromString(value: String): Option[Node] = node(scala.xml.Text(value))

      prop match
        case <getlastmodified/> => nodeFromString(httpDate(file.lastModified))
        case <getcontentlength/> => nodeFromString(file.length.toString)
        case <resourcetype/> => if file.isDirectory then node(<D:collection/>) else Some(prop)
        case _ => None

    def url(resource: FileResource) = s"/${resource.path.mkString("/")}" + (if resource.file.isDirectory then "/" else "")
    def children(resource: FileResource): Seq[FileResource] =
      if resource.file.exists() && resource.file.isDirectory
      then
        resource.file.listFiles.map: f =>
          FileResource(f, resource.path ++ Seq(f.getName))
      else Seq()


  case class FileResource(file: java.io.File, path: Seq[String])
  def propfind(props: NodeSeq, file: java.io.File, depth: String, basePath: String) =
    val res = FileResource(file, Seq(basePath))
    val resources = depth match
      case "0" => Seq(res)
      case "1" => FileResource.children(res) ++ Seq(res)

    <D:multistatus xmlns:D="DAV:"> {
      resources.map: res =>
        val mapped = props.map(p => (p, FileResource.property(res.file, p)))
        <D:response>
          <D:href>{FileResource.url(res)}</D:href>
          <D:propstat xmlns:D="DAV:">
            <D:prop>
              {
                mapped.flatMap:
                  case (_, Some(p)) => p :: Nil
                  case (_, None) => Nil
              }
            </D:prop>
            <D:status>HTTP/1.1 200 OK</D:status>
          </D:propstat>
          <D:propstat xmlns:D="DAV:">
            <D:prop>
              {
                mapped.flatMap:
                  case (_, Some(p)) => Nil
                  case (p, None) => p
              }
            </D:prop>
            <D:status>HTTP/1.1 404 Not Found</D:status>
          </D:propstat>
        </D:response>
      }
    </D:multistatus>

  def handle(req: Request[IO], directory: java.io.File) =
    val file =
      val path = req.uri.path.segments.drop(1).mkString("/") //req.pathInfo.segments.map(_.decoded()).mkString("/")
      new java.io.File(directory, path)

    val requestPath = req.uri.path.segments.mkString("/")

    req.method match
      case Method.OPTIONS =>
        Ok().map:
          _.withHeaders(
            Header.Raw(ci"DAV", "1"),
            Header.Raw(ci"Allow","GET,OPTIONS,PROPFIND,DELETE,PUT,MOVE,MKCOL")
          )

      case Method.PUT =>
          file.getParentFile.mkdirs()
          file.setWritable(true)
          val stream = fs2.io.toInputStreamResource(req.body)
          val r =
            stream.use: st =>
              import org.openmole.tool.stream.*
              IO.blocking:
                st.copy(file)
                file.setExecutable(true)
                ""
          Ok(r)

      case Method.MOVE =>
        val destination = java.net.URI.create(req.headers.get(ci"Destination").map(_.head.value).get)
        val destinationFile = directory / destination.getPath.split("/").drop(2).mkString("/")
        file move destinationFile
        Ok()

      case Method.MKCOL =>
        file.mkdirs()
        Ok()

      case _ if !file.exists() => NotFound()

      case Method.PROPFIND =>
        val depth = req.headers.get(ci"Depth").map(_.head.value).getOrElse("0")

        import scala.xml.XML

        val input =
          val stream = fs2.io.toInputStreamResource(req.body)
          stream.use: is =>
            IO(XML.load(is))

        val st =
          fs2.io.readOutputStream[IO](64 * 1024): out =>
            input.map: input =>
              val response = propfind(input \ "prop" \ "_", file, depth, requestPath)
              val w = new OutputStreamWriter(out)
              try XML.write(w, response, "utf-8", true, null)
              finally w.flush()

        MultiStatus(st).map: res =>
          res.withContentType(org.http4s.headers.`Content-Type`.parse("application/xml").right.get)

      case Method.HEAD =>
        Ok().map:
          _.withHeaders(org.http4s.headers.`Content-Length`.fromLong(file.length))

      case Method.GET =>
        StaticFile.fromPath(fs2.io.file.Path.fromNioPath(file.toPath), Some(req)).getOrElseF(Status.NotFound.apply(s"${file.getName} not found"))

      case Method.DELETE =>
        file.recursiveDelete
        Ok()

      case m => NotImplemented(s"$m not implement")


class WebdavServer(directory: java.io.File):
  def routes: HttpRoutes[IO] =
    HttpRoutes.of:
      case req => WebdavServer.handle(req, directory)
