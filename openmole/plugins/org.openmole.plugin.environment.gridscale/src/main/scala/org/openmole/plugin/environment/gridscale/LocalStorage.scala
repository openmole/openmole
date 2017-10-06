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

import java.io.InputStream

import org.openmole.core.communication.storage._
import org.openmole.plugin.environment.batch.storage._
import org.openmole.tool.file._

object LocalStorage {

  import freedsl.dsl._
  import gridscale.local

  implicit def isStorage(implicit interpreter: local.LocalInterpreter) = new StorageInterface[LocalStorage] {
    override def home(t: LocalStorage) = local.home[DSL].eval
    override def child(t: LocalStorage, parent: String, child: String): String = (File(parent) / child).getAbsolutePath
    override def parent(t: LocalStorage, path: String): Option[String] = Option(File(path).getParent)
    override def name(t: LocalStorage, path: String): String = File(path).getName
    override def exists(t: LocalStorage, path: String): Boolean = local.exists[DSL](path).eval
    override def list(t: LocalStorage, path: String): Seq[gridscale.ListEntry] = local.list[DSL](path).eval
    override def makeDir(t: LocalStorage, path: String): Unit = local.makeDir[DSL](path).eval
    override def rmDir(t: LocalStorage, path: String): Unit = local.rmDir[DSL](path).eval
    override def rmFile(t: LocalStorage, path: String): Unit = local.rmFile[DSL](path).eval
    override def mv(t: LocalStorage, from: String, to: String): Unit = local.mv[DSL](from, to).eval

    override def upload(t: LocalStorage, src: File, dest: String, options: TransferOptions): Unit =
      StorageInterface.upload(false, local.writeFile[DSL](_, _).eval)(src, dest, options)
    override def download(t: LocalStorage, src: String, dest: File, options: TransferOptions): Unit =
      StorageInterface.download(false, local.readFile[DSL, Unit](_, _).eval)(src, dest, options)
  }

}

case class LocalStorage()

//
//trait LocalStorage extends GridScaleStorage {
//  val root: String
//  val authentication: Unit = Unit
//  val storage = new GSLocalStorage {}
//
//  override protected def _mv(from: String, to: String): Unit = new File(from).move(new File(to))
//  def home = System.getProperty("user.home")
//}

