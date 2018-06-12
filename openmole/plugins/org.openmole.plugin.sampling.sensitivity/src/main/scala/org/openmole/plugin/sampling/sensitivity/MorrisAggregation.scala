/*
 * Copyright (C) 2018 Samuel Thiriot
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

package org.openmole.plugin.sampling.sensitivity

import org.openmole.core.context.{ Namespace, Val, Variable }
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.task.ClosureTask

/**
 * Describes a part of the space of inputs/ouputs of a model (or actually, puzzle)
 * on which to assess the sensitivity.
 * Describes that we wish to analyze the impact of variations of a given input on a given output,
 * and return it into variables indicatorMu, indicatorMu* and indicatorSigma.
 */
case class SubspaceToAnalyze(
  input:           Val[Double],
  output:          Val[Double],
  indicatorMu:     Val[Double],
  indicatorMuStar: Val[Double],
  indicatorSigma:  Val[Double]
)

object MorrisAggregation {

  /**
   * Variables produced by Morris analysis
   */
  val namespace = Namespace("morris")

  val varMu = Val[Double]("mu", namespace = MorrisSampling.namespace)
  val varMuStar = Val[Double]("mustar", namespace = MorrisSampling.namespace)
  val varSigma = Val[Double]("sigma", namespace = MorrisSampling.namespace)

  val varIndexTrajectories = Val[Array[Int]]("trajid", namespace = MorrisSampling.namespace)
  val varIndexPoints = Val[Array[Int]]("pointid", namespace = MorrisSampling.namespace)
  val varFactorNames = Val[Array[String]]("factorname", namespace = MorrisSampling.namespace)
  val varDeltas = Val[Array[Double]]("delta", namespace = MorrisSampling.namespace)

  /**
   * Takes lists for trajectories:
   * idtraj:   [0,   0,    0,  0,  1,  1,  1,  1]
   * idpoint:  [0,   1,    2,  3,  0,  1,  2,  3]
   * factor:   [,    a,    b,  c,  ,   c,  a,  b]
   * deltas:   [0.,  0.2,  -1, 0.1,0,  -1, 0.1, 2]
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
    inputName:     String,
    outputValues:  Array[Double],
    factorChanged: Array[String], deltas: Array[Double]): (Double, Double, Double) = {

    // indices of results in which the input had been changed
    val indicesWithEffect: Array[Int] = factorChanged.zipWithIndex
      .collect { case (name, idx) if (inputName == name) ⇒ idx }
    val r: Int = indicesWithEffect.length

    System.out.println("searching for " + inputName + " in factorChanges " + factorChanged.toList)
    System.out.println("found " + r + " repetitions: " + indicesWithEffect.toList)
    System.out.println("will use them in " + outputValues.toList)

    // ... so we know each index - 1 leads to the case before changing the factor
    // elementary effects
    val elementaryEffects: Array[Double] = indicesWithEffect.map(idxChanged ⇒ (outputValues(idxChanged) - outputValues(idxChanged - 1)) / (deltas(idxChanged)))
    // leading to indicators
    val rD: Double = r.toDouble
    val mu: Double = elementaryEffects.sum / rD
    val muStar: Double = elementaryEffects.reduceLeft(sumAbs) / rD
    val sigma: Double = Math.sqrt(elementaryEffects.map(ee ⇒ squaredDiff(ee, mu)).sum / rD)

    (mu, muStar, sigma)

  }

  /**
   * Takes from the user the list of subspaces to analyze,
   * that is tuples of (model input, model output) for which
   * indicators (mu, mu* and sigma) should be computed.
   * Receives at execution time the lists of
   */

  def apply[T](subspaces: SubspaceToAnalyze*)(implicit name: sourcecode.Name, definitionScope: DefinitionScope) = {

    // the list of all the user inputs corresponding to model inputs we tuned
    //val allInputsForModelInputs: List[Val[Array[Double]]] = subspaces.map(s ⇒ s.input.toArray).toList.distinct

    // the list of all the user inputs corresponding to model outputs
    val allInputsForModelOutputs: List[Val[Array[Double]]] = subspaces.map(s ⇒ s.output.toArray).toList.distinct

    // the list of all the indicators produces by the aggregation task
    val allOutputIndicators: List[Val[Double]] = subspaces.flatMap(s ⇒ List(s.indicatorMu, s.indicatorMuStar, s.indicatorSigma)).toList

    System.out.println("in apply with parameters: " + subspaces)

    ClosureTask("MorrisAggregation") { (context, _, _) ⇒
      System.out.println("received context " + context)

      // retrieve the metadata passed by the sampling method
      val factorChanged: Array[String] = context(MorrisSampling.varFactorName.toArray)
      val iterationIdx: Array[Int] = context(MorrisSampling.varIndexPoint.toArray)
      val trajectoryIdx: Array[Int] = context(MorrisSampling.varIndexTrajectory.toArray)
      val deltas: Array[Double] = context(MorrisSampling.varDelta.toArray)

      // for each part of the space we were asked to explore, compute the elementary effects and returns them
      // into the variables passed by the user
      subspaces.flatMap { subspace ⇒
        // retrieve the values to analyze
        val inputName: String = subspace.input.name
        //val inputValue:Double = context(subspace.input)
        val outputValues: Array[Double] = context(subspace.output.toArray)
        val outputName: String = subspace.output.name

        System.out.println("Processing the elementary change for input " + inputName + " on " + outputName)

        val (mu: Double, muStar: Double, sigma: Double) = elementaryEffect(inputName, outputValues, factorChanged, deltas)

        System.out.println("Processed the elementary change for input " + inputName + " on " + outputName + " => mu=" + mu + ", mu*=" + muStar + ", sigma=" + sigma)
        List(
          Variable(subspace.indicatorMu, mu),
          Variable(subspace.indicatorMuStar, muStar),
          Variable(subspace.indicatorSigma, sigma))
      }

    } set (
      // we expect as inputs:
      // ... the inputs of the model we tuned
      //inputs ++= allInputsForModelInputs,
      // ... the outputs of the model we want to analyze
      inputs ++= allInputsForModelOutputs,
      // ... the metadata generated by the sampling
      inputs += (
        MorrisSampling.varIndexTrajectory.toArray,
        MorrisSampling.varIndexPoint.toArray,
        MorrisSampling.varFactorName.toArray,
        MorrisSampling.varDelta.toArray
      ),
        // we provide as outputs
        // ... our output indicators
        outputs ++= allOutputIndicators
    )

  }
}