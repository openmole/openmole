/*
 * Copyright (C) 2015 Romain Reuillon
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
package org.openmole.plugin.environment.desktopgrid

import java.io.File
import java.nio.file.Paths

import fr.iscpif.gridscale.storage.LocalStorage

class RelativeStorage(base: File) extends LocalStorage {
  def relativise(path: String) = new File(base, path).getPath
  override def child(parent: String, child: String) = new File(parent, child).toString
  override def exists(path: String) = super.exists(relativise(path))
  override def _list(path: String) = super._list(relativise(path))
  override def _makeDir(path: String) = super._makeDir(relativise(path))
  override def _rmDir(path: String) = super._rmDir(relativise(path))
  override def _rmFile(path: String) = super._rmFile(relativise(path))
  override def _mv(from: String, to: String) = super._mv(relativise(from), relativise(to))
  override protected def _openInputStream(path: String) = super._openInputStream(relativise(path))
  override protected def _openOutputStream(path: String) = super._openOutputStream(relativise(path))
}