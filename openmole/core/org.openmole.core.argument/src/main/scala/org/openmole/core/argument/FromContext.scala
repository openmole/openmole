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

package org.openmole.core.argument

import org.openmole.core.context.*
import org.openmole.core.exception.*
import org.openmole.tool.cache.*
import org.openmole.tool.random.*
import org.openmole.tool.file.*

import org.openmole.core.fileservice.FileService
import org.openmole.core.workspace.TmpDirectory

import scala.annotation.tailrec

import cats.*
import cats.implicits.*

trait ToFromContextLowPriorityGiven:
  given ToFromContext[String, Float] = FromContext.codeToFromContext[Float]
  given ToFromContext[String, Double] = FromContext.codeToFromContext[Double]
  given ToFromContext[String, Long] = FromContext.codeToFromContext[Long]
  given ToFromContext[String, Int] = FromContext.codeToFromContext[Int]
  given ToFromContext[String, BigDecimal] = FromContext.codeToFromContext[BigDecimal]
  given ToFromContext[String, BigInt] = FromContext.codeToFromContext[BigInt]
  given ToFromContext[String, Boolean] = FromContext.codeToFromContext[Boolean]

  given ToFromContext[File, String] = f ⇒ ExpandedString(f.getPath) copy (stringValue = Some(s"file:${f.getPath}"))
  given ToFromContext[String, File] = s ⇒ ExpandedString(s).map(s ⇒ File(s)) copy (stringValue = Some(s"file:$s"))
  given ToFromContext[Int, Long] = i ⇒ FromContext.value(i.toLong) copy (stringValue = Some(i.toString))
  //given ToFromContext[Int, Int] = i ⇒ FromContext.value(i) copy(stringValue = Some(i.toString))

  given ToFromContext[Int, Double] = i ⇒
    val v = i.toDouble
    FromContext.value(v) copy (stringValue = Some(v.toString))

  given ToFromContext[Long, Double] = i ⇒
    val v = i.toDouble
    FromContext.value(v) copy (stringValue = Some(v.toString))

  given[T]: ToFromContext[FromContext[T], T] = identity

  //given ToFromContext[Boolean, Boolean] = b ⇒ FromContext.value(b).copy(stringValue = Some(b.toString))
  given[T]: ToFromContext[Val[T], T] = p ⇒ FromContext.prototype(p)

  given[T]: ToFromContext[Val[T], String] = p =>
    FromContext: c =>
      import c.*
      context(p).toString

object ToFromContext extends ToFromContextLowPriorityGiven:
  given ToFromContext[File, File] = f ⇒ ExpandedString(f.getPath).map(s ⇒ File(s)) copy (stringValue = Some(s"file:${f.getPath}"))
  given ToFromContext[String, String] = s => ExpandedString(s)
  given [T]: ToFromContext[T, T] = t ⇒ FromContext.value[T](t)

@FunctionalInterface trait ToFromContext[F, T]: 
  def convert(f: F): FromContext[T]

trait LowPriorityFromContext:
  def contextConverter[F, T](f: F)(implicit tfc: ToFromContext[F, T]): FromContext[T] = tfc.convert(f)

  /**
   * implicit converter of a value to a FromContext (using implicit [[LowPriorityToFromContext]])
   * @param t
   * @tparam T
   * @return
   */
  implicit def fromTToContext[F, T](t: F)(implicit tfc: ToFromContext[F, T]): FromContext[T] = contextConverter[F, T](t)(tfc)

