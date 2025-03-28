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
  def Index(url: String, name: String, text: String): JsObject = {
    Index(url, name, XML.loadString(text))
  }

  def attributeValueEquals(value: String)(node: Node) =
    node.attributes.exists(_.value.text == value)

  def Index(url: String, name: String, xml: Elem): JsObject = {
    val body = (xml \\ "body").text
    val code = (xml \\ "pre").text
    val h2 = (xml \\ "h2").text
    val h3 = (xml \\ "h2").text
    JsObject(
      "url" → JsString(url),
      "title" → JsString(name),
      "h2" → JsString(h2),
      "h3" → JsString(h3),
      "body" → JsString(body),
      "pre" → JsString(code)
    )
  }
}