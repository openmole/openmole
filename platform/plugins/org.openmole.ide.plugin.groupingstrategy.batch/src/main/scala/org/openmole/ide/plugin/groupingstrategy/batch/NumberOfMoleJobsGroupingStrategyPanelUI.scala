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

import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.ide.core.model.control.IExecutionManager
import org.openmole.ide.misc.widget.multirow.RowWidget._
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.MultiComboTextField
import org.openmole.ide.misc.widget.multirow.MultiComboTextField._
import java.awt.Font
import java.awt.Font._
import org.openmole.ide.core.model.panel.IGroupingStrategyPanelUI
import org.openmole.ide.misc.exception.GUIUserBadDataError
import org.openmole.ide.misc.widget.MigPanel
import scala.swing.ComboBox
import scala.swing.Panel
import scala.swing.event.SelectionChanged

object NumberOfMoleJobsGroupingStrategyPanelUI {
  def rowFactory(strategypanel: NumberOfMoleJobsGroupingStrategyPanelUI) = new Factory[ICapsule] {
    override def apply(row: ComboTextFieldRowWidget[ICapsule], p: Panel) = {
      import row._
     
      val combotfrow: ComboTextFieldRowWidget[ICapsule] = 
        new ComboTextFieldRowWidget(name,comboContentA,selectedA,"",plus) {
          override def doOnClose = strategypanel.executionManager.commitHook("org.openmole.plugin.groupingstrategy.batch.NumberOfMoleJobsGroupingStrategy")
        }
      combotfrow
    }
  }
}
//object ToStringHookPanelUI{
//  def rowFactory(hookpanel: ToStringHookPanelUI) = new Factory[IPrototype[_],ICapsule] {
//    override def apply(row: TwoCombosRowWidget[IPrototype[_],ICapsule], p: Panel) = {
//      import row._
//      val twocombrow: TwoCombosRowWidget[IPrototype[_],ICapsule] = 
//        new TwoCombosRowWidget(name,comboContentA,selectedA,comboContentB,selectedB,inBetweenString,plus) {
//          override def doOnClose = hookpanel.executionManager.commitHook("org.openmole.plugin.hook.display.ToStringHook")
//        }
//      
//      twocombrow.combo1.selection.reactions += {case SelectionChanged(twocombrow.`combo1`)=>commit}
//      twocombrow.combo2.selection.reactions += {case SelectionChanged(twocombrow.`combo2`)=>
//          val items = hookpanel.protosFromTask(twocombrow.`combo2`.selection.item)
//          twocombrow.changeCombo1Items(items)
//          commit}
//      
//      def commit = 
//        hookpanel.executionManager.commitHook("org.openmole.plugin.hook.display.ToStringHook")
//      
//      twocombrow
//    }
//  }
//}
import NumberOfMoleJobsGroupingStrategyPanelUI._
//
class NumberOfMoleJobsGroupingStrategyPanelUI(val executionManager: IExecutionManager) extends MigPanel("") with IGroupingStrategyPanelUI{
  var multiRow : Option[MultiComboTextField[ICapsule]] = None
  val capsules : List[ICapsule]= executionManager.capsuleMapping.values.filter(_.outputs.size > 0).toList
  
  if (capsules.size>0){
    val r =  new ComboTextFieldRowWidget("Display",
                                         capsules,
                                         capsules(0),
                                         "",
                                         NO_ADD)
    
    multiRow =  Some(new MultiComboTextField(List(r),
                                             rowFactory(this),
                                             CLOSE_IF_EMPTY,
                                             NO_ADD))
  }
  
  if (multiRow.isDefined) contents+= multiRow.get.panel
  
  def saveContent = {
    if (multiRow.isDefined) multiRow.get.content.map{c=> try{
        new NumberOfMoleJobsGroupingStrategyDataUI(executionManager,(c._1,c._2.toInt))
      } catch {
        case e:NumberFormatException=> throw new GUIUserBadDataError(c._2 + " is not an integer")
      }
    }
    else List()
  }
  
  def addStrategy = if (multiRow.isDefined) multiRow.get.showComponent
}