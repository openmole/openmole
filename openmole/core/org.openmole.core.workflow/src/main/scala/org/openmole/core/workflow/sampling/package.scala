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

package org.openmole.core.workflow.sampling


/**
 * Sampling aims at associating prototypes with values.
 */

import org.openmole.core.context._
import org.openmole.core.argument._
import org.openmole.tool.types._
import org.openmole.core.workflow.domain._
import cats.implicits._
import org.openmole.tool.logger.Prettifier


object IsSampling:
  given factorIsSampling[D, T](using domain: DiscreteFromContextDomain[D, T]): IsSampling[Factor[D, T]] = f =>
    def inputs =
      val domainValue = domain(f.domain)
      domain(f.domain).inputs ++ domainValue.inputs

    def outputs = List(f.value)

    def validate: Validate =
      val domainValue = domain(f.domain)
      domainValue.domain.validate ++ domainValue.validation

    Sampling(
      domain(f.domain).domain.map { values => values.map { v => List(Variable(f.value, v)) } },
      outputs,
      inputs,
      validate,
    )

trait IsSampling[-S]:
  def apply(s: S): Sampling


trait SamplingPackage:

  implicit class PrototypeFactorDecorator[T](p: Val[T]):
    def is(d: FromContext[T]) = Factor(p, d)

  type Sampling = org.openmole.core.workflow.sampling.Sampling

  object EmptySampling:
    given IsSampling[EmptySampling] = s =>
      Sampling(
        FromContext.value(Iterator.empty),
        Seq(),
        Seq(),
        Validate.success
      )

  case class EmptySampling()



import org.openmole.core.context._
import org.openmole.core.keyword._

/**
 * The factor type associates a Val to a domain through the keyword In
 * @tparam D
 * @tparam T
 */
type Factor[D, T] = In[Val[T], D]

/**
 * Construct a [[Factor]] from a prototype and its domain
 * @param p
 * @param d
 * @return
 */
def Factor[D, T](p: Val[T], d: D) = In(p, d)

//given [F, D, T](using isFactor: IsFactor[F, D, T]): Conversion[F, Factor[D, T]] = t => isFactor(t)
//
//object IsFactor:
//
//  def apply[F, D, T](f: F => Factor[D, T]) =
//    new IsFactor[F, D, T]:
//      def apply(x: F) = f(x)
//
//  given [D, T]: IsFactor[Factor[D, T], D, T] = IsFactor(identity)
//
//  given equalIsFactor[D, T]: IsFactor[:=[Val[T], D], D, T] = IsFactor(f => Factor(f.value, f.equal))
//
//  given IsFactor[Val[Boolean], Seq[Boolean], Boolean] =
//    IsFactor(v => v in Seq(true, false))
//
//  given seqIsFactor[F, D, T](using isf: IsFactor[F, D, T]): IsFactor[Seq[F], Seq[D], T] =
//    IsFactor(v => v.map(isf.apply))
//
//  given weightFactor[F, D, T, B](using isf: IsFactor[F, D, T]): IsFactor[Weight[F, B], D, T] =
//    IsFactor(w => isf(w.value))
//
//  given byFactor[F, D, T, B](using isf: IsFactor[F, D, T]): IsFactor[By[F, B], D, T] =
//    IsFactor(w => isf(w.value))
//
//trait IsFactor[F, D, T]:
//  def apply(f: F): Factor[D, T]

