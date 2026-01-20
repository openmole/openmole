package org.openmole.gui.client.tool.bootstrapnative

/*
 * Copyright (C) 02/05/16 // mathieu.leclaire@openmole.org
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

object Popup {

  sealed trait PopupPosition {
    def value: String
  }

  object Left extends PopupPosition {
    def value = "left"
  }

  object Right extends PopupPosition {
    def value = "right"
  }

  object Top extends PopupPosition {
    def value = "top"
  }

  object Bottom extends PopupPosition {
    def value = "bottom"
  }

  sealed trait PopupType

  object HoverPopup extends PopupType

  object ClickPopup extends PopupType

  object Manual extends PopupType

  object DialogPopup extends PopupType

}
