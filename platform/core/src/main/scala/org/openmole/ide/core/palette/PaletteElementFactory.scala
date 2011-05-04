/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.palette

import javax.swing.text.JTextComponent
import org.openide.text.ActiveEditorDrop
import org.openmole.ide.core.properties.IFactoryUI

class PaletteElementFactory(val displayName: String,val factoryUI: IFactoryUI) extends ActiveEditorDrop{
  def buildEntity = factoryUI.buildEntity(displayName,factoryUI.panel)
  def buildNewEntity = factoryUI.buildEntity(factoryUI.panel)
  override def handleTransfer(targetComponent: JTextComponent) = {
    println("handletransfer")
    true
  }
}
