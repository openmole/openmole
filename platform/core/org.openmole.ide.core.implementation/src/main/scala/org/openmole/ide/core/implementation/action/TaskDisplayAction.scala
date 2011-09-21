/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.action

import org.openmole.ide.core.implementation.dataproxy.TaskDataProxyFactory
import org.openmole.ide.core.implementation.display.Displays
import org.openmole.ide.core.implementation.display.TaskDisplay
import scala.swing.Action

class TaskDisplayAction (dpf:TaskDataProxyFactory,tytype : String) extends Action(dpf.factory.displayName){
  override def apply = {
    Displays.currentType = tytype
    TaskDisplay.currentDataProxy = Some(dpf.buildDataProxyUI(Displays.name))
    Displays.propertyPanel.initNewEntity
  }
}