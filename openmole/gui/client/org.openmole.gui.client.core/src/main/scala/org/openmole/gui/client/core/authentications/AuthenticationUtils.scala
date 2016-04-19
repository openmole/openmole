package org.openmole.gui.client.core.authentications

import org.scalajs.dom.html.{ Input, Label }

import scalatags.JsDom.all._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import scalatags.JsDom.{ TypedTag, tags ⇒ tags }
import sheet._

/*
 * Copyright (C) 15/03/16 // mathieu.leclaire@openmole.org
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

object AuthenticationUtils {

  case class LabeledInput(label: Label, input: Input)

  def defaultLabeledInput(default: String, pHolder: String, w: Int) = LabeledInput(
    label(pHolder)(`for` := pHolder, sheet.marginLeft(5)).render,
    bs.input(default)(
    id := pHolder,
    formControl,
    sheet.paddingTop(5) +++ sheet.marginLeft(5),
    placeholder := pHolder,
    width := w
  ).render
  )

  def loginInput(default: String) = defaultLabeledInput(default, "Login", 130)

  def targetInput(default: String) = defaultLabeledInput(default, "Host", 130)

  def portInput(default: String) = defaultLabeledInput(default, "Port", 60)

  def passwordInput(default: String) = {
    val ID = "Password"
    LabeledInput(
      label(ID)(`for` := ID, sheet.marginLeft(5)).render,
      bs.input(default)(
      id := ID,
      formControl,
      sheet.paddingTop(5) +++ sheet.marginLeft(5),
      placeholder := ID,
      `type` := "password",
      width := "130px"
    ).render
    )
  }

}
