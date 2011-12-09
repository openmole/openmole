/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.environment.local

import org.openmole.core.implementation.execution.local.LocalExecutionEnvironment
import org.openmole.ide.core.model.data.IEnvironmentDataUI

class LocalEnvironmentDataUI(val name: String="",val nbThread: Int= 1) extends IEnvironmentDataUI {

  override def coreObject = new LocalExecutionEnvironment(nbThread)

  override def coreClass = classOf[LocalExecutionEnvironment] 
  
  override def imagePath = "img/local.png" 
  
  override def buildPanelUI = new LocalEnvironmentPanelUI(this)
}
