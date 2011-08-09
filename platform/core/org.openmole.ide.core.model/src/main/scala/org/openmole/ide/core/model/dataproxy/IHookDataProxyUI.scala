/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.model.dataproxy

import org.openmole.ide.core.model.data.IHookDataUI

trait IHookDataProxyUI extends IDataProxyUI{
  def dataUI_=(d: IHookDataUI)
  
  override def dataUI: IHookDataUI 
}