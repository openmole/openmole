/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.environment.sge

import org.openmole.plugin.environment.sge.SGEEnvironment
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.ide.core.implementation.data.EnvironmentDataUI
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.service._

class SGEEnvironmentDataUI(val name: String = "",
                           val login: String = "",
                           val host: String = "",
                           val port: Int = 22,
                           val queue: Option[String] = None,
                           val openMOLEMemory: Option[Int] = Some(BatchEnvironment.defaultRuntimeMemory),
                           val wallTime: Option[String] = None,
                           val memory: Option[Int] = None,
                           val path: Option[String] = None) extends EnvironmentDataUI { ui ⇒

  def coreObject = util.Try {
    SGEEnvironment(login,
      host,
      port,
      queue,
      openMOLEMemory,
      wallTime.map(_.toDuration),
      memory,
      path)(Workspace.authenticationProvider)
  }

  def coreClass = classOf[SGEEnvironment]

  override def imagePath = "img/sge.png"

  def fatImagePath = "img/sge_fat.png"

  def buildPanelUI = new SGEEnvironmentPanelUI(this)
}
