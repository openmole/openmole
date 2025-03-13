package org.openmole.gui.server.core

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


import cats.effect.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.{Location, `Content-Type`}
import org.http4s.syntax.all.*
import org.openmole.core.preference.Preference
import org.openmole.gui.shared
import org.openmole.gui.server.core
import org.openmole.gui.server.core.{GUIServer, GUIServlet}
import org.openmole.tool.crypto.Cypher

import java.io.File
import java.util.concurrent.atomic.AtomicReference

object ApplicationServer:
  def redirect(s: String) =
    SeeOther.apply(Location(Uri.unsafeFromString(s)))

class ApplicationServer(webapp: File, extraHeader: String, password: Option[String], services: GUIServerServices.ServicesProvider):
  
//  val cypher = new AtomicReference[Cypher](Cypher(password))

  def passwordProvided = password.isDefined
  def passwordIsChosen = Preference.passwordChosen(services.preference)
  def passwordIsCorrect = Preference.passwordIsCorrect(services.cypher, services.preference)

  def connectionContent =
    val ht = GUIServlet.html(s"${GUIServlet.webpackLibrary}.connection();", GUIServlet.cssFiles(webapp), extraHeader)
    Ok.apply(ht.render).map(_.withContentType(`Content-Type`(MediaType.text.html)))

  val routes: HttpRoutes[IO] = HttpRoutes.of:
    case GET -> Root / shared.api.appRoute =>
      def application = GUIServlet.html(s"${GUIServlet.webpackLibrary}.run();", GUIServlet.cssFiles(webapp), extraHeader)
       Ok.apply(application.render).map(_.withContentType(`Content-Type`(MediaType.text.html)))
    case GET -> Root / shared.api.connectionRoute =>
      if (passwordIsChosen && passwordIsCorrect) ApplicationServer.redirect(shared.api.appRoute)
      else if (passwordIsChosen) {
//        response.setHeader("Access-Control-Allow-Origin", "*")
//        response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, UPDATE, OPTIONS")
//        response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With")
//        contentType = "text/html"
//        connection
        connectionContent
//        val ht = GUIServlet.html(s"${GUIServlet.webpackLibrary}.connection();", GUIServlet.cssFiles(webapp), extraHeader)
//        Ok.apply(ht.render).map(_.withContentType(`Content-Type`(MediaType.text.html)))
      }
      else ApplicationServer.redirect(shared.api.resetPasswordRoute)
    case GET -> Root / shared.api.resetPasswordRoute =>
      import services._
      org.openmole.core.services.Services.resetPassword
      val ht = GUIServlet.html(s"${GUIServlet.webpackLibrary}.resetPassword();", GUIServlet.cssFiles(webapp), extraHeader)
      Ok.apply(ht.render).map(_.withContentType(`Content-Type`(MediaType.text.html)))
    case GET -> Root / shared.api.restartRoute =>
      val ht = GUIServlet.html(s"${GUIServlet.webpackLibrary}.restarted();", GUIServlet.cssFiles(webapp), extraHeader)
      Ok.apply(ht.render).map(_.withContentType(`Content-Type`(MediaType.text.html)))
    case GET -> Root / shared.api.shutdownRoute =>
      val ht = GUIServlet.html(s"${GUIServlet.webpackLibrary}.stopped();", GUIServlet.cssFiles(webapp), extraHeader)
      Ok.apply(ht.render).map(_.withContentType(`Content-Type`(MediaType.text.html)))
    case req @ POST -> Root / shared.api.connectionRoute => ApplicationServer.redirect(shared.api.appRoute)
    case request@GET -> Root / "js" / "snippets" / path =>
      StaticFile.fromFile(new File(webapp, s"js/$path"), Some(request)).getOrElseF(NotFound())
    case request @ GET -> "js" /: path =>
      StaticFile.fromFile(new File(webapp, s"js/${path.segments.mkString("/")}"), Some(request)).getOrElseF(NotFound())
    case request @ GET -> "css" /: path =>
      StaticFile.fromFile(new File(webapp, s"css/${path.segments.mkString("/")}"), Some(request)).getOrElseF(NotFound())
    case request @ GET -> "img" /: path =>
      StaticFile.fromFile(new File(webapp, s"img/${path.segments.mkString("/")}"), Some(request)).getOrElseF(NotFound())
    case request @ GET -> "fonts" /: path =>
      StaticFile.fromFile(new File(webapp, s"fonts/${path.segments.mkString("/")}"), Some(request)).getOrElseF(NotFound())
    case GET -> Root => ApplicationServer.redirect(shared.api.appRoute)

