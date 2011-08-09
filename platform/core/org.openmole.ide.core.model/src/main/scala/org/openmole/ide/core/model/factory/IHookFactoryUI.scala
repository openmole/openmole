/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.model.factory


import org.openmole.ide.core.model.data.IHookDataUI

trait IHookFactoryUI  extends IFactoryUI{
  override def displayName: String = buildDataUI("").coreClass.getSimpleName
  
  def buildDataUI(name: String): IHookDataUI
}
