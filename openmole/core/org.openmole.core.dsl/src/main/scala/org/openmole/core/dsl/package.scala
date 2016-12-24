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

import org.openmole.core.macros.ExtractValName._

import scala.reflect.macros.blackbox.{ Context ⇒ MContext }
import java.time.{ Duration ⇒ JDuration }

package dsl {

  import org.openmole.core.context._
  import org.openmole.core.logging.LoggerService
  import org.openmole.core.workspace.Workspace
  import cats._
  import org.openmole.core.tools.io.FromString
  import squants._
  import squants.information._

  trait DSLPackage <: Commands
      with Serializer
      with Classes
      with workflow.ExportedPackage
      with cats.instances.AllInstances {

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

    implicit def timeConversion[N: Numeric](n: N) = squants.time.TimeConversions.TimeConversions(n)
    implicit class singularTimeConversion[N: Numeric](n: N) {
      def nanosecond = n nanoseconds
      def microsecond = n microseconds
      def millisecond = n milliseconds
      def second = n seconds
      def minute = n minutes
      def hour = n hours
      def day = n days
    }

    implicit def informationUnitConversion[N: Numeric](n: N) = squants.information.InformationConversions.InformationConversions(n)
    implicit class singularInformationUnitConversion[N: Numeric](n: N) {
      def byte = n bytes
      def kilobyte = n kilobytes
      def megabyte = n megabytes
      def gigabyte = n gigabytes
      def terabyte = n terabytes
      def petabyte = n petabytes
      def exabyte = n exabytes
      def zettabyte = n zettabytes
      def yottabyte = n yottabytes
    }

    implicit def stringToTime(s: String): Time = implicitly[FromString[Time]].apply(s)
    implicit def stringToTimeOptional(s: String): OptionalArgument[Time] = OptionalArgument(stringToTime(s))
    implicit def intToMemory(i: Int): Information = (i megabytes)
    implicit def intToMemoryOptional(i: Int): OptionalArgument[Information] = OptionalArgument(intToMemory(i))

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
