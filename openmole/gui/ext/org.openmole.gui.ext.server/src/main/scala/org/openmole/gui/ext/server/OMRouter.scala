package org.openmole.gui.ext.server

//import boopickle.Default.*
import cats.effect.IO
import org.http4s.HttpRoutes

import scala.reflect.ClassTag
import java.nio.ByteBuffer

/*
 * Copyright (C) 30/11/16 // mathieu.leclaire@openmole.org
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
//
//object AutowireServer extends autowire.Server[ByteBuffer, Pickler, Pickler] {
//
//  def read[R: Pickler](p: ByteBuffer) = Unpickle[R].fromBytes(p)
//
//  def write[R: Pickler](r: R) = Pickle.intoBytes(r)
//
//}
//
//object OMRouter {
//  def route[T: ClassTag] = reflect.classTag[T].runtimeClass.getCanonicalName.replace('.', '/')
//  def apply[T: ClassTag](router: AutowireServer.Router): OMRouter = new OMRouter(router, route[T])
//}

case class OMRouter(router: HttpRoutes[IO])