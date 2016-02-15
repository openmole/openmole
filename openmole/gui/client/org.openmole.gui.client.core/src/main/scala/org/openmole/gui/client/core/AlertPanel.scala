package org.openmole.gui.client.core

/*
 * Copyright (C) 04/08/15 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.client.core.AbsolutePositioning._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs, ClassKeyAggregator }
import org.openmole.gui.client.core.files.{ TreeNodeComment, TreeNodeError }
import org.openmole.gui.misc.js.OMTags.AlertAction
import org.openmole.gui.misc.js.{ OptionsDiv, OMTags }
import org.scalajs.dom.html.Div
import org.scalajs.dom.raw.HTMLDivElement
import scalatags.JsDom.all._
import scalatags.JsDom.{ TypedTag, tags }
import org.openmole.gui.misc.js.JsRxTags._
import bs._
import rx._

object AlertPanel {
  private val panel = new AlertPanel

  val alertDiv = panel.alertDiv

  def div(messageDiv: TypedTag[HTMLDivElement],
          okaction: () ⇒ Unit,
          cancelaction: () ⇒ Unit = () ⇒ {},
          transform: Transform = CenterTransform(),
          zone: Zone = FullPage(),
          alertType: ClassKeyAggregator = warning,
          buttonGroupClass: ClassKeyAggregator = "right"): Unit = {
    panel.popup(messageDiv, Seq(AlertAction(okaction), AlertAction(cancelaction)), transform, zone, alertType, buttonGroupClass)
  }

  def treeNodeErrorDiv(error: TreeNodeError): Unit = div(
    tags.div(
      error.message,
      OptionsDiv(error.filesInError).div
    ), okaction = error.okaction, cancelaction = error.cancelaction, zone = FileZone())

  def treeNodeCommentDiv(error: TreeNodeComment): Unit = panel.popup(
    tags.div(
      error.message,
      OptionsDiv(error.filesInError).div
    ), Seq(AlertAction(error.okaction)), CenterTransform(), FileZone(), warning, "right")

  def string(message: String,
             okaction: () ⇒ Unit,
             cancelaction: () ⇒ Unit = () ⇒ {},
             transform: Transform = CenterTransform(),
             zone: Zone = FullPage(),
             alertType: ClassKeyAggregator = warning,
             buttonGroupClass: ClassKeyAggregator = "left"): Unit = div(tags.div(message), okaction, cancelaction, transform, zone, alertType, buttonGroupClass)
}

class AlertPanel {

  val visible: Var[Boolean] = Var(false)
  val overlayZone: Var[Zone] = Var(FullPage())
  val alertElement: Var[TypedTag[Div]] = Var(tags.div)

  val elementDiv = tags.div(
    Rx {
      alertElement()
    }
  ).render

  val alertDiv = tags.div(`class` := Rx {
    if (visible()) s"alertOverlay ${overlayZone().zoneClass}" else "displayNone"
  })(elementDiv)

  def popup(messageDiv: TypedTag[HTMLDivElement],
            actions: Seq[AlertAction],
            /* okaction: () ⇒ Unit,
            cancelaction: () ⇒ Unit,*/
            transform: Transform,
            zone: Zone = FullPage(),
            alertType: ClassKeyAggregator = warning,
            buttonGroupClass: ClassKeyAggregator = "left") = {
    alertElement() = OMTags.alert(alertType, messageDiv, actions.map { a ⇒ a.copy(action = actionWrapper(a.action)) }, buttonGroupClass)
    transform(elementDiv)
    overlayZone() = zone
    visible() = true
  }

  def actionWrapper(action: () ⇒ Unit): () ⇒ Unit = () ⇒ {
    action()
    visible() = false
  }

}
