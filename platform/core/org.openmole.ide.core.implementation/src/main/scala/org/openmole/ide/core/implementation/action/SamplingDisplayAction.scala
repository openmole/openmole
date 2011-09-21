/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.action

import scala.swing.Action
import org.openmole.ide.core.implementation.dataproxy.SamplingDataProxyFactory
import org.openmole.ide.core.implementation.display.Displays
import org.openmole.ide.core.implementation.display.SamplingDisplay

class SamplingDisplayAction(dpf:SamplingDataProxyFactory,tytype : String) extends Action(dpf.factory.displayName){
  override def apply = {
    Displays.currentType = tytype
    SamplingDisplay.currentDataProxy = Some(dpf.buildDataProxyUI(Displays.name))
    Displays.propertyPanel.initNewEntity
  }
}
