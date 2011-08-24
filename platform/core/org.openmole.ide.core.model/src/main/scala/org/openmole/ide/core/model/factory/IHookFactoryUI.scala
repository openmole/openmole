/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.model.factory

import org.openmole.ide.core.model.control.IExecutionManager
import org.openmole.ide.core.model.data.IHookDataUI

trait IHookFactoryUI extends IFactoryUI{
  override def displayName: String = coreClass.getSimpleName
  
  def coreClass : Class[_]
  
  def buildDataUI(executionManager: IExecutionManager): IHookDataUI
}