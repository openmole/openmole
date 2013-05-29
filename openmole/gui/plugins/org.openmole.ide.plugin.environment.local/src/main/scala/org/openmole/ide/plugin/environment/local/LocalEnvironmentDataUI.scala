/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.environment.local

import org.openmole.core.implementation.execution.local.LocalEnvironment
import org.openmole.ide.core.model.data.IEnvironmentDataUI

class LocalEnvironmentDataUI(val name: String = "",
                             val nbThread: Int = 1) extends IEnvironmentDataUI {

  def coreObject = new LocalEnvironment(nbThread)

  def coreClass = classOf[LocalEnvironment]

  def imagePath = "img/local.png"

  override def fatImagePath = "img/local_fat.png"

  def buildPanelUI = new LocalEnvironmentPanelUI(this)
}
