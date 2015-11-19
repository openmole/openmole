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

package org.openmole.plugin.method.evolution

import fr.iscpif.mgo._
import fr.iscpif.mgo.algorithm._
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.tools.TextClosure
import org.openmole.tool.statistics._

case class Replication(
  max: Int = 100,
  reevaluate: Double = 0.2,
  aggregation: Option[Seq[TextClosure[Seq[Double], Double]]] = None)

trait GAAlgorithm extends Algorithm with GeneticAlgorithm with InputsConverter {
  def toPhenotype(s: Seq[Double]): P
  def populationType: PrototypeType[Pop]
  def individualType: PrototypeType[Ind]
  def objectives: Objectives
  def inputsPrototypes = inputs.inputs.map(_.prototype)
  def outputPrototypes: Seq[Prototype[_]] = objectives
  def toVariables(genome: G, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]] = scaled(genomeValues.get(genome), context)
  def toVariables(population: Pop, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]]
}

trait DeterministicGAAlgorithm extends GAAlgorithm {

  final type P = Seq[Double]

  def toPhenotype(s: Seq[Double]) = s

  def toVariables(population: Pop, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]] = {
    val scaledValues = population.map(i ⇒ scaled(genomeValues.get(i.genome), context))

    inputs.inputs.zipWithIndex.map {
      case (input, i) ⇒
        input match {
          case Scalar(prototype, _, _)   ⇒ Variable(prototype.toArray, scaledValues.map(_(i).value.asInstanceOf[Double]).toArray[Double])
          case Sequence(prototype, _, _) ⇒ Variable(prototype.toArray, scaledValues.map(_(i).value.asInstanceOf[Array[Double]]).toArray[Array[Double]])
        }
    }.toList ++
      objectives.zipWithIndex.map {
        case (p, i) ⇒
          Variable(
            p.toArray,
            population.map(ind ⇒ ind.phenotype(i)).toArray)
      }
  }
}

trait StochasticGAAlgorithm extends GAAlgorithm {

  type P = History[Seq[Double]]

  def replications = Prototype[Int]("replications")
  override def outputPrototypes = Seq(replications) ++ objectives

  def toPhenotype(s: Seq[Double]) = History(List(s))

  def aggregation: Option[Seq[TextClosure[Seq[Double], Double]]]

  def aggregate(individual: Ind): Seq[Double] =
    aggregation match {
      case Some(aggs) ⇒ (individual.phenotype.history zip aggs).map { case (p, a) ⇒ a(p) }
      case None       ⇒ individual.phenotype.history.transpose.map(_.median)
    }

  def toVariables(population: Pop, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]] = {
    val scaledValues = population.map(i ⇒ scaled(genomeValues.get(i.genome), context))

    inputs.inputs.zipWithIndex.map {
      case (input, i) ⇒
        input match {
          case Scalar(prototype, _, _)   ⇒ Variable(prototype.toArray, scaledValues.map(_(i).value.asInstanceOf[Double]).toArray[Double])
          case Sequence(prototype, _, _) ⇒ Variable(prototype.toArray, scaledValues.map(_(i).value.asInstanceOf[Array[Double]]).toArray[Array[Double]])
        }
    }.toList ++
      objectives.zipWithIndex.map {
        case (p, i) ⇒
          Variable(
            p.toArray,
            population.map(ind ⇒ aggregate(ind)(i)).toArray)
      } ++ Seq(Variable(replications.toArray, population.map(_.phenotype.history.size).toArray))
  }
}