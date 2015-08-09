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

import org.openmole.gui.client.core.AbsolutePositioning.{ Zone, FullPage, CenterTransform, Transform }
import org.openmole.gui.misc.js.{ BootstrapTags ⇒ bs, ClassKeyAggregator }
import org.scalajs.dom.html.Div
import scalatags.JsDom.all._
import scalatags.JsDom.{ TypedTag, tags }
import org.openmole.gui.misc.js.JsRxTags._
import bs._
import rx._

object AlertPanel {
  private val panel = new AlertPanel

  val div = panel.alertDiv

  def popup(message: String,
            okaction: () ⇒ Unit,
            cancelaction: () ⇒ Unit = () ⇒ {},
            transform: Transform = CenterTransform(),
            zone: Zone = FullPage(),
            alertType: ClassKeyAggregator = warning) = {
    panel.popup(message, okaction, cancelaction, transform, zone, alertType)
  }
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

  def popup(message: String,
            okaction: () ⇒ Unit,
            cancelaction: () ⇒ Unit,
            transform: Transform,
            zone: Zone = FullPage(),
            alertType: ClassKeyAggregator = warning) = {
    alertElement() = bs.alert(alertType, message, actionWrapper(okaction), actionWrapper(cancelaction))
    transform(elementDiv)
    overlayZone() = zone
    visible() = true
  }

  def actionWrapper(action: () ⇒ Unit): () ⇒ Unit = () ⇒ {
    action()
    visible() = false
  }

}
