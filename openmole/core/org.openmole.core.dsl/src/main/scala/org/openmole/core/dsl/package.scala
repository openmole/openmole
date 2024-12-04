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

package dsl:

  import org.openmole.core.context._
  import org.openmole.core.logconfig.LoggerConfig
  import cats._
  import org.openmole.core.workflow.mole

  import org.openmole.tool.crypto.Cypher
  import squants.information._

  import scala.collection.immutable.NumericRange

  trait DSLPackage extends Commands
    with Serializer
    with Classes
    with workflow.ExportedPackage
    with cats.instances.AllInstances:

    implicit lazy val implicitContext: Context = Context.empty

    //implicit lazy val workspace = Workspace
    lazy val logger = LoggerConfig

    //implicit def decrypt = Decrypt(workspace)

    implicit def stringToFile(path: String): File = File(path)

    export squants.time.TimeConversions.TimeConversions
    //implicit def timeConversion[N: Numeric](n: N): squants.Time.TimeConversions = squants.time.TimeConversions.TimeConversions(n)
    extension [N: Numeric](n: N)
      def nanosecond = n nanoseconds
      def microsecond = n microseconds
      def millisecond = n milliseconds
      def second = n seconds
      def minute = n minutes
      def hour = n hours
      def day = n days

    export squants.information.InformationConversions.InformationConversions

    //implicit def informationUnitConversion[N: Numeric](n: N): squants.information.InformationConversions.InformationConversions = squants.information.InformationConversions.InformationConversions(n)
    extension [N: Numeric](n: N)
      def byte = n bytes
      def kilobyte = n kilobytes
      def megabyte = n megabytes
      def gigabyte = n gigabytes
      def terabyte = n terabytes
      def petabyte = n petabytes
      def exabyte = n exabytes
      def zettabyte = n zettabytes
      def yottabyte = n yottabytes

    //implicit def intToMemory(i: Int): Information = (i megabytes)
    //implicit def intToMemoryOptional(i: Int): OptionalArgument[Information] = OptionalArgument(intToMemory(i))

    def encrypt(s: String)(implicit cypher: Cypher) = cypher.encrypt(s)

    implicit def seqIsFunctor: Functor[Seq] = new Functor[Seq]:
      override def map[A, B](fa: Seq[A])(f: (A) ⇒ B): Seq[B] = fa.map(f)

    type Data = File

    //export org.openmole.tool.collection.DoubleRangeDecorator
    @inline implicit class DoubleWrapper(d: Double):
      infix def to(h: Double) = org.openmole.tool.collection.DoubleRange.to(d, h)
      infix def until(h: Double) = org.openmole.tool.collection.DoubleRange.until(d, h)

    //implicit def doubleRange(d: Double): org.openmole.tool.collection.DoubleRangeDecorator = new org.openmole.tool.collection.DoubleRangeDecorator(d)
    export Predef.longWrapper
    export Predef.intWrapper
    export Predef.doubleWrapper


package object dsl extends DSLPackage
