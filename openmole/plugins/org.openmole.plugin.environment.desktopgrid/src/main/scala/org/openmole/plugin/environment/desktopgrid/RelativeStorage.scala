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

import java.io.{ InputStream, File }
import fr.iscpif.gridscale.storage.{ LocalStorage, Storage }

class RelativeStorage(base: File) extends Storage {
  val localStorage = LocalStorage()

  def relativise(path: String) = new File(base, path).getPath
  override def child(parent: String, child: String) = new File(parent, child).toString
  override def exists(path: String) = localStorage.exists(relativise(path))
  override def _list(path: String) = localStorage._list(relativise(path))
  override def _makeDir(path: String) = localStorage._makeDir(relativise(path))
  override def _rmDir(path: String) = localStorage._rmDir(relativise(path))
  override def _rmFile(path: String) = localStorage._rmFile(relativise(path))
  override def _mv(from: String, to: String) = localStorage._mv(relativise(from), relativise(to))
  override def _read(path: String) = localStorage._read(relativise(path))
  override def _write(is: InputStream, path: String) = localStorage._write(is, relativise(path))
}