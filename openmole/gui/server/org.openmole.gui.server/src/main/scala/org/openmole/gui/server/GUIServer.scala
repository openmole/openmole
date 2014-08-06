/*
 * Copyright (C) 21/07/14 // mathieu.leclaire@openmole.org
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.gui.server

import java.util.UUID

import org.scalatra._
import scala.concurrent.ExecutionContext.Implicits.global
import autowire._
import org.openmole.gui.shared._
import scala.concurrent.duration._
import scala.concurrent.Await

object Server extends Api {
  def hello(a: Int) = a * 3
}

class GUIServer extends ServertestStack {

  val basePath = "shared"

  get("/") {
    contentType = "text/html"
    jade("/default.jade")
  }

  post(s"/$basePath/*") {
    Await.result(autowire.Macros.route[Web](Server)(
      autowire.Request(Seq(basePath) ++ multiParams("splat").head.split("/"),
        upickle.read[Map[String, String]](request.body))
    ), Duration.Inf)
  }

}
