package org.openmole.site

import org.scalajs.dom.raw.{ HTMLDivElement, Node }

import scaladget.api.{ BootstrapTags ⇒ bs }
import scaladget.stylesheet.{ all ⇒ sheet }
import bs._
import sheet._

/*
 * Copyright (C) 01/04/16 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY, without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package object sitesheet {

  import scalatags.JsDom.all._
  import scaladget.stylesheet.all._

  lazy val mainDiv: ModifierSeq = Seq(
    width := "50%",
    margin := "0 auto",
    scalatags.JsDom.all.paddingTop := 150
  )

  lazy val stepHeader: ModifierSeq = Seq(
    width := "22%",
    fontSize := "22px",
    fontWeight := "bold"
  )

  lazy val marginAuto: ModifierSeq = Seq(
    margin := "0 auto",
    width := "50%"
  )

  lazy val detailButtons: ModifierSeq = Seq(
    float := "left",
    fixedPosition,
    top := 500,
    scalatags.JsDom.all.marginLeft := -200
  )

  lazy val navigateDoc: ModifierSeq = Seq(
    float := "left",
    fixedPosition,
    top := 200,
    fontSize := "32px"
  )

  lazy val previousDoc: ModifierSeq = Seq(
    scalatags.JsDom.all.marginLeft := -200
  ) ++ navigateDoc

  lazy val nextDoc: ModifierSeq = Seq(
    scalatags.JsDom.all.paddingLeft := "calc(50% + 200px)"
  ) ++ navigateDoc

  lazy val searchResult: ModifierSeq = Seq(
    color := "black",
    left := -20,
    width := 200
  )
}

package object utils {

  import scalatags.JsDom.all.raw
  import scaladget.stylesheet.{ all ⇒ sheet }
  import scalatags.JsDom.tags
  import scalatags.JsDom.all._

  def to(page: JSPage) {
    org.scalajs.dom.window.location.href = page.file
  }

  implicit def texToDiv(textFrag: scalatags.Text.all.Frag): HTMLDivElement = {
    val aDiv = tags.div(sheet.paddingTop(60)).render
    raw(textFrag.render).applyTo(aDiv)
    aDiv
  }

  implicit def SomeTexToDiv(textFrag: Option[scalatags.Text.all.Frag]): HTMLDivElement = {
    textFrag.map(t ⇒ texToDiv(t)).getOrElse(div().render)
  }

  def replacer = new Replacer()

  case class Replacer() {
    private val parentID = uuID.short
    private val childID = uuID.short

    val tag =
      tags.div(id := parentID)(
        tags.div(id := childID)
      )

    def replaceWith(id: String) = {
      val newTag = org.scalajs.dom.window.document.getElementById(id)
      val childNode = org.scalajs.dom.window.document.getElementById(childID)
      val parentNode = org.scalajs.dom.window.document.getElementById(parentID)
      parentNode.replaceChild(newTag, childNode)
    }
  }

  def father(page: JSPage) =
    JSPages.all.collect { case doc: JSDocumentationPage ⇒ doc }.filter {
      _.children.contains(page)
    }.headOption

}
