/*
 * Copyright (C) 23/09/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workspace

import java.util.UUID

import org.openmole.core.tools.service.Logger
import org.openmole.tool.file._
import scala.util.{ Failure, Success, Try }
import java.io.File

object Authentication extends Logger {
  val pattern = "[-0-9A-z]+\\.key"
}

import Authentication.Log

trait Authentication <: Persistent {

  def category[T](implicit m: Manifest[T]): String = m.runtimeClass.getCanonicalName

  def set[T](obj: T)(implicit m: Manifest[T]): Unit = saveAs[T]("0.key", obj)

  def save[T: Manifest](t: T, eq: (T, T) ⇒ Boolean) =
    allByCategory.getOrElse(category[T], Seq.empty).toList.filter {
      case (_, t1: T) ⇒ eq(t, t1)
    }.headOption match {
      case Some((fileName, _)) ⇒ Workspace.authentications.saveAs(fileName, t)
      case None                ⇒ Workspace.authentications.saveAs(UUID.randomUUID.toString + ".key", t)
    }

  protected def saveAs[T](fileName: String, obj: T)(implicit m: Manifest[T]): Unit =
    save(obj, fileName, Some(category[T]))

  def clean[T](implicit m: Manifest[T]): Unit = super.clean(Some(m.runtimeClass.getCanonicalName))

  def allByCategory: Map[String, Seq[(String, Any)]] = synchronized {
    baseDir.listFiles { f: File ⇒ f.isDirectory }.map { d ⇒
      d.getName -> d.listFiles { f: File ⇒ f.getName.matches(Authentication.pattern) }.flatMap {
        f ⇒
          Try(loadFile[Any](f)) match {
            case Success(t) ⇒ Some(f.getName, t)
            case Failure(e) ⇒
              Log.logger.log(Log.WARNING, "Error while deserialising an authentication", e)
              None
          }
      }.toSeq
    }.toMap
  }
}
