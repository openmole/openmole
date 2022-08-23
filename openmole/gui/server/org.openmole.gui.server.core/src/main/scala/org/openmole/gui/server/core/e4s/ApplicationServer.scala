package org.openmole.gui.server.core.e4s

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


import org.openmole.gui.server.core.{GUIServer, GUIServerServices, GUIServlet}
import org.openmole.gui.ext
import org.http4s.HttpRoutes
import scalatags.Text.all.*
import scalatags.Text.all as tags
import cats.effect.*
import org.http4s.*
import org.http4s.dsl.io.*
import org.http4s.headers.{`Content-Type`, Location}
import org.http4s.syntax.all.*
import org.openmole.core.preference.Preference
import org.openmole.tool.crypto.Cypher


import java.io.File
import java.util.concurrent.atomic.AtomicReference

object ApplicationServer {
  def redirect(s: String) = SeeOther.apply(Location.parse(s"/$s").right.get)
}

class ApplicationServer(webapp: File, extraHeader: String, password: Option[String], services: GUIServerServices) {
  
  val cypher = new AtomicReference[Cypher](Cypher(password))
  def passwordProvided = password.isDefined
  def passwordIsChosen = Preference.passwordChosen(services.preference)
  def passwordIsCorrect = Preference.passwordIsCorrect(cypher.get, services.preference)

  def connectionContent = {
    val ht = GUIServlet.html(s"${GUIServlet.webpackLibrary}.connection();", GUIServlet.cssFiles(webapp), extraHeader)
    Ok.apply(ht.render).map(_.withContentType(`Content-Type`(MediaType.text.html)))
  }

  val routes: HttpRoutes[IO] = HttpRoutes.of{
    case GET -> Root / ext.data.routes.appRoute =>
      def application = GUIServlet.html(s"${GUIServlet.webpackLibrary}.run();", GUIServlet.cssFiles(webapp), extraHeader)

      if(!passwordIsChosen && passwordProvided) Preference.setPasswordTest(services.preference, cypher.get)

      if (!passwordIsChosen && !passwordProvided) ApplicationServer.redirect(ext.data.routes.connectionRoute)
      else
        if (passwordIsCorrect) Ok.apply(application.render).map(_.withContentType(`Content-Type`(MediaType.text.html)))
        else ApplicationServer.redirect(ext.data.routes.connectionRoute)
    case GET -> Root / ext.data.routes.connectionRoute =>
      if (passwordIsChosen && passwordIsCorrect) ApplicationServer.redirect(ext.data.routes.appRoute)
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
      else ApplicationServer.redirect(ext.data.routes.resetPasswordRoute)
    case req @ POST -> Root / ext.data.routes.connectionRoute =>

      //      response.setHeader("Access-Control-Allow-Origin", "*")
//      response.setHeader("Access-Control-Allow-Methods", "POST, GET, PUT, UPDATE, OPTIONS")
//      response.setHeader("Access-Control-Allow-Headers", "Content-Type, Accept, X-Requested-With")

      req.decode[UrlForm] { m =>
        val password = m.getFirstOrElse("password", "")
        cypher.set(Cypher(password))
        passwordIsCorrect match {
          case true ⇒ ApplicationServer.redirect(ext.data.routes.appRoute)
          case _ ⇒ ApplicationServer.redirect(ext.data.routes.connectionRoute)
        }
      }
    case request@GET -> Root / "js" / "snippets" / path =>
      StaticFile.fromFile(new File(webapp, s"js/$path"), Some(request)).getOrElseF(NotFound())
    case request @ GET -> Root / "js" / path =>
      StaticFile.fromFile(new File(webapp, s"js/$path"), Some(request)).getOrElseF(NotFound())
    case request @ GET -> Root / "css" / path =>
      StaticFile.fromFile(new File(webapp, s"css/$path"), Some(request)).getOrElseF(NotFound())
    case request @ GET -> Root / "img" / path =>
      StaticFile.fromFile(new File(webapp, s"img/$path"), Some(request)).getOrElseF(NotFound())
    case request @ GET -> Root / "fonts" / path =>
      StaticFile.fromFile(new File(webapp, s"fonts/$path"), Some(request)).getOrElseF(NotFound())
    case GET -> Root / path => ApplicationServer.redirect(ext.data.routes.appRoute)
  }
}
