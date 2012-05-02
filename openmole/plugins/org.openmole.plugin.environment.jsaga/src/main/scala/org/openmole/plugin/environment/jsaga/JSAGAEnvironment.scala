/*
 * Copyright (C) 2010 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.jsaga

import org.openmole.core.batch.environment.BatchEnvironment
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.misc.workspace.Workspace
import scala.math._
import org.openmole.plugin.environment.jsaga._
import Requirement._

object JSAGAEnvironment {
  val DefaultRequieredMemory = new ConfigurationLocation("JSAGAEnvironment", "RequieredMemory")

  Workspace += (DefaultRequieredMemory, "1024")
}

abstract class JSAGAEnvironment extends BatchEnvironment {
  import JSAGAEnvironment._

  val memory = max(Workspace.preferenceAsInt(DefaultRequieredMemory), runtimeMemory).toString

  def requirements: Map[String, String]
  val allRequirements = {
    requirements.get(MEMORY) match {
      case Some(m) ⇒ if (m < memory) requirements + (MEMORY -> memory) else requirements
      case None ⇒ requirements + (MEMORY -> memory)
    }
  }

}
