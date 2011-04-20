/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.properties

import org.openmole.ide.core.workflow.implementation.TaskUI

trait ITaskFactoryUI extends IFactoryUI {

  override def entity(name: String, entityType: Class[_]) = new TaskUI(name, entityType)
}
