/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.core.implementation.preference

import org.openmole.misc.workspace.Workspace
import java.io.{ FileNotFoundException, File }

object Preferences {
  def apply() = instance

  def empty = new Preferences(List(), "", false)

  private var instance = {
    try {
      Workspace.persistent("gui").load("preferences") match {
        case p: Preferences ⇒ p
        case _ ⇒  empty
      }
    }
    catch {
      case fnde: FileNotFoundException ⇒ empty
    }
  }

  private def update(pref: Preferences) = instance = pref

  def setServers(s: List[String]) = update(instance.copy(servers = s))

  def setSandBox(s: String) = update(instance.copy(sandbox = s))

  def setEmbeddRessources(b: Boolean) = update(instance.copy(embeddResources = b))

  def dumpFile = Workspace.persistent("gui").save(instance, "preferences")
}

case class Preferences(servers: List[String],
                       sandbox: String,
                       embeddResources: Boolean)