/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.control

import org.openmole.ide.core.workflow.implementation.CapsuleUI
import org.openmole.ide.core.workflow.model.IMoleScene

object TabsManager {

  def getCurrentObject: Object= MoleScenesManager.getCurrentObject.getOrElse(TaskSettingsManager.getCurrentObject)

  def getCurrentScene: IMoleScene= {
    val o= getCurrentObject
    o match{
      case x: CapsuleUI=> x.scene
      case _=> o.asInstanceOf[IMoleScene]
    }
  }
  
  def removeCurrentSceneAndChild= MoleScenesManager.removeCurrentSceneAndChilds(getCurrentScene)
}