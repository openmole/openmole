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
import org.openmole.core.workspace.TmpDirectory

import scala.annotation.tailrec

trait LowPriorityToFromContext {
  implicit def fromTToContext[T]: ToFromContext[T, T] = ToFromContext[T, T](t ⇒ FromContext.value[T](t))
}

object ToFromContext extends LowPriorityToFromContext {
  import FromContext._

  def apply[F, T](func: F ⇒ FromContext[T]): ToFromContext[F, T] = new ToFromContext[F, T] {
    def apply(f: F) = func(f)
  }

  //implicit def functionToFromContext[T]: ToFromContext[T, T] = ToFromContext[T, T](f ⇒ FromContext { p ⇒ import p._; f(context) })

  implicit def codeToFromContextFloat: ToFromContext[String, Float] = ToFromContext(codeToFromContext[Float])
  implicit def codeToFromContextDouble: ToFromContext[String, Double] = ToFromContext(codeToFromContext[Double])
  implicit def codeToFromContextLong: ToFromContext[String, Long] = ToFromContext(codeToFromContext[Long])
  implicit def codeToFromContextInt: ToFromContext[String, Int] = ToFromContext(codeToFromContext[Int])
  implicit def codeToFromContextBigDecimal: ToFromContext[String, BigDecimal] = ToFromContext(codeToFromContext[BigDecimal])
  implicit def codeToFromContextBigInt: ToFromContext[String, BigInt] = ToFromContext(codeToFromContext[BigInt])
  implicit def codeToFromContextBoolean: ToFromContext[String, Boolean] = ToFromContext(codeToFromContext[Boolean])

  implicit def fileToString: ToFromContext[File, String] = ToFromContext[File, String](f ⇒ ExpandedString(f.getPath))
  implicit def stringToString: ToFromContext[String, String] = ToFromContext[String, String](s ⇒ ExpandedString(s))
  implicit def stringToFile: ToFromContext[String, File] = ToFromContext[String, File](s ⇒ ExpandedString(s).map(s ⇒ File(s)))
  implicit def fileToFile: ToFromContext[File, File] = ToFromContext[File, File](f ⇒ ExpandedString(f.getPath).map(s ⇒ File(s)))
  implicit def intToLong: ToFromContext[Int, Long] = ToFromContext[Int, Long](i ⇒ FromContext.value(i.toLong))
  implicit def intToInt: ToFromContext[Int, Int] = ToFromContext[Int, Int](i ⇒ FromContext.value(i))
  
  implicit def fromContextToFromContext[T]: ToFromContext[FromContext[T], T] = ToFromContext[FromContext[T], T](identity)

  implicit def booleanToCondition: ToFromContext[Boolean, Boolean] = ToFromContext[Boolean, Boolean](b ⇒ FromContext.value(b))
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
  implicit def fromTToContext[F, T](t: F)(implicit tfc: ToFromContext[F, T]): FromContext[T] = contextConverter[F, T](t)(tfc)
}

object FromContext extends LowPriorityFromContext {

  /**
   * A [[FromContext]] can be seen as a monad
   */
  object asMonad {
    implicit val monad: Monad[FromContext] = new Monad[FromContext] {
      def tailRecM[A, B](a: A)(f: A ⇒ FromContext[Either[A, B]]): FromContext[B] = {

        @tailrec def computeB(a: A, context: Context)(implicit rng: RandomProvider, newFile: TmpDirectory, fileService: FileService): B = {
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
      } withValidate { fa.validate ++ ff.validate }
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
      proxy()().from(context)
    } withValidate proxy().validate
  }

  //implicit def functionToFromContext[T](f: (Context ⇒ T)): FromContext[T] = contextConverter(f)

  implicit def codeToFromContextFloat(s: String): FromContext[Float] = contextConverter[String, Float](s)
  implicit def codeToFromContextDouble(s: String): FromContext[Double] = contextConverter[String, Double](s)
  implicit def codeToFromContextLong(s: String): FromContext[Long] = contextConverter[String, Long](s)
  implicit def codeToFromContextInt(s: String): FromContext[Int] = contextConverter[String, Int](s)
  implicit def codeToFromContextBigDecimal(s: String): FromContext[BigDecimal] = contextConverter[String, BigDecimal](s)
  implicit def codeToFromContextBigInt(s: String): FromContext[BigInt] = contextConverter[String, BigInt](s)
  implicit def codeToFromContextBoolean(s: String): FromContext[Boolean] = contextConverter[String, Boolean](s)

