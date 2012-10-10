/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.plugin.environment.tools

import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.misc.workspace.Workspace
import scala.collection.mutable.ListBuffer
import org.openmole.plugin.environment.glite._

class RequirementDataUI(val architecture64: Boolean = false,
                        val workerNodeMemory: String = "",
                        val maxCPUTime: String = "",
                        val otherRequirements: String = "") {

  //  val toMap = {
  //    val requirements = new ListBuffer[Requirement]
  //    if (architecture64 == true) requirements += x86_64
  //    if (workerNodeMemory != "") requirements += MEMORY -> workerNodeMemory
  //    if (maxCPUTime != "") requirements += CPU_TIME -> maxCPUTime
  //    if (otherRequirements != "") requirements += GLITE_REQUIREMENTS -> otherRequirements
  //    requirements
  //  }
}
