/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.model.factory

import org.openmole.ide.core.model.control.IExecutionManager
import org.openmole.ide.core.model.panel.IHookPanelUI

trait IHookFactoryUI extends IFactoryUI{
  override def displayName: String = ""
  
  def coreClass : Class[_]
  
  def buildPanelUI(executionManager: IExecutionManager): IHookPanelUI
}