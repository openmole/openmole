/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.dialog

import javax.swing.JOptionPane
import javax.swing.JOptionPane._
import org.openmole.ide.core.implementation.workflow.LabeledConnectionWidget

object TransitionDialog {
  def display(connectionWidget: LabeledConnectionWidget) {
    connectionWidget.transition.condition = None
    val cond = JOptionPane.showInputDialog(null, "Edit transition condition:", connectionWidget.transition.condition)
    if (cond == null) CLOSED_OPTION
    else connectionWidget.transition.condition = Some(cond)
    connectionWidget.setConditionLabel(connectionWidget.transition.condition)
    connectionWidget.scene.validate
  }
}