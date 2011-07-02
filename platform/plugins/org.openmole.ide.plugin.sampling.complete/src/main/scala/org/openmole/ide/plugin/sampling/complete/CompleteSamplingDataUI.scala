/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.complete


import java.io.File
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.properties.ISamplingDataUI
import org.openmole.plugin.sampling.complete.CompleteSampling
import org.openmole.ide.core.properties.EmptyDataUIs._

class CompleteSamplingDataUI(val name: String) extends ISamplingDataUI {
  
  override def coreObject = new CompleteSampling

  override def coreClass = classOf[CompleteSampling] 
  
  override def imagePath = "img/thumb/completeSampling.png" 
  
  override def buildPanelUI = new CompleteSamplingPanelUI(this)
}
