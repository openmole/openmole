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

import java.io.PrintStream

import org.openmole.core.dsl
import org.openmole.core.dsl._
import org.openmole.core.dsl.extension._
import org.openmole.plugin.tool.pattern._
import org.openmole.core.csv
import org.openmole.core.workflow.format.WritableOutput

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


    def variableResults[F, D](inputs: Seq[Val[_]], outputs: Seq[Val[_]], coefficient: (Val[_], Val[_]) ⇒ Val[_])(implicit outputFormat: OutputFormat[F, D]) = FromContext { p ⇒
      import p._

      def results = outputs.map { o ⇒
        val vs = inputs.map { i ⇒ coefficient(i, o) }
        Seq(o.name) ++ vs.map(v ⇒ context(v))
      }

      def allVals = Seq(Val[String]("output")) ++ inputs
      (results.transpose zip allVals).map { case (value, v) => Variable.unsecure(v.array, value) }
    }


    case class SaltelliParams(inputs: Seq[ScalarOrSequenceOfDouble[_]], outputs: Seq[Val[_]])
    case class MorrisParams(inputs: Seq[ScalarOrSequenceOfDouble[_]], outputs: Seq[Val[_]])
  }

  /**
    * Decorator of the Saltelli method to implicitely call SaltelliHook in the DSL with hook.
    * @param dsl
    */
  implicit class SaltelliMethodContainer(dsl: DSLContainer[Sensitivity.SaltelliParams])  extends DSLContainerHook(dsl) {
    def hook[F](output: WritableOutput, format: F = CSVOutputFormat(unrollArray = true))(implicit outputFormat: OutputFormat[F, Sensitivity.SaltelliParams]): DSLContainer[Sensitivity.SaltelliParams] = {
      implicit val defScope = dsl.scope
      dsl hook SaltelliHook(dsl, output, format)
    }
  }


  /**
    * Decorator of the Morris method to implicitely call MorrisHook in the DSL with hook.
    * @param dsl
    */
  implicit class MorrisMethodContainer(dsl: DSLContainer[Sensitivity.MorrisParams]) extends DSLContainerHook(dsl) {
    def hook[F](output: WritableOutput, format: F = CSVOutputFormat(unrollArray = true))(implicit outputFormat: OutputFormat[F, Sensitivity.MorrisParams]): DSLContainer[Sensitivity.MorrisParams] = {
      implicit val defScope = dsl.scope
      dsl hook MorrisHook(dsl, output, format)
    }
  }


  /**
   * A Morris Sensitivity Analysis takes a puzzle (a model) that we want to analyse,
   * the list of the inputs (and their ranges), the list of outputs we want
   * to test the sensitivity of the inputs on, how many repetitions to conduct,
   * and in how many levels inputs should be analyzed on.
   *
   * The sensitivity analysis is driven as an exploration based on the Morris sampling,
   * running the model, and aggregating the result to produce the sensitivity outputs.
   */
  def SensitivityMorris(
    evaluation:  DSL,
    inputs:      Seq[ScalarOrSequenceOfDouble[_]],
    outputs:     Seq[Val[Double]],
    sample:      Int,
    level:       Int,
    scope: DefinitionScope = "sensitivity morris") = {

    implicit def defScope = scope

    // the sampling for Morris is a One At a Time one,
    // with respect to the user settings for repetitions, levels and inputs
    val sampling = MorrisSampling(sample, level, inputs)

    // the aggregation obviously is a Morris aggregation!
    // it collects all the specific inputs added from the sampling
    // to interpret the results
    val aggregation = MorrisAggregation(inputs, outputs)


    val w =
      MapReduce(
        evaluation = evaluation,
        sampler = ExplorationTask(sampling),
        aggregation = aggregation
      )

    DSLContainerExtension(w, data = Sensitivity.MorrisParams(inputs, outputs))
  }

  /**
    * Variance-based sensitivity indices (Saltelli method).
    *   Saltelli, A., Annoni, P., Azzini, I., Campolongo, F., Ratto, M., & Tarantola, S. (2010). Variance based sensitivity analysis of model output. Design and estimator for the total sensitivity index. Computer Physics Communications, 181(2), 259-270.
    *
    * @param evaluation
    * @param inputs input prototypes
    * @param outputs outputs double prototypes
    * @param sample number of samples to estimates sensitivity indices
    * @param scope
    * @return
    */
  def SensitivitySaltelli(
    evaluation:   DSL,
    inputs:  Seq[ScalarOrSequenceOfDouble[_]],
    outputs: Seq[Val[Double]],
    sample:      FromContext[Int],
    scope: DefinitionScope = "sensitivity saltelli") = {
    implicit def defScope = scope

    val sampling = SaltelliSampling(sample, inputs: _*)

    val aggregation =
      SaltelliAggregation(
        modelInputs = inputs,
        modelOutputs = outputs,
      ) set (
        dsl.inputs += (SaltelliSampling.matrixName.array, SaltelliSampling.matrixIndex.array)
      )

    val w =
      MapReduce(
        evaluation = evaluation,
        sampler = ExplorationTask(sampling),
        aggregation = aggregation
      )

    DSLContainerExtension(w, data = Sensitivity.SaltelliParams(inputs, outputs))
  }

}