  implicit def fileToString(f: File): FromContext[String] = contextConverter[File, String](f)
  //implicit def stringToString(s: String): StringFromContext[String] = StringFromContext.fromString(s)
  implicit def stringToFile(s: String): FromContext[File] = contextConverter[String, File](s)
  implicit def fileToFile(f: File): FromContext[File] = contextConverter[File, File](f)

  implicit def booleanToCondition(b: Boolean): FromContext[Boolean] = contextConverter[Boolean, Boolean](b)
  implicit def prototypeIsFromContext[T](p: Val[T]): FromContext[T] = contextConverter[Val[T], T](p)

  /**
   * FromContext for a given prototype
   * @param p
   * @tparam T
   * @return
   */
  def prototype[T](v: Val[T]) =
    FromContext[T] { param ⇒
      import param._
      context(v)
    } withValidate {
      Validate { p ⇒
        import p._
        inputs.find(_.name == v.name) match {
          case Some(i) if v == i ⇒ Seq()
          case None              ⇒ Seq(new UserBadDataError(s"FromContext validation failed, $v not found among inputs $inputs"))
          case Some(i)           ⇒ Seq(new UserBadDataError(s"FromContext validation failed, $v has incorrect type, should be $i, among inputs $inputs"))
        }
      }
    } withInputs (Seq(v))

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
  case class Parameters(val context: Context)(implicit val random: RandomProvider, val newFile: TmpDirectory, val fileService: FileService)
  //case class ValidationParameters(inputs: Seq[Val[_]], implicit val tmpDirectory: TmpDirectory, implicit val fileService: FileService)

  /**
   * Construct a FromContext from a function of [[Parameters]]
   * @param f
   * @tparam T
   * @return
   */
  def apply[T](f: Parameters ⇒ T): FromContext[T] = new FromContext[T](f, Validate.success, Seq.empty)

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
      } withValidate { f.validate ++ d.validate }

    def ||(d: Condition): Condition =
      FromContext[Boolean] { p ⇒
        import p._
        f.from(context) || d.from(context)
      } withValidate { f.validate ++ d.validate }
  }

  implicit class ExpandedStringOperations(s1: FromContext[String]) {
    def +(s2: FromContext[String]) = (s1 map2 s2)(_ + _)
  }

  implicit class FromContextFileDecorator(f: FromContext[File]) {
    def exists = f.map(_.exists)
    def isEmpty = f.map(_.isEmpty)
    def /(path: FromContext[String]) = (f map2 path)(_ / _)
  }

  def copy[T](f: FromContext[T])(validate: Validate = f.v, inputs: Seq[Val[_]] = f.inputs) =
    new FromContext(c = f.c, v = validate, inputs = inputs)

}

class FromContext[+T](val c: FromContext.Parameters ⇒ T, val v: Validate, val inputs: Seq[Val[_]]) {
  def apply(context: ⇒ Context)(implicit rng: RandomProvider, tmpDirectory: TmpDirectory, fileService: FileService): T = 
    val parameters: FromContext.Parameters = FromContext.Parameters(context)(rng, tmpDirectory, fileService)
    val result = c(parameters)
    result

  def from(context: ⇒ Context)(implicit rng: RandomProvider, tmpDirectory: TmpDirectory, fileService: FileService): T = apply(context)

  def validate = v

  def withValidate(validate: Validate): FromContext[T] = FromContext.copy(this)(validate = v ++ validate)
  def withInputs(v: Seq[Val[_]]): FromContext[T] = FromContext.copy(this)(inputs = inputs ++ v)
  def using(fs: FromContext[_]*): FromContext[T] =
    this.withValidate(fs.map(_.validate)).withInputs(fs.flatMap(_.inputs))
}

object StringFromContext {

  implicit def fromString(s: String): StringFromContext[String] = {
    val fc = FromContext.contextConverter[String, String](s)

    new StringFromContext(
      c = fc.c,
      v = fc.v,
      string = s
    )
  }

}

class StringFromContext[+T](c: FromContext.Parameters ⇒ T, v: Validate, val string: String) extends FromContext(c, v, Seq()) {
  override def toString = s"FromContext($string)"
}

object Expandable {
  def apply[S, T](f: S ⇒ FromContext[T]) = new Expandable[S, T] {
    override def expand(s: S): FromContext[T] = f(s)
  }

  implicit def stringToString: Expandable[String, String] = Expandable[String, String](s ⇒ s: FromContext[String])
  implicit def stringToFile: Expandable[String, File] = Expandable[String, File](s ⇒ s: FromContext[File])
  implicit def fileToFile: Expandable[File, File] = Expandable[File, File](f ⇒ f: FromContext[File])
}

trait Expandable[S, T] {
  def expand(s: S): FromContext[T]
}

