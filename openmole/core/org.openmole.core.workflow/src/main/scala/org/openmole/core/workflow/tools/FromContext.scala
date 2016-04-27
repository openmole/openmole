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

import java.io.File

import org.openmole.core.tools.io._
import org.openmole.core.workflow.data._

import scalaz._
import Scalaz._

object FromContext {

  implicit def fromTToContext[T](t: T): FromContext[T] = FromContext.value[T](t)

  def codeToFromContext[T: Manifest](code: String): FromContext[T] =
    new FromContext[T] {
      @transient lazy val proxy = ScalaWrappedCompilation.dynamic[T](code)
      override def from(context: ⇒ Context)(implicit rng: RandomProvider): T = proxy().from(context)
    }

  implicit def codeToFromContextFloat(code: String) = codeToFromContext[Float](code)
  implicit def codeToFromContextDouble(code: String) = codeToFromContext[Double](code)
  implicit def codeToFromContextLong(code: String) = codeToFromContext[Long](code)
  implicit def codeToFromContextInt(code: String) = codeToFromContext[Int](code)
  implicit def codeToFromContextBigDecimal(code: String) = codeToFromContext[BigDecimal](code)
  implicit def codeToFromContextBigInt(code: String) = codeToFromContext[BigInt](code)
  implicit def codeToFromContextBoolean(condition: String) = codeToFromContext[Boolean](condition)
  implicit def codeToFromContextFile(code: String) = codeToFromContext[File](code)
  implicit def codeToFromContextString(code: String) = codeToFromContext[String](code)

  def value[T](t: T): FromContext[T] =
    new FromContext[T] {
      def from(context: ⇒ Context)(implicit rng: RandomProvider): T = t
    }

  def apply[T](f: (Context, RandomProvider) ⇒ T) =
    new FromContext[T] {
      def from(context: ⇒ Context)(implicit rng: RandomProvider) = f(context, rng)
    }

  implicit val monad = new Functor[FromContext] with Monad[FromContext] {
    override def bind[A, B](fa: FromContext[A])(f: (A) ⇒ FromContext[B]): FromContext[B] = FromContext {
      (context, rng) ⇒
        val res = fa.from(context)(rng)
        f(res).from(context)(rng)
    }

    override def point[A](a: ⇒ A): FromContext[A] = FromContext.value(a)
  }

  implicit def booleanToCondition(b: Boolean) = FromContext.value(b)

  implicit def booleanPrototypeIsCondition(p: Prototype[Boolean]) = new FromContext[Boolean] {
    override def from(context: ⇒ Context)(implicit rng: RandomProvider) = context(p)
  }

  implicit class ConditionDecorator(f: Condition) {

    def unary_! = f.map(v ⇒ !v)

    def &&(d: Condition) =
      for {
        c1 ← f()
        c2 ← d()
      } yield c1 && c2

    def ||(d: Condition) =
      for {
        c1 ← f()
        c2 ← d()
      } yield c1 || c2

  }

}

trait FromContext[+T] {
  def from(context: ⇒ Context)(implicit rng: RandomProvider): T
}

