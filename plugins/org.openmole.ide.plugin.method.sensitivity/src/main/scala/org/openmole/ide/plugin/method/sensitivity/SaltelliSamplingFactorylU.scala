/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.method.sensitivity

import org.openmole.ide.core.implementation.panel.ComponentCategories
import org.openmole.ide.core.model.factory.ISamplingFactoryUI

class SaltelliSamplingFactoryUI extends ISamplingFactoryUI {
  override def displayName = "Saltelli"

  def buildDataUI = new SaltelliSamplingDataUI

  def category = ComponentCategories.SAMPLING
}