/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.environment.oar

import org.openmole.plugin.environment.oar.OAREnvironment
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.ide.core.implementation.data.EnvironmentDataUI
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.service._

class OAREnvironmentDataUI(val name: String = "",
                           val user: String = "",
                           val host: String = "",
                           val port: Int = 22,
                           val queue: Option[String] = None,
                           val core: Option[Int] = None,
                           val cpu: Option[Int] = None,
                           val wallTime: Option[String] = None,
                           val openMOLEMemory: Option[Int] = Some(BatchEnvironment.defaultRuntimeMemory),
                           val workDirectory: Option[String] = None,
                           val threads: Option[Int] = None)
    extends EnvironmentDataUI {
  ui ⇒

  def coreObject = util.Try {
    OAREnvironment(user,
      host,
      port,
      queue,
      core,
      cpu,
      wallTime.map(_.toDuration),
      openMOLEMemory,
      workDirectory,
      threads)(Workspace.authenticationProvider)
  }

  def coreClass = classOf[OAREnvironment]

  override def imagePath = "img/oar.png"

  def fatImagePath = "img/oar_fat.png"

  def buildPanelUI = new OAREnvironmentPanelUI(this)
}
