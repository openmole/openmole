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
import org.openmole.plugin.environment.jsaga.JSAGAAttributes._
import scala.math._

object JSAGAEnvironment {
  val DefaultRequieredMemory  = new ConfigurationLocation("JSAGAEnvironment", "RequieredMemory")

  Workspace += (DefaultRequieredMemory, "1024")
}


abstract class JSAGAEnvironment(val inAttributes: Option[Map[String, String]]) extends BatchEnvironment {
  import JSAGAEnvironment._
    
  val memory = max(Workspace.preferenceAsInt(DefaultRequieredMemory), memorySizeForRuntime).toString
  val attributes = inAttributes match {
    case Some(map) => if(map.contains(MEMORY)) map else map + {MEMORY -> memory}
    case None => Map(MEMORY -> memory)
  }
}
