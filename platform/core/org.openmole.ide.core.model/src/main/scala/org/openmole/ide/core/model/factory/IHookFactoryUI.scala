/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.model.factory

import java.io.PrintStream
import org.openmole.core.model.data.IPrototype
import org.openmole.core.model.mole.ICapsule
import org.openmole.core.model.mole.IMoleExecution
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.panel.IHookPanelUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import scala.collection.mutable.HashMap

trait IHookFactoryUI{
  def buildPanelUI(execution: IMoleExecution, 
                   prototypes: HashMap[IPrototypeDataProxyUI,IPrototype[_]], 
                   capsuleUI: ICapsuleUI, 
                   capsule: ICapsule,
                   printStream: PrintStream): IHookPanelUI
}
