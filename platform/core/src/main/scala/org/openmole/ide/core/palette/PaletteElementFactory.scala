/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.palette

import org.openmole.ide.core.properties.IFactoryUI

class PaletteElementFactory(val displayName: String,val factoryUI: IFactoryUI) {
  def buildEntity = factoryUI.buildEntity(displayName,factoryUI.panel)
  def buildNewEntity = factoryUI.buildEntity(factoryUI.panel)
}
