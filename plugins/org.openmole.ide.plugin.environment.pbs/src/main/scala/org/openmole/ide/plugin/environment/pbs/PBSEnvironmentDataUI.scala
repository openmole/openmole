/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.environment.pbs

import org.openmole.plugin.environment.pbs.PBSEnvironment
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.ide.core.model.data.IEnvironmentDataUI

class PBSEnvironmentDataUI(val name: String = "",
                           val login: String = "",
                           val host: String = "",
                           val dir: String = "/tmp/",
                           val queue: String = "",
                           val requirements: String = "",
                           val runtimeMemory: Int = BatchEnvironment.defaultRuntimeMemory) extends IEnvironmentDataUI {

  def coreObject = new PBSEnvironment(login, host, dir, if (queue.isEmpty) None else Some(queue), runtimeMemory)

  def coreClass = classOf[PBSEnvironment]

  def imagePath = "img/pbs.png"

  override def fatImagePath = "img/pbs_fat.png"

  def buildPanelUI = new PBSEnvironmentPanelUI(this)
}
