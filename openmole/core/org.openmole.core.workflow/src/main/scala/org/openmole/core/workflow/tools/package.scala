/*
 * Copyright (C) 2012 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow

import scala.concurrent.stm._
import org.openmole.tool.file._

import scala.ref.WeakReference
import cats._
import cats._
import org.openmole.core.exception.UserBadDataError
import org.openmole.core.expansion.{ FromContext, ToFromContext }

package tools {

  import org.openmole.core.exception.UserBadDataError
  import org.openmole.core.expansion.{ Expandable, ExpandedString, FromContext, ToFromContext }
  import org.openmole.core.fileservice.FileService
  import org.openmole.core.workspace.NewFile

  trait ToolsPackage {

    implicit def objectToWeakReferenceConverter[T <: AnyRef](v: T) = new WeakReference[T](v)

    implicit class RefDecorator[T](r: Ref[T]) {
      def getUpdate(t: T ⇒ T): T = atomic { implicit txn ⇒ val v = r(); r() = t(v); v }
    }

    implicit class RefLongDecorator(r: Ref[Long]) {
      def next = r getUpdate (_ + 1)
    }

    implicit class VectorLensDecorator[T, U](l: monocle.Lens[T, Vector[U]]) {
      def add(u: U) = l.modify(_ ++ Seq(u))
    }

    implicit class SeqOfEndofunctorDecorator[T](f: Seq[T ⇒ T]) {
      def sequence: T ⇒ T = { t: T ⇒ f.foldLeft(t)((a, f) ⇒ f(a)) }
    }

    implicit def seqOfFunction[T](s: Seq[T ⇒ T]) = s.sequence
    implicit def arrayOfFunction[T](s: Array[T ⇒ T]) = s.toSeq.sequence

    def OptionalArgument = tools.OptionalArgument
    type OptionalArgument[T] = tools.OptionalArgument[T]

    implicit def optionalArgumentToOption[T](optionalArgument: OptionalArgument[T]) = optionalArgument.option
    implicit def fromStringToExpandedStringOptionalArgument(s: String) = OptionalArgument[FromContext[String]](Some(ExpandedString(s)))

    def Expression[T] = new {
      def apply[S](s: S)(implicit expandable: Expandable[S, T]) = expandable.expand(s)
    }

  }
}

