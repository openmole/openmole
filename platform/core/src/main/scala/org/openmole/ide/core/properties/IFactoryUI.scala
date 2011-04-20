/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.properties

import org.openmole.ide.core.workflow.model.IEntityUI

trait IFactoryUI {
  def panel: PanelUI
  
  def entity(name: String, entityType: Class[_]): IEntityUI
  
  def coreClass: Class[_]
  
  def coreObject(p: PanelUI): Object
}