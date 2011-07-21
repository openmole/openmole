/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.action

import org.openmole.ide.core.implementation.dataproxy.EnvironmentDataProxyFactory
import org.openmole.ide.core.implementation.display.Displays
import org.openmole.ide.core.implementation.display.EnvironmentDisplay
import scala.swing.Action

class EnvironmentDisplayAction(dpf:EnvironmentDataProxyFactory,tytype : String) extends Action(dpf.factory.displayName){
  override def apply = {
    Displays.currentType = tytype
    EnvironmentDisplay.dataProxy = Some(dpf.buildDataProxyUI(Displays.name))
    Displays.propertyPanel.initNewEntity
  }
}
