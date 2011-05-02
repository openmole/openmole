/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.model

import org.openmole.ide.core.properties.PanelUI

trait IEntityUI {
  def name: String
  
  def coreObject = panel.coreObject(name)
  
  def coreClass: Class[_]
  
  def panel: PanelUI
}

//public interface IEntityUI {
//    String getName();
//    Class getType();
//}
