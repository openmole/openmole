/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.properties

import org.openmole.ide.core.workflow.implementation.SamplingUI

trait ISamplingFactoryUI extends IFactoryUI {
  
  override def entity(name: String, entityType: Class[_]) = new SamplingUI(name, entityType)
}
