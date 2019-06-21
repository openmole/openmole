/*
 * Copyright (C) 2018 Samuel Thiriot
 *                    Romain Reuillon
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

package org.openmole.plugin.method.sensitivity

import org.openmole.core.context.{ Namespace, Val, Variable }
import org.openmole.core.expansion._
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.core.dsl._
import org.openmole.core.workflow.task.FromContextTask
import org.openmole.core.workflow.tools.ScalarOrSequenceOfDouble

/**
 * Describes a part of the space of inputs/ouputs of a model (or actually, puzzle)
 * on which to assess the sensitivity.
 * Describes that we wish to analyze the impact of variations of a given input on a given output,
 * and return it into variables indicatorMu, indicatorMu* and indicatorSigma.
 */
case class SubspaceToAnalyze(
  input:  Val[Double],
  output: Val[Double]
)

object MorrisAggregation {

  /**
   * Variables produced by Morris analysis
   */
  val namespace = Namespace("morris")

  /**
   * Takes lists for trajectories:
   * factor:   [,    a,    b,  c,  ,   c,  a,  b]
   * deltas:   [0.,  0.2,  -1, 0.1,0,  -1, 0.1, 2]
   * so for each result, we are able to now which factor was changed compared to the previous one, and of how much
   *
   *
   * Also receives the list of all the ouputs to analyze:
   * List(
   *   o1:     [1,   2,    1,  0,   4, 1,  2,  3]
   *   o2:     [2,   1,    1,  0,  1,  3,  1,  2]
   *   ...
   * )
   *
   *
   *
   */

  def sumAbs(res: Double, b: Double): Double = res + Math.abs(b)

  def squaredDiff(ee: Double, mu: Double): Double = Math.pow(ee - mu, 2)

  /**
   * For a given model input named inputName,
   * analyze the output values outputValues
   * based on metadata passed by sampling which describes for each line of input
   * which factor was changed and of how much delta
   */
  def elementaryEffect(
    input:         Val[_],
    output:        Val[_],
    outputValues:  Array[Double],
    factorChanged: Array[String],
    deltas:        Array[Double]): (Double, Double, Double) = {

    // indices of results in which the input had been changed
    val indicesWithEffect: Array[Int] = factorChanged.zipWithIndex
      .collect { case (name, idx) if (input.name == name) ⇒ idx }
    val r: Int = indicesWithEffect.length

    MorrisSampling.Log.logger.fine("measuring the elementary effects of " + input + " on " + output + " based on " + r + " repetitions")
    indicesWithEffect.foreach(idxChanged ⇒ MorrisSampling.Log.logger.fine("For a delta: " + deltas(idxChanged) + ", value changed from " + outputValues(idxChanged - 1) + " to " + outputValues(idxChanged) + " => " + (outputValues(idxChanged) - outputValues(idxChanged - 1)) / (deltas(idxChanged)) + ")"))

    // ... so we know each index - 1 leads to the case before changing the factor
    // elementary effects
    val elementaryEffects: Array[Double] = indicesWithEffect.map(idxChanged ⇒ (outputValues(idxChanged) - outputValues(idxChanged - 1)) / (deltas(idxChanged)))
    // leading to indicators
    val rD: Double = r.toDouble
    val mu: Double = elementaryEffects.sum / rD
    val muStar: Double = elementaryEffects.reduceLeft(sumAbs) / rD
    val sigma: Double = Math.sqrt(elementaryEffects.map(ee ⇒ squaredDiff(ee, mu)).sum / rD)

    MorrisSampling.Log.logger.fine("=> aggregate impact of " + input + " on " + output + ": mu=" + mu + ", mu*=" + muStar + ", sigma=" + sigma)

    (mu, muStar, sigma)

  }

  /**
   * Takes from the user the list of subspaces to analyze,
   * that is tuples of (model input, model output) for which
   * indicators (mu, mu* and sigma) should be computed.
   * Receives at execution time the lists of
   */

  def apply[T](
    modelInputs:  Seq[ScalarOrSequenceOfDouble[_]],
    modelOutputs: Seq[Val[Double]])(implicit name: sourcecode.Name, definitionScope: DefinitionScope) = {

    def morrisOutputs(
      modelInputs:  Seq[ScalarOrSequenceOfDouble[_]],
      modelOutputs: Seq[Val[Double]]) =
      for {
        i ← ScalarOrSequenceOfDouble.prototypes(modelInputs)
        o ← modelOutputs
      } yield (i, o)

    val muOutputs = morrisOutputs(modelInputs, modelOutputs).map { case (i, o) ⇒ Morris.mu(i, o) }
    val muStarOutputs = morrisOutputs(modelInputs, modelOutputs).map { case (i, o) ⇒ Morris.muStar(i, o) }
    val sigmaOutputs = morrisOutputs(modelInputs, modelOutputs).map { case (i, o) ⇒ Morris.sigma(i, o) }

    FromContextTask("MorrisAggregation") { p ⇒
      import p._

      // retrieve the metadata passed by the sampling method
      val factorChanged: Array[String] = context(MorrisSampling.varFactorName.toArray)
      val deltas: Array[Double] = context(MorrisSampling.varDelta.toArray)

      // for each part of the space we were asked to explore, compute the elementary effects and returns them
      // into the variables passed by the user
      val List(mu, muStar, sigma) =
        morrisOutputs(modelInputs, modelOutputs).map {
          case (input, output) ⇒
            val outputValues: Array[Double] = context(output.toArray)
            MorrisSampling.Log.logger.fine("Processing the elementary change for input " + input + " on " + output)
            val (mu, muStar, sigma) = elementaryEffect(input, output, outputValues, factorChanged, deltas)
            List(mu, muStar, sigma)
        }.transpose.toList

      context ++
        (muOutputs zip mu).map { case (v, i) ⇒ Variable.unsecure(v, i) } ++
        (muStarOutputs zip muStar).map { case (v, i) ⇒ Variable.unsecure(v, i) } ++
        (sigmaOutputs zip sigma).map { case (v, i) ⇒ Variable.unsecure(v, i) }
    } set (
      // we expect as inputs:
      // ... the outputs of the model we want to analyze
      inputs ++= modelOutputs.map(_.array),

      // ... the metadata generated by the sampling
      inputs += (
        MorrisSampling.varFactorName.toArray,
        MorrisSampling.varDelta.toArray
      ),

        // we provide as outputs
        // ... our output indicators
        outputs ++= (muOutputs, muStarOutputs, sigmaOutputs)
    )

  }
}