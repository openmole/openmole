/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.control

import org.openmole.ide.core.model.workflow.IMoleSceneManager
import scala.swing.TabbedPane
import scala.collection.JavaConversions._

class ExecutionTabbedPane(manager: IMoleSceneManager) extends TabbedPane {
  manager.capsules.values.foreach{c=>pages+= new TabbedPane.Page(c.dataProxy.get.dataUI.name,new ExecutionPanel(c))}
}
