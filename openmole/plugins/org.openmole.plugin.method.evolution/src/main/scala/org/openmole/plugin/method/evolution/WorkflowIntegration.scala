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
import scalaz._
import scala.util.Random

case class Replication(
  max: Int = 100,
  reevaluate: Double = 0.2,
  aggregation: Option[Seq[TextClosure[Seq[Double], Double]]] = None)

object WorkflowIntegration {

  implicit val gaWorkflowIntegration: WorkflowIntegration[GAAlgorithm] = new WorkflowIntegration[GAAlgorithm] {
    override def resultPrototypes(algorithm: GAAlgorithm): Seq[Prototype[_]] = algorithm.resultPrototypes
    override def inputsPrototypes(algorithm: GAAlgorithm): Seq[Prototype[_]] = algorithm.inputsPrototypes
    override def outputPrototypes(algorithm: GAAlgorithm): Seq[Prototype[_]] = algorithm.outputPrototypes
    override def variablesToPhenotype(algorithm: GAAlgorithm)(context: Context): algorithm.P = algorithm.toPhenotype(context)
    override def populationToVariables(algorithm: GAAlgorithm)(population: algorithm.Pop, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]] = algorithm.populationToVariables(population, context)
    override def genomeToVariables(algorithm: GAAlgorithm)(genome: algorithm.GAGenome, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]] = algorithm.genomeToVariables(genome, context)
    override def populationType(algorithm: GAAlgorithm): PrototypeType[algorithm.Pop] = algorithm.populationType
    override def individualType(algorithm: GAAlgorithm): PrototypeType[algorithm.Ind] = algorithm.individualType
    override def genomeType(algorithm: GAAlgorithm): PrototypeType[algorithm.G] = algorithm.genomeType
    override def randomGenome(algorithm: GAAlgorithm): State[Random, algorithm.G] = algorithm.randomGenome(algorithm.genome.size)
  }

}

trait WorkflowIntegration[-T <: Algorithm] {
  def resultPrototypes(algorithm: T): Seq[Prototype[_]]
  def inputsPrototypes(algorithm: T): Seq[Prototype[_]]
  def outputPrototypes(algorithm: T): Seq[Prototype[_]]
  def genomeToVariables(algorithm: T)(genome: algorithm.G, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]]
  def populationToVariables(algorithm: T)(population: algorithm.Pop, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]]
  def variablesToPhenotype(algorithm: T)(context: Context): algorithm.P
  def populationType(algorithm: T): PrototypeType[algorithm.Pop]
  def individualType(algorithm: T): PrototypeType[algorithm.Ind]
  def genomeType(algorithm: T): PrototypeType[algorithm.G]
  def randomGenome(algorithm: T): State[Random, algorithm.G]
}


trait GAAlgorithm extends Algorithm with GeneticAlgorithm { ga =>
  def toPhenotype(context: Context): P = {
    val scaled: Seq[(Prototype[Double], Double)] = objectives.map(o ⇒ o -> context(o))
    val phenotype: Seq[Double] = scaled.map(_._2)
    toPhenotype(phenotype)
  }

  def genome: Genome

  def scaled(genome: Seq[Double], context: Context)(implicit rng: RandomProvider): List[Variable[_]] =
    InputConverter.scaled(ga.genome.inputs.toList, genome.toList, context)

  def resultPrototypes: Seq[Prototype[_]]
  def genomeType = PrototypeType[G]
  def toPhenotype(s: Seq[Double]): P
  def populationType: PrototypeType[Pop]
  def individualType: PrototypeType[Ind]
  def objectives: Objectives
  def inputsPrototypes = genome.inputs.map(_.prototype)
  def outputPrototypes: Seq[Prototype[_]] = objectives
  def genomeToVariables(genome: G, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]] = scaled(genomeValues.get(genome), context)
  def populationToVariables(population: Pop, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]]
}

trait DeterministicGAAlgorithm extends GAAlgorithm {

  final type P = Seq[Double]

  def toPhenotype(s: Seq[Double]) = s

  override def resultPrototypes = (inputsPrototypes ++ outputPrototypes).distinct

  def populationToVariables(population: Pop, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]] = {
    val scaledValues = population.map(i ⇒ scaled(genomeValues.get(i.genome), context))

    genome.inputs.zipWithIndex.map {
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
  override def resultPrototypes = (inputsPrototypes ++ outputPrototypes ++ Seq(replications)).distinct

  def toPhenotype(s: Seq[Double]) = History(List(s))

  def aggregation: Option[Seq[TextClosure[Seq[Double], Double]]]

  def aggregate(individual: Ind): Seq[Double] =
    aggregation match {
      case Some(aggs) ⇒ (individual.phenotype.history zip aggs).map { case (p, a) ⇒ a(p) }
      case None       ⇒ individual.phenotype.history.transpose.map(_.median)
    }

  def populationToVariables(population: Pop, context: Context)(implicit rng: RandomProvider): Seq[Variable[_]] = {
    val scaledValues = population.map(i ⇒ scaled(genomeValues.get(i.genome), context))

    genome.inputs.zipWithIndex.map {
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