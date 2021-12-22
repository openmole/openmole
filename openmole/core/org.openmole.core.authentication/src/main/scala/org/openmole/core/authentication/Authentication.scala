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

package org.openmole.core.authentication

import java.io.File
import java.util.UUID

import org.openmole.core.serializer.SerializerService
import org.openmole.core.workspace.Workspace
import org.openmole.tool.file._
import org.openmole.tool.logger.JavaLogger

import scala.util.{ Failure, Success, Try }

object Authentication extends JavaLogger {

  val pattern = "[-0-9A-z]+\\.key"

  //override def /(name: String) = new Persistent(new File(baseDir, name)) with Authentication

  def category[T](implicit m: Manifest[T]): String = m.runtimeClass.getCanonicalName

  def set[T](obj: T)(implicit m: Manifest[T], authenticationStore: AuthenticationStore, serializerService: SerializerService): Unit = saveAs[T]("0.key", obj)

  def save[T: Manifest](t: T, eq: (T, T) ⇒ Boolean)(implicit authenticationStore: AuthenticationStore, serializerService: SerializerService) =
    inCategory(authenticationStore, category[T]).toList.filter {
      case (_, t1: Any) ⇒ eq(t, t1.asInstanceOf[T])
    }.headOption match {
      case Some((fileName, _)) ⇒ saveAs(fileName, t)
      case None                ⇒ saveAs(UUID.randomUUID.toString + ".key", t)
    }

  def remove[T: Manifest](t: T, eq: (T, T) ⇒ Boolean)(implicit authenticationStore: AuthenticationStore, serializerService: SerializerService) =
    inCategory(authenticationStore, category[T]).toList.filter {
      case (_, t1: Any) ⇒ eq(t, t1.asInstanceOf[T])
    }.foreach {
      case (fileName, _) ⇒
        val cat = new File(authenticationStore.baseDir, category[T])
        new File(cat, fileName).delete()
    }

  def saveAs[T](fileName: String, obj: T)(implicit m: Manifest[T], authenticationStore: AuthenticationStore, serializerService: SerializerService): Unit =
    (authenticationStore / category[T]).save(obj, fileName)

  def clear[T](implicit m: Manifest[T], authenticationStore: AuthenticationStore): Unit = (authenticationStore / category[T]).delete()

  protected def inCategory(store: AuthenticationStore, category: String)(implicit serializerService: SerializerService) = {
    val d = new File(store.baseDir, category)
    d.listFilesSafe { (f: File) ⇒ f.getName.matches(Authentication.pattern) }.flatMap {
      f ⇒
        Try(store.loadFile[Any](f)) match {
          case Success(t) ⇒ Some(f.getName → t)
          case Failure(e) ⇒
            Log.logger.log(Log.WARNING, "Error while deserializing an authentication", e)
            None
        }
    }.toSeq
  }

  def allByCategory(implicit store: AuthenticationStore, serializerService: SerializerService): Map[String, Seq[Any]] =
    store.baseDir.listFilesSafe { (f: File) ⇒ f.isDirectory }.map { d ⇒ d.getName → inCategory(store, d.getName).map(_._2) }.toMap

}
