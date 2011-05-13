/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.properties

trait IFactoryUI {

//  def buildEntity(name: String, panel: PanelUI): IEntityUI
//  
//  def buildEntity(panel: PanelUI): IEntityUI
  def panelUIData: PanelUIData
  
  def coreClass: Class[_]
  
  def imagePath: String 
  
  def coreObject: Object
  
  def buildPanelUI: IPanelUI
}