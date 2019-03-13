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

import org.openmole.core.expansion.FromContext
import org.openmole.core.outputmanager.OutputManager
import org.openmole.core.workflow.builder.DefinitionScope
import org.openmole.core.workflow.tools.ScalarOrSequenceOfDouble
import org.openmole.core.workflow.validation.DataflowProblem._
import org.openmole.core.workflow.validation._
import org.openmole.core.workflow.transition.TransitionSlot
import org.openmole.core.dsl
import org.openmole.core.dsl._
import org.openmole.plugin.method.directsampling._

package object sensitivity {

  object Sensitivity {
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
      case _ ⇒ throw new IllegalArgumentException("expect inputs to be of type Double, but received " + v)
    }


    def outputs(
      modelInputs: Seq[ScalarOrSequenceOfDouble[_]],
      modelOutputs: Seq[Val[Double]]) =
      for {
        i ← ScalarOrSequenceOfDouble.prototypes(modelInputs)
        o ← modelOutputs
      } yield (i, o)
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
    evaluation:  DSL,
    inputs:      Seq[ScalarOrSequenceOfDouble[_]],
    outputs:     Seq[Val[Double]],
    repetitions: Int,
    levels:      Int,
    scope: DefinitionScope = "sensitivity morris") = {

    implicit def defScope = scope

    // the sampling for Morris is a One At a Time one,
    // with respect to the user settings for repetitions, levels and inputs
    val sampling = MorrisSampling(repetitions, levels, inputs)

    // the aggregation obviously is a Morris aggregation!
    // it collects all the specific inputs added from the sampling
    // to interpret the results
    val aggregation = MorrisAggregation(inputs, outputs)

    DirectSampling(
      evaluation = evaluation,
      sampling = sampling,
      aggregation = aggregation,
      scope = scope
    )
  }

  def SensitivitySaltelli(
    evaluation:   DSL,
    inputs:  Seq[ScalarOrSequenceOfDouble[_]],
    outputs: Seq[Val[Double]],
    samples:      FromContext[Int],
    scope: DefinitionScope = "sensitivity saltelli") = {

    implicit def defScope = scope

    val sampling = SaltelliSampling(samples, inputs: _*)

    val aggregation =
      SaltelliAggregation(
        modelInputs = inputs,
        modelOutputs = outputs,
      ) set (
        dsl.inputs += (SaltelliSampling.matrixName.array, SaltelliSampling.matrixIndex.array)
      )

    DirectSampling(
      evaluation = evaluation,
      sampling = sampling,
      aggregation = aggregation,
      scope = scope
    )
  }

}

