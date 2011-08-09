/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.action

import org.openmole.ide.core.implementation.dataproxy.HookDataProxyFactory
import org.openmole.ide.core.implementation.display.Displays
import org.openmole.ide.core.implementation.display.HookDisplay
import scala.swing.Action

class HookDisplayAction(dpf:HookDataProxyFactory,tytype : String) extends Action(dpf.factory.displayName){
  override def apply = {
    Displays.currentType = tytype
    HookDisplay.dataProxy = Some(dpf.buildDataProxyUI(Displays.name))
    Displays.propertyPanel.initNewEntity
  }
}