object FromContext extends LowPriorityFromContext:

  given Applicative[FromContext] with
    override def pure[A](x: A): FromContext[A] = FromContext.value(x)
    override def ap[A, B](ff: FromContext[(A) ⇒ B])(fa: FromContext[A]): FromContext[B] =
      FromContext[B] { p ⇒
        import p._
        val res = fa.from(context)
        ff.from(context).apply(res)
      } copy(v = fa.validate ++ ff.validate, inputs = fa.inputs ++ ff.inputs)

    /*object asMonad:
      given Monad[FromContext] with
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
      override def pure[A](x: A): FromContext[A] = FromContext.value(x)*/
  
  /**
   * Convert scala code to a FromContext (code is compiled by [[ScalaCompilation]])
   *
   * @param code
   * @tparam T
   * @return
   */
  def codeToFromContext[T: Manifest](code: String) =
    val proxy = Cache(ScalaCompilation.dynamic[T](code))

    FromContext[T] { p ⇒
      import p._
      proxy()().from(context)
    } copy(stringValue = Some(code)) withValidate(proxy().validate)

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
    } withInputs (Seq(v)) copy(stringValue = Some(s"val:$v"))

  /**
   * From context for a given value
   * @param t
   * @tparam T
   * @return
   */
  def value[T](t: T): FromContext[T] =
    val stringValue =
      t match
        case t: File => s"file:${t.getPath}"
        case t => t.toString
    FromContext[T] { _ ⇒ t } copy(stringValue = Some(stringValue))

  def fromString(s: String): FromContext[String] = s

  /**
   * Parameters wrap a Context and implicit services
   * @param context
   * @param random
   * @param tmpDirectory
   * @param fileService
   */
  case class Parameters(context: Context)(implicit val random: RandomProvider, val tmpDirectory: TmpDirectory, val fileService: FileService)
  //case class ValidationParameters(inputs: Seq[Val[?]], implicit val tmpDirectory: TmpDirectory, implicit val fileService: FileService)

  /**
   * Construct a FromContext from a function of [[Parameters]]
   * @param f
   * @tparam T
   * @return
   */
  def apply[T](f: Parameters ⇒ T): FromContext[T] = new FromContext[T](f, Validate.success, Seq.empty, DefaultSet.empty, None)


  /**
   * Operators for boolean FromContext ([[Condition]] ~ FromContext[Boolean])
   * @param f
   */
  extension (f: Condition)
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


  extension (s1: FromContext[String])
    def +(s2: FromContext[String]) = (s1 map2 s2)(_ + _)

  extension (f: FromContext[File])
    def exists = f.map(_.exists)
    def isEmpty = f.map(_.isEmpty)
    def /(path: FromContext[String]) = (f map2 path)(_ / _)

  extension[A](fr: FromContext[A]) 
    def map[S](f: A => S) = fr.map(f)
    def map2[B, C](fb: FromContext[B])(f: (A, B) => C) = fr.map2(fb)(f)

case class FromContext[+T](c: FromContext.Parameters ⇒ T, v: Validate, inputs: Seq[Val[?]], defaults: DefaultSet, stringValue: Option[String]):
  def apply(context: ⇒ Context)(implicit rng: RandomProvider, tmpDirectory: TmpDirectory, fileService: FileService): T =
    def fullContext = DefaultSet.completeContext(defaults, context)
    val parameters: FromContext.Parameters = FromContext.Parameters(fullContext)(rng, tmpDirectory, fileService)
    val result = c(parameters)
    result

  def from(context: ⇒ Context)(implicit rng: RandomProvider, tmpDirectory: TmpDirectory, fileService: FileService): T = apply(context)

  def validate = Validate.withExtraInputs(v, i => DefaultSet.defaultVals(i, defaults))

  def withValidate(validate: Validate): FromContext[T] = copy(v = v ++ validate)
  def withInputs(v: Seq[Val[?]]): FromContext[T] = copy(inputs = inputs ++ v)

  def using(fs: FromContext[_]*): FromContext[T] =
    this.withValidate(fs.map(_.validate)).withInputs(fs.flatMap(_.inputs))


  override def toString =
    stringValue match
      case Some(string) => s"FromContext($string)"
      case _ => super.toString

object Expandable:
  def apply[S, T](f: S ⇒ FromContext[T]) =
    new Expandable[S, T]:
      override def expand(s: S): FromContext[T] = f(s)


  given Expandable[String, String] = Expandable[String, String](s ⇒ s: FromContext[String])
  given Expandable[String, File] = Expandable[String, File](s ⇒ s: FromContext[File])
  given Expandable[File, File] = Expandable[File, File](f ⇒ f: FromContext[File])

trait Expandable[S, T]:
  def expand(s: S): FromContext[T]

