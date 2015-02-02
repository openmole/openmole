package org.openmole.gui.client.core

import org.openmole.gui.client.service.ClientService
import org.openmole.gui.ext.dataui._

//import org.openmole.gui.misc.js.Forms

import org.openmole.gui.misc.js.Forms._

//import org.scalajs.dom
//import scalatags.JsDom._

import org.openmole.gui.misc.js.JsRxTags._

//import scalatags.JsDom.tags.{ div, ul, li, label, form, input, button }
//import scalatags.JsDom.attrs._

import scalatags.JsDom.all._

//import scalatags.JsDom.all._

import rx._

/*
 * Copyright (C) 11/12/14 // mathieu.leclaire@openmole.org
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

object IOPanel {
  def apply[T <: InputDataUI](dataUI: T) = new IOPanel(dataUI)
}

class IOPanel[T <: InputDataUI](dataUI: T) {

}

