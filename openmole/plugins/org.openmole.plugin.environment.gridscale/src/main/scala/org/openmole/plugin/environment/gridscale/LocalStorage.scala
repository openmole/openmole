/*
 * Copyright (C) 2012 reuillon
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

package org.openmole.plugin.environment.gridscale

import java.io.File

import fr.iscpif.gridscale.storage.{ LocalStorage ⇒ GSLocalStorage }
import org.openmole.core.batch.storage.SimpleStorage
import org.openmole.tool.file._

object LocalStorage {
  def apply(root: String) = {
    val _root = root
    new LocalStorage {
      override val root: String = _root
    }
  }
}

trait LocalStorage extends GridScaleStorage {
  val root: String
  val authentication: Unit = Unit
  val storage = new GSLocalStorage {}

  override protected def _mv(from: String, to: String): Unit = new File(from).move(new File(to))
  def home = System.getProperty("user.home")
}

