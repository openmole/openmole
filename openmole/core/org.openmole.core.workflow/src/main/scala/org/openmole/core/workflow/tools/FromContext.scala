/*
 * Copyright (C) 2014 Romain Reuillon
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

package org.openmole.core.workflow.tools

import org.openmole.core.exception._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.dsl._
import org.openmole.tool.cache._

import scalaz._
import Scalaz._

object FromContext {

  implicit val applicative: Applicative[FromContext] = new Applicative[FromContext] {
    override def ap[A, B](fa: ⇒ FromContext[A])(f: ⇒ FromContext[(A) ⇒ B]): FromContext[B] =
      new FromContext[B] {
        override def from(context: ⇒ Context)(implicit rng: RandomProvider): B = {
          val res = fa.from(context)(rng)
          f.from(context)(rng)(res)
        }

        override def validate(inputs: Seq[Val[_]]): Seq[Throwable] =
          fa.validate(inputs) ++ f.validate(inputs)
      }

    override def point[A](a: ⇒ A): FromContext[A] = FromContext.value(a)
  }

  def codeToFromContext[T: Manifest](code: String): FromContext[T] =
    new FromContext[T] {
      val proxy = Cache(ScalaWrappedCompilation.dynamic[T](code))
      override def from(context: ⇒ Context)(implicit rng: RandomProvider): T = proxy()().from(context)
      override def validate(inputs: Seq[Val[_]]): Seq[Throwable] = proxy().validate(inputs).toSeq
    }

  implicit def codeToFromContextFloat(code: String) = codeToFromContext[Float](code)
  implicit def codeToFromContextDouble(code: String) = codeToFromContext[Double](code)
  implicit def codeToFromContextLong(code: String) = codeToFromContext[Long](code)
  implicit def codeToFromContextInt(code: String) = codeToFromContext[Int](code)
  implicit def codeToFromContextBigDecimal(code: String) = codeToFromContext[BigDecimal](code)
  implicit def codeToFromContextBigInt(code: String) = codeToFromContext[BigInt](code)
  implicit def codeToFromContextBoolean(condition: String) = codeToFromContext[Boolean](condition)

  implicit def fileToString(f: File): FromContext[String] = ExpandedString(f.getPath)
  implicit def stringToString(s: String): FromContext[String] = ExpandedString(s)
  implicit def stringToFile(s: String): FromContext[File] = ExpandedString(s).map(s ⇒ File(s))
  implicit def fileToFile(f: File): FromContext[File] = ExpandedString(f.getPath).map(s ⇒ File(s))

  implicit def fromTToContext[T](t: T): FromContext[T] = FromContext.value[T](t)

  def prototype[T](p: Prototype[T]) =
    new FromContext[T] {
      override def from(context: ⇒ Context)(implicit rng: RandomProvider) = context(p)
      def validate(inputs: Seq[Val[_]]): Seq[Throwable] = {
        if (inputs.exists(_ == p)) Seq.empty else Seq(new UserBadDataError(s"Prototype $p not found"))
      }
    }

  def value[T](t: T): FromContext[T] =
    new FromContext[T] {
      def from(context: ⇒ Context)(implicit rng: RandomProvider): T = t
      def validate(inputs: Seq[Val[_]]): Seq[Throwable] = Seq.empty
    }

  def apply[T](f: (Context, RandomProvider) ⇒ T) =
    new FromContext[T] {
      def from(context: ⇒ Context)(implicit rng: RandomProvider) = f(context, rng)
      def validate(inputs: Seq[Val[_]]): Seq[Throwable] = Seq.empty
    }

  implicit def booleanToCondition(b: Boolean) = FromContext.value(b)
  implicit def booleanPrototypeIsCondition(p: Prototype[Boolean]) = prototype(p)

  implicit class ConditionDecorator(f: Condition) {
    def unary_! = f.map(v ⇒ !v)

    def &&(d: Condition): Condition =
      new FromContext[Boolean] {
        override def from(context: ⇒ Context)(implicit rng: RandomProvider): Boolean = f.from(context) && d.from(context)
        override def validate(inputs: Seq[Val[_]]): Seq[Throwable] = f.validate(inputs) ++ d.validate(inputs)
      }

    def ||(d: Condition): Condition =
      new FromContext[Boolean] {
        override def from(context: ⇒ Context)(implicit rng: RandomProvider): Boolean = f.from(context) || d.from(context)
        override def validate(inputs: Seq[Val[_]]): Seq[Throwable] = f.validate(inputs) ++ d.validate(inputs)
      }
  }

  implicit class ExpandedStringOperations(s1: FromContext[String]) {
    def +(s2: FromContext[String]) = (s1 |@| s2) apply (_ + _)
  }

  implicit class FromContextFileDecorator(f: FromContext[File]) {
    def exists = f.map(_.exists)
    def isEmpty = f.map(_.isEmpty)
    def /(path: FromContext[String]) = (f |@| path)(_ / _)
  }

}

trait FromContext[+T] {
  def from(context: ⇒ Context)(implicit rng: RandomProvider): T
  def validate(inputs: Seq[Prototype[_]]): Seq[Throwable]
}

object Expandable {
  def apply[S, T](f: S ⇒ FromContext[T]) = new Expandable[S, T] {
    override def expand(s: S): FromContext[T] = f(s)
  }

  implicit def stringToString = Expandable[String, String](FromContext.stringToString)
  implicit def stringToFile = Expandable[String, File](s ⇒ FromContext.stringToString(s).map(File(_)))
  implicit def fileToFile = Expandable[File, File](FromContext.fileToFile)
}

trait Expandable[S, T] {
  def expand(s: S): FromContext[T]
}

