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
                           val port: Int = 22,
                           val queue: Option[String] = None,
                           val openMOLEMemory: Option[Int] = Some(BatchEnvironment.defaultRuntimeMemory),
                           val wallTime: Option[String] = None,
                           val memory: Option[Int] = None,
                           val path: Option[String] = None,
                           val threads: Option[Int] = None,
                           val nodes: Option[Int] = None,
                           val coreByNode: Option[Int] = None)
    extends IEnvironmentDataUI {
  ui ⇒

  def coreObject = PBSEnvironment(login,
    host,
    port,
    queue,
    openMOLEMemory,
    wallTime,
    memory,
    path,
    threads,
    nodes,
    coreByNode)

  def coreClass = classOf[PBSEnvironment]

  def imagePath = "img/pbs.png"

  override def fatImagePath = "img/pbs_fat.png"

  def buildPanelUI = new PBSEnvironmentPanelUI(this)
}
