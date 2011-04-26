/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation

import org.openmole.ide.core.control.MoleScenesManager
import org.openmole.ide.core.properties.ISamplingFactoryUI
import org.openmole.ide.core.workflow.model.IEntityUI

class SamplingUI(var name: String,val factory: ISamplingFactoryUI) extends IEntityUI {
  def this(factory: ISamplingFactoryUI)= this(MoleScenesManager.incrementSamplingName,factory)
}
//class SamplingUI extends IEntityUI {
//  
//  var name = "SamplingUI"
//  
//  var entityType = classOf[Sampling[_]]
  