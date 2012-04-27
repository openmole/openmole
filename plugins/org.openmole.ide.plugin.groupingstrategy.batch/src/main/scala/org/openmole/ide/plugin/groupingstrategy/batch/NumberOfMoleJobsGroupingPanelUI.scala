/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.groupingstrategy.batch

import org.openide.awt.StatusDisplayer
import org.openmole.core.model.mole.ICapsule
import org.openmole.ide.core.model.control.IExecutionManager
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.MultiComboTextField
import org.openmole.ide.misc.widget.multirow.MultiComboTextField._
import java.awt.Font
import java.awt.Font._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.model.panel.IGroupingPanelUI
import org.openmole.ide.misc.widget.MyPanel
import org.openmole.ide.misc.widget.PluginPanel

object NumberOfMoleJobsGroupingPanelUI {
  def rowFactory(strategypanel: NumberOfMoleJobsGroupingPanelUI) = new Factory[ICapsule] {
    override def apply(row: ComboTextFieldRowWidget[ICapsule], p: MyPanel) = {
      import row._
     
      val combotfrow: ComboTextFieldRowWidget[ICapsule] = 
        new ComboTextFieldRowWidget(comboContentA,selectedA,"",plus)
      combotfrow
    }
  }
}

import NumberOfMoleJobsGroupingPanelUI._
//
class NumberOfMoleJobsGroupingPanelUI(val executionManager: IExecutionManager) extends PluginPanel("") with IGroupingPanelUI{
  val capsules : List[ICapsule]= executionManager.capsuleMapping.values.toList
  
  val multiRow = 
    capsules.headOption match {
      case Some(capsule) =>
        val r =  new ComboTextFieldRowWidget(capsules, capsule, "by", NO_ADD)
    
        Some(new MultiComboTextField("Group", List(r), rowFactory(this), CLOSE_IF_EMPTY, NO_ADD))
      case None =>
        StatusDisplayer.getDefault().setStatusText("No capsules are defined")
        None
    }

  
  if (multiRow.isDefined) contents+= multiRow.get.panel
  
  def saveContent = 
    multiRow match {
     case Some(multiRow) =>
       multiRow.content.map {
        case(capsule, num)=>
          try new NumberOfMoleJobsGroupingDataUI(executionManager, (capsule, num.toInt))
          catch {
            case e:NumberFormatException=> throw new UserBadDataError(num + " is not an integer")
          }
      }
     case None => List()
    }
  
  def addStrategy = multiRow match {
    case Some(multiRow) => 
      multiRow.addRow
      multiRow.refresh
    case None => 
  }
}