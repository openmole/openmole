/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.model.data

import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.panel.IHookPanelUI

trait IHookDataUI extends IDataUI{
  override def entityType = HOOK
  
  def buildPanelUI: IHookPanelUI
}