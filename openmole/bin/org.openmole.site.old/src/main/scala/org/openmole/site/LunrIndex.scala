package org.openmole.site

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
