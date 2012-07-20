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

package org.openmole.ide.plugin.groupingstrategy.batch

import org.openmole.ide.core.model.control.IExecutionManager
import org.openmole.ide.core.model.factory.IGroupingFactoryUI
import org.openmole.misc.exception.UserBadDataError
import org.openmole.plugin.grouping.batch.NumberOfMoleJobsGrouping

class NumberOfMoleJobsGroupingFactoryUI extends IGroupingFactoryUI {

  var panelUI: Option[NumberOfMoleJobsGroupingPanelUI] = None

  def buildPanelUI(executionManager: IExecutionManager) = {
    panelUI = Some(new NumberOfMoleJobsGroupingPanelUI(executionManager))
    panelUI.get
  }

  def coreClass = classOf[NumberOfMoleJobsGrouping]

  def coreObject = panelUI match {
    case p: NumberOfMoleJobsGroupingPanelUI ⇒
      p.multiComboTextField.content.map { c ⇒
        (new NumberOfMoleJobsGrouping(c.textFieldValue.toInt),
          c.comboValue.get)
      }
    case _ ⇒ throw new UserBadDataError("An error occured when settings the grouping strategies (by number of jobs)")
  }

  override def toString = "Group by number of jobs"
}
