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

import java.io.{ InputStream, OutputStream }

import fr.iscpif.gridscale.storage.{ FileType, Storage ⇒ GSStorage }
import org.openmole.core.batch.storage.Storage

trait GridScaleStorage <: Storage {
  val storage: GSStorage

  def child(parent: String, child: String): String = storage.child(parent, child)
  protected def _exists(path: String): Boolean = storage.exists(path)
  protected def _listNames(path: String): Seq[String] = storage.listNames(path)
  protected def _list(path: String): Seq[(String, FileType)] = storage.list(path)
  protected def _makeDir(path: String): Unit = storage.makeDir(path)
  protected def _rmDir(path: String): Unit = storage.rmDir(path)
  protected def _rmFile(path: String): Unit = storage.rmFile(path)
  protected def _openInputStream(path: String): InputStream = storage.openInputStream(path)
  protected def _openOutputStream(path: String): OutputStream = storage.openOutputStream(path)
  protected def _mv(from: String, to: String) = storage.mv(from, to)
}
