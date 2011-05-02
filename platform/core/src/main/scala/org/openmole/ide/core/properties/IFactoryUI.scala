/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.properties

import org.openmole.ide.core.workflow.model.IEntityUI

trait IFactoryUI {

  def buildEntity(name: String, panel: PanelUI): IEntityUI
  
  def buildEntity(panel: PanelUI): IEntityUI
  
  def coreClass: Class[_]
  
  def imagePath: String 
  
  def panel: PanelUI
  
  def coreObject(name: String): Object
}