/*
 * Copyright (C) 2017 Romain Reuillon
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

package org.openmole.core.workflow.sampling

import cats.implicits.*
import org.openmole.core.context.*
import org.openmole.core.fileservice.*
import org.openmole.core.argument.*
import org.openmole.tool.math.*
import org.openmole.core.workflow.domain.*
import org.openmole.core.workflow.sampling.*
import org.openmole.core.workspace.*
import org.openmole.tool.random.*

import scala.annotation.tailrec
import scala.reflect.ClassTag

object ScalableValue:

  object Scaled:
    def toVariable(s: Scaled, values: List[Double], inputSize: Int) =
      s match
        case s: ScaledScalar[_]   => Variable(s.prototype, s.v) -> values.tail
        case s: ScaledSequence[_] => Variable(s.prototype, s.s) -> values.drop(inputSize)
  
  enum Scaled:
    case ScaledSequence[T](prototype: Val[Array[T]], s: Array[T])
    case ScaledScalar[T](prototype: Val[T], v: T) 

  object ScalableType :
    given ScalableType[Double] = new ScalableType[Double]:
      def scale(v: Double, min: Double, max: Double) = v.scale(min, max)
      def convert(v: Double) = v

    given ScalableType[Int] = new ScalableType[Int]:
      def scale(v: Double, min: Int, max: Int) = v.scale(min.toDouble, max.toDouble + 1).toInt
      def convert(v: Double) = v.toInt


    given ScalableType[Long] = new ScalableType[Long]:
      def scale(v: Double, min: Long, max: Long) = v.scale(min.toDouble, max.toDouble + 1).toLong
      def convert(v: Double) = v.toLong



  trait ScalableType[T]:
    def scale(v: Double, min: T, max: T): T
    def convert(v: Double): T


  implicit def fromFactor[D, T: ScalableType](s: Factor[D,T])(implicit bounded: BoundedFromContextDomain[D, T]): ScalableValue =
    def unflatten(values: Seq[Double], scale: Boolean): FromContext[Scaled] = FromContext { p =>
      import p.*

      val g = values.head
      assert(!g.isNaN)

      val (min, max) = bounded(s.domain).domain

      val sc = if (scale) implicitly[ScalableType[T]].scale(g, min.from(context), max.from(context)) else implicitly[ScalableType[T]].convert(g)
      Scaled.ScaledScalar(s.value, sc)
    }

    ScalarValue(bounded(s.domain).inputs, s.value, unflatten)

  
  implicit def fromFactorOfSeq[D, T: ScalableType: ClassTag](t: Factor[D, Array[T]])(implicit bounded: BoundedFromContextDomain[D, Array[T]]): ScalableValue = 
    def size: FromContext[Int] = FromContext { p =>
      import p.*
      val (min, max) = bounded(t.domain).domain
      math.min(min.from(context).size, max.from(context).size)
    }

    def unflatten(values: Seq[Double], scale: Boolean): FromContext[Scaled] = FromContext { p =>
      import p.*

      val (min, max) = bounded(t.domain).domain
      def scaled =
        if (scale) (values zip (min.from(context) zip max.from(context))).map { case (g, (min, max)) => implicitly[ScalableType[T]].scale(g, min, max) }
        else values.map(implicitly[ScalableType[T]].convert)

      Scaled.ScaledSequence(t.value, scaled.toArray)
    }
  
    SeqValue(bounded(t.domain).inputs, t.value, unflatten, size)


  def prototypes(scales: Seq[ScalableValue]) = scales.map(_.prototype)

  def toVariables(scales: Seq[ScalableValue], values: Seq[Double], scale: Boolean = true): FromContext[List[Variable[?]]] = 
    @tailrec def scaled0(scales: List[ScalableValue], values: List[Double], acc: List[Variable[?]] = Nil)(context: => Context, rng: RandomProvider, newFile: TmpDirectory, fileService: FileService): List[Variable[?]] =
      if (scales.isEmpty || values.isEmpty) acc.reverse
      else {
        val input = scales.head
        val (variable, tail) =
          unflatten(input)(values, scale).map { Scaled.toVariable(_, values, input.size(context)(rng, newFile, fileService)) }.from(context)(rng, newFile, fileService)

        scaled0(scales.tail, tail, variable :: acc)({ context + variable }, rng, newFile, fileService)
      }

    FromContext { p => scaled0(scales.toList, values.toList)(p.context, p.random, p.tmpDirectory, p.fileService) }


  def unflatten(s: ScalableValue) = 
    s match 
      case s: ScalarValue => s.unflatten
      case s: SeqValue => s.unflatten

  extension (s: ScalableValue)
    def inputs = 
      s match 
        case s: ScalarValue => s.inputs
        case s: SeqValue => s.inputs
      
    def prototype = 
      s match 
        case s: ScalarValue => s.prototype
        case s: SeqValue => s.prototype

    def size: FromContext[Int] = 
      s match 
        case s: ScalarValue => 1
        case s: SeqValue => s.size    

    def isScalar = 
      s match 
        case s: ScalarValue => true
        case s: SeqValue => false

  def isContinuous(s: ScalableValue) =
    def continuousType(t: Val[?]) =
      t match
        case Val.caseDouble(_) => true
        case _ => false

    s match
      case s: ScalarValue => continuousType(s.prototype)
      case s: SeqValue => continuousType(Val.fromArray(s.prototype))

  type Unflatten = (Seq[Double], Boolean) => FromContext[Scaled]


enum ScalableValue:
  case ScalarValue(inputs: PrototypeSet, prototype: Val[?], unflatten: ScalableValue.Unflatten)
  case SeqValue(inputs: PrototypeSet, prototype: Val[?], unflatten: ScalableValue.Unflatten, size: FromContext[Int])
