/*
 * Copyright (C) 2012 mathieu
 * Copyright (C) 2014 Jonathan Passerat-Palmbach
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.environment.condor

import org.openmole.plugin.environment.condor.CondorEnvironment
import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.ide.core.implementation.data.EnvironmentDataUI
import org.openmole.misc.workspace.Workspace
import org.openmole.misc.tools.service._

class CondorEnvironmentDataUI(val name: String = "",
                              val login: String = "",
                              val host: String = "",
                              val port: Int = 22,
                              // TODO not available in GridScale plugin yet
                              //val queue: Option[String] = None,
                              val openMOLEMemory: Option[Int] = Some(BatchEnvironment.defaultRuntimeMemory),
                              val wallTime: Option[String] = None,
                              val memory: Option[Int] = None,
                              val path: Option[String] = None) extends EnvironmentDataUI { ui ⇒

  def coreObject = util.Try {
    CondorEnvironment(login,
      host,
      port,
      // TODO not available in GridScale plugin yet
      //queue,
      openMOLEMemory,
      wallTime.map(_.toDuration),
      memory,
      path)(Workspace.authenticationProvider)
  }

  def coreClass = classOf[CondorEnvironment]

  override def imagePath = "img/condor.png"

  def fatImagePath = "img/condor_fat.png"

  def buildPanelUI = new CondorEnvironmentPanelUI(this)
}
