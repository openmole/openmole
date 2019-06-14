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

package org.openmole.core.expansion

import org.openmole.core.context._
import org.openmole.core.exception._
import org.openmole.tool.cache._
import org.openmole.tool.random._
import org.openmole.tool.file._
import cats._
import cats.implicits._
import org.openmole.core.fileservice.FileService
import org.openmole.core.workspace.NewFile

import scala.annotation.tailrec

trait LowPriorityToFromContext {
  implicit def fromTToContext[T] = ToFromContext[T, T](t ⇒ FromContext.value[T](t))
}

object ToFromContext extends LowPriorityToFromContext {
  import FromContext._

  def apply[F, T](func: F ⇒ FromContext[T]): ToFromContext[F, T] = new ToFromContext[F, T] {
    def apply(f: F) = func(f)
  }

  implicit def functionToFromContext[T] =
    ToFromContext[(Context ⇒ T), FromContext[T]](f ⇒ FromContext(p ⇒ f(p.context)))

  implicit def codeToFromContextFloat = ToFromContext(codeToFromContext[Float])
  implicit def codeToFromContextDouble = ToFromContext(codeToFromContext[Double])
  implicit def codeToFromContextLong = ToFromContext(codeToFromContext[Long])
  implicit def codeToFromContextInt = ToFromContext(codeToFromContext[Int])
  implicit def codeToFromContextBigDecimal = ToFromContext(codeToFromContext[BigDecimal])
  implicit def codeToFromContextBigInt = ToFromContext(codeToFromContext[BigInt])
  implicit def codeToFromContextBoolean = ToFromContext(codeToFromContext[Boolean])

  implicit def fileToString = ToFromContext[File, String](f ⇒ ExpandedString(f.getPath))
  implicit def stringToString = ToFromContext[String, String](s ⇒ ExpandedString(s))
  implicit def stringToFile = ToFromContext[String, File](s ⇒ ExpandedString(s).map(s ⇒ File(s)))
  implicit def fileToFile = ToFromContext[File, File](f ⇒ ExpandedString(f.getPath).map(s ⇒ File(s)))
  implicit def fromContextToFromContext[T] = ToFromContext[FromContext[T], T](identity)

  implicit def booleanToCondition = ToFromContext[Boolean, Boolean](b ⇒ FromContext.value(b))
  implicit def prototypeIsFromContext[T]: ToFromContext[Val[T], T] = ToFromContext[Val[T], T](p ⇒ prototype(p))
}

trait ToFromContext[F, T] {
  def apply(f: F): FromContext[T]
}

trait LowPriorityFromContext {
  def contextConverter[F, T](f: F)(implicit tfc: ToFromContext[F, T]): FromContext[T] = tfc(f)

  /**
   * implicit converter of a value to a FromContext (using implicit [[LowPriorityToFromContext]])
   * @param t
   * @tparam T
   * @return
   */
  implicit def fromTToContext[T](t: T): FromContext[T] = contextConverter(t)
}

object FromContext extends LowPriorityFromContext {

  /**
   * A [[FromContext]] can be seen as a monad
   */
  object asMonad {
    implicit val monad: Monad[FromContext] = new Monad[FromContext] {
      def tailRecM[A, B](a: A)(f: A ⇒ FromContext[Either[A, B]]): FromContext[B] = {

        @tailrec def computeB(a: A, context: Context)(implicit rng: RandomProvider, newFile: NewFile, fileService: FileService): B = {
          f(a)(context) match {
            case Left(a)  ⇒ computeB(a, context)
            case Right(b) ⇒ b
          }
        }

        FromContext { p ⇒
          import p._
          computeB(a, context)
        }
      }

      override def flatMap[A, B](fa: FromContext[A])(f: A ⇒ FromContext[B]): FromContext[B] = FromContext { p ⇒
        import p._
        val faVal = fa(context)
        f(faVal)(context)
      }
      override def pure[A](x: A): FromContext[A] = FromContext.value(x)
    }
  }

  /**
   * Implicitly define an Applicative on FromContext
   */
  implicit val applicative: Applicative[FromContext] = new Applicative[FromContext] {
    override def pure[A](x: A): FromContext[A] = FromContext.value(x)
    override def ap[A, B](ff: FromContext[(A) ⇒ B])(fa: FromContext[A]): FromContext[B] =
      FromContext[B] { p ⇒
        import p._
        val res = fa.from(context)
        ff.from(context).apply(res)
      } validate { p ⇒
        import p._
        fa.validate(inputs) ++ ff.validate(inputs)
      }
  }

  /**
   * Convert scala code to a FromContext (code is compiled by [[ScalaCompilation]])
   *
   * @param code
   * @tparam T
   * @return
   */
  def codeToFromContext[T: Manifest](code: String) = {
    val proxy = Cache(ScalaCompilation.dynamic[T](code))

    FromContext[T] { p ⇒
      import p._
      proxy.apply.apply.from(context)
    } validate { p ⇒
      import p._
      proxy().validate(inputs).toSeq
    }
  }

