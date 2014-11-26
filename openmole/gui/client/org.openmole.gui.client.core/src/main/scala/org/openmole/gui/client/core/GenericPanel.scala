package org.openmole.gui.client.core

import org.openmole.gui.ext.factoryui.FactoryUI
import org.openmole.gui.misc.js.Forms._
import rx._
/*
 * Copyright (C) 12/11/14 // mathieu.leclaire@openmole.org
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

object GenericPanel {

  def apply(id: String, title: String, factories: Seq[FactoryUI]) = {
    factories.foreach { f ⇒ println("dis " + f.dataUI.name()) }
    modalDialog(id,
      modalHeader(title),
      modalBody("This my body, eat it ! This is my blood, drink it !"),
      modalFooter(button("Yo"))
    )
  }
}
