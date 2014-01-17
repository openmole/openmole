/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 * Copyright (C) 2014 Jonathan Passerat-Palmbach
 */

package org.openmole.ide.plugin.environment.slurm

import org.openmole.plugin.environment.slurm.SLURMEnvironment
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.ide.core.implementation.data.EnvironmentDataUI
import org.openmole.misc.workspace.Workspace

class SLURMEnvironmentDataUI(val name: String = "",
                           val login: String = "",
                           val host: String = "",
                           val port: Int = 22,
                           val queue: Option[String] = None,
                           val openMOLEMemory: Option[Int] = Some(BatchEnvironment.defaultRuntimeMemory),
                           val wallTime: Option[String] = None,
                           val memory: Option[Int] = None,
                           val path: Option[String] = None,
			   val gres: List[Gres] = List(),
			   val constraints: List[String] = List()
			   // not supported yet in GridScale
//                           val threads: Option[Int] = None,
//                           val nodes: Option[Int] = None,
//                           val coreByNode: Option[Int] = None)
    extends EnvironmentDataUI {
  ui ⇒

  def coreObject = util.Try {
    SLURMEnvironment(login,
      host,
      port,
      queue,
      openMOLEMemory,
      wallTime,
      memory,
      path,
      gres,
      constraints)(Workspace.authenticationProvider)
  }

  def coreClass = classOf[SLURMEnvironment]

  override def imagePath = "img/slurm.png"

  def fatImagePath = "img/slurm_fat.png"

  def buildPanelUI = new SLURMEnvironmentPanelUI(this)
}
