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
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.environment.jsaga

import org.openmole.core.implementation.execution.batch.BatchEnvironment
import org.openmole.misc.workspace.ConfigurationLocation
import org.openmole.plugin.environment.jsaga.internal.Activator
import org.openmole.plugin.environment.jsaga.JSAGAAttributes._

object JSAGAEnvironment {
    val DefaultRequieredMemory  = new ConfigurationLocation("JSAGAEnvironment", "RequieredMemory")

    Activator.getWorkspace += (DefaultRequieredMemory, "1024")
}


abstract class JSAGAEnvironment(val inAttributes: Option[Map[String, String]], inRequieredMemory: Option[Int]) extends BatchEnvironment(inRequieredMemory) {
    import JSAGAEnvironment._
    
    val attributes = inAttributes match {
      case Some(map) => if(map.contains(MEMORY)) map else map + {MEMORY -> Activator.getWorkspace.preference(DefaultRequieredMemory)}
      case None => Map(MEMORY -> Activator.getWorkspace.preference(DefaultRequieredMemory))
    }
}
