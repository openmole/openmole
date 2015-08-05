package org.openmole.gui.client.core

import org.openmole.gui.misc.js.BootstrapTags._
import org.scalajs.jquery

/*
 * Copyright (C) 28/05/15 // mathieu.leclaire@openmole.org
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

trait PanelTriggerer {
  val modalPanel: ModalPanel

  def triggerOpen: Unit = modalPanel.onOpen()

  def open: Unit = {
    triggerOpen
    jquery.jQuery("#" + modalPanel.modalID).modal("show")
  }
}

trait ModalPanel {
  def modalID: ModalID

  def dialog: Dialog

  def onOpen: () ⇒ Unit

  def onClose: () ⇒ Unit

  def close: Unit = {
    jquery.jQuery("#" + modalID).modal("hide")
  }

  jquery.jQuery(org.scalajs.dom.document).on("hide.bs.modal", "#" + modalID, () ⇒ {
    onClose()
  })
}