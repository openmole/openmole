package org.openmole.gui.client.core

import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import fr.iscpif.scaladget.mapping.bootstrap
import fr.iscpif.scaladget.mapping.bootstrap._

//import org.querki.jquery._
import scalatags.JsDom.all._
import bs._

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

  def triggerOpen: Unit = {
    modalPanel.onOpen()
  }

  def open: Unit = {
    bs.showModal(modalPanel.modalID)
    triggerOpen
  }
}

trait ModalPanel {
  def modalID: ModalID

  def dialog: Dialog

  def onOpen(): Unit

  def onClose(): Unit

  val closeButton = bs.button("Close", btn_default)(data("dismiss") := "modal", onclick := { () ⇒ close })

  def close: Unit = bs.hideModal(modalID)

  def isVisible: Boolean = bs.isModalVisible(modalID)
}
