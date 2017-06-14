package org.openmole.site

/*
 * Copyright (C) 16/05/17 // mathieu.leclaire@openmole.org
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
import scala.xml._
import spray.json._

object LunrIndex {
  def Index(url: String, text: String): JsObject = {
    Index(url, XML.loadString(text))
  }
  def Index(url: String, xml: Elem): JsObject = {
    val title = (xml \\ "title").text
    val body = (xml \\ "body").text
    JsObject(
      "url" → JsString(url),
      "title" → JsString(title),
      "body" → JsString(body)
    )
  }
}