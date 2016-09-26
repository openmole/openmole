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

package org.openmole.core

import org.openmole.core.context.Val
import org.openmole.core.macros.ExtractValName
import org.openmole.core.macros.ExtractValName._

import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration
import scala.reflect.macros.blackbox.{ Context ⇒ MContext }

package dsl {

  import org.openmole.core.context._
  import org.openmole.core.logging.LoggerService
  import org.openmole.core.workspace.Workspace

  import scalaz.Functor

  trait DSLPackage <: Commands
      with Serializer
      with Classes
      with workflow.ExportedPackage {

    def valImpl[T: c.WeakTypeTag](c: MContext): c.Expr[Val[T]] = {
      import c.universe._
      val n = getValName(c)
      val wt = weakTypeTag[T].tpe
      c.Expr[Val[T]](q"Prototype[$wt](${n})")
    }

    implicit lazy val implicitContext = Context.empty

    lazy val workspace = Workspace
    lazy val logger = LoggerService

    implicit def authenticationProvider = workspace.authenticationProvider

    implicit def stringToFile(path: String) = File(path)

    implicit def durationInt(n: Int) = duration.DurationInt(n)

    implicit def durationLong(n: Long) = duration.DurationLong(n)

    implicit def stringToDuration(s: String): FiniteDuration = org.openmole.core.tools.service.stringToDuration(s)

    def encrypt(s: String) = Workspace.encrypt(s)

    implicit def seqIsFunctor = new Functor[Seq] {
      override def map[A, B](fa: Seq[A])(f: (A) ⇒ B): Seq[B] = fa.map(f)
    }

    type Val[T] = org.openmole.core.context.Val[T]
  }

}

package object dsl extends DSLPackage {
  import scala.language.experimental.macros
  def Val[T]: Val[T] = macro valImpl[T]
  def Val[T: Manifest](name: String): Val[T] = Val(name)
}
