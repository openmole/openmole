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

package org.openmole.plugin.method

import org.openmole.core.context._
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.core.workflow.dsl._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.puzzle._
import org.openmole.core.workflow.sampling._
import org.openmole.core.workflow.task._
import org.openmole.core.workflow.tools.ScalarOrSequenceOfDouble
import org.openmole.core.workflow.validation.DataflowProblem._
import org.openmole.core.workflow.validation._
import org.openmole.core.workflow.transition.Slot

package object sensitivity {

  implicit def scope = DefinitionScope.Internal

  /**
   * For a given input of the model, and a given output of a the model,
   * returns the subspace of analysis, namely: the subspace made of these input and
   * output, with the additional outputs for this sensitivity quantified over
   * mu, mu* and sigma.
   */
  def subspaceForInputOutput(input: Val[Double], output: Val[Double]): SubspaceToAnalyze = {
    SubspaceToAnalyze(
      input,
      output
    )
  }

  /**
   * Casts a Val[_] (value of something) to a Val[Double]
   * (value containing a Double), and throws a nice
   * exception in case it's not possible
   */
  def toValDouble(v: Val[_]): Val[Double] = v match {
    case Val.caseDouble(vd) ⇒ vd
    case _                  ⇒ throw new IllegalArgumentException("expect inputs to be of type Double, but received " + v)
  }

  /**
   * A Morris Sensitivity Analysis takes a puzzle (a model) that we want to analyse,
   * the list of the inputs (and their ranges), the list of outputs we want
   * to test the sensitivity of the inputs on, how many repetitions to conduct,
   * and in how many levels inputs should be analyzed on.
   *
   * The sensitivity analysis is driven as an exploration based on the Morris sampling,
   * running the model, and aggregating the result to produce the sensitivty outputs.
   */
  def SensitivityMorris(
    evaluation:  Puzzle,
    inputs:      Seq[ScalarOrSequenceOfDouble[_]],
    outputs:     Seq[Val[Double]],
    repetitions: Int,
    levels:      Int): Puzzle = {

    // the sampling for Morris is a One At a Time one,
    // with respect to the user settings for repetitions, levels and inputs
    val sampling = MorrisSampling(repetitions, levels, inputs)
    val exploration = ExplorationTask(sampling)
    val cExploration = Capsule(exploration, strain = true)

    // generate the space of to analyze for outputs:
    // for each input, for each output, add to the space
    // the subspace corresponding to this one
    val space: Seq[SubspaceToAnalyze] = inputs.flatMap(
      input ⇒ outputs.map(
        output ⇒ sensitivity.subspaceForInputOutput(
          sensitivity.toValDouble(input.prototype),
          output))).toSeq

    // the aggregation obviously is a Morris aggregation!
    // it collects all the specific inputs added from the sampling
    // to interpret the results
    val aggregation = MorrisAggregation(space)
    val cAggregation = Capsule(aggregation)
    val sAggregation = Slot(cAggregation)

    ((cExploration -< evaluation >- sAggregation) &
      (cExploration -- sAggregation))

  }

}