  implicit def functionToFromContext[T](f: (Context ⇒ T)) = contextConverter(f)
  implicit def codeToFromContextFloat(s: String) = contextConverter[String, Float](s)
  implicit def codeToFromContextDouble(s: String) = contextConverter[String, Double](s)
  implicit def codeToFromContextLong(s: String) = contextConverter[String, Long](s)
  implicit def codeToFromContextInt(s: String) = contextConverter[String, Int](s)
  implicit def codeToFromContextBigDecimal(s: String) = contextConverter[String, BigDecimal](s)
  implicit def codeToFromContextBigInt(s: String) = contextConverter[String, BigInt](s)
  implicit def codeToFromContextBoolean(s: String) = contextConverter[String, Boolean](s)

  implicit def fileToString(f: File) = contextConverter[File, String](f)
  implicit def stringToString(s: String) = contextConverter[String, String](s)
  implicit def stringToFile(s: String) = contextConverter[String, File](s)
  implicit def fileToFile(f: File) = contextConverter[File, File](f)

  implicit def booleanToCondition(b: Boolean) = contextConverter[Boolean, Boolean](b)
  implicit def prototypeIsFromContext[T](p: Val[T]) = contextConverter[Val[T], T](p)

  /**
   * FromContext for a given prototype
   * @param p
   * @tparam T
   * @return
   */
  def prototype[T](p: Val[T]) =
    FromContext[T] { param ⇒
      import param._
      context(p)
    } validate { param ⇒
      import param._
      if (inputs.exists(_ == p)) Seq.empty else Seq(new UserBadDataError(s"Prototype $p not found"))
    }

  /**
   * From context for a given value
   * @param t
   * @tparam T
   * @return
   */
  def value[T](t: T): FromContext[T] = FromContext[T] { _ ⇒ t }

  /**
   * Parameters wrap a Context and implicit services
   * @param context
   * @param random
   * @param newFile
   * @param fileService
   */
  case class Parameters(context: Context, implicit val random: RandomProvider, implicit val newFile: NewFile, implicit val fileService: FileService)
  case class ValidationParameters(inputs: Seq[Val[_]], implicit val newFile: NewFile, implicit val fileService: FileService)

  /**
   * Construct a FromContext from a function of [[Parameters]]
   * @param f
   * @tparam T
   * @return
   */
  def apply[T](f: Parameters ⇒ T): FromContext[T] = new FromContext[T](f, _ ⇒ Seq.empty)

  /**
   * Operators for boolean FromContext ([[Condition]] ~ FromContext[Boolean])
   * @param f
   */
  implicit class ConditionDecorator(f: Condition) {
    def unary_! = f.map(v ⇒ !v)

    def &&(d: Condition): Condition =
      FromContext[Boolean] { p ⇒
        import p._
        f.from(context) && d.from(context)
      } validate { p ⇒
        import p._
        f.validate(inputs) ++ d.validate(inputs)
      }

    def ||(d: Condition): Condition =
      FromContext[Boolean] { p ⇒
        import p._
        f.from(context) || d.from(context)
      } validate { p ⇒
        import p._
        f.validate(inputs) ++ d.validate(inputs)
      }
  }

  implicit class ExpandedStringOperations(s1: FromContext[String]) {
    def +(s2: FromContext[String]) = (s1 map2 s2)(_ + _)
  }

  implicit class FromContextFileDecorator(f: FromContext[File]) {
    def exists = f.map(_.exists)
    def isEmpty = f.map(_.isEmpty)
    def /(path: FromContext[String]) = (f map2 path)(_ / _)
  }

}

class FromContext[+T](c: FromContext.Parameters ⇒ T, v: FromContext.ValidationParameters ⇒ Seq[Throwable]) {
  def apply(context: ⇒ Context)(implicit rng: RandomProvider, newFile: NewFile, fileService: FileService): T = c(FromContext.Parameters(context, rng, newFile, fileService))
  def from(context: ⇒ Context)(implicit rng: RandomProvider, newFile: NewFile, fileService: FileService): T = apply(context)
  def validate(inputs: Seq[Val[_]])(implicit newFile: NewFile, fileService: FileService): Seq[Throwable] = v(FromContext.ValidationParameters(inputs, newFile, fileService))

  def validate(v2: FromContext.ValidationParameters ⇒ Seq[Throwable]) = {
    def nv(p: FromContext.ValidationParameters) = v(p) ++ v2(p)
    new FromContext(c, v = nv)
  }
}

object Expandable {
  def apply[S, T](f: S ⇒ FromContext[T]) = new Expandable[S, T] {
    override def expand(s: S): FromContext[T] = f(s)
  }

  implicit def stringToString = Expandable[String, String](s ⇒ s: FromContext[String])
  implicit def stringToFile = Expandable[String, File](s ⇒ s: FromContext[File])
  implicit def fileToFile = Expandable[File, File](f ⇒ f: FromContext[File])
}

trait Expandable[S, T] {
  def expand(s: S): FromContext[T]
}

