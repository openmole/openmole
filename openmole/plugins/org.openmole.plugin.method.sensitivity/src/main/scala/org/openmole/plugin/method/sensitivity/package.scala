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
import org.openmole.core.dsl.*
import org.openmole.core.dsl.extension.*
import org.openmole.plugin.tool.pattern._

package object sensitivity {

  object Sensitivity {
    /**
     * For a given input of the model, and a given output of a the model,
     * returns the subspace of analysis, namely: the subspace made of these input and
     * output, with the additional outputs for this sensitivity quantified over
     * mu, mu* and sigma.
     */
    //    def subspaceForInputOutput(input: Val[Double], output: Val[Double]): SubspaceToAnalyze = {
    //      SubspaceToAnalyze(
    //        input,
    //        output
    //      )
    //    }

    /**
     * Casts a Val[?] (value of something) to a Val[Double]
     * (value containing a Double), and throws a nice
     * exception in case it's not possible
     */
    def toValDouble(v: Val[?]): Val[Double] = v match
      case Val.caseDouble(vd) => vd
      case _                  => throw new IllegalArgumentException("expect inputs of type Double, but received " + v)

    def variableResults(inputs: Seq[Val[?]], outputs: Seq[Val[?]], coefficient: (Val[?], Val[?]) => Val[?]) = FromContext { p =>
      import p._

      def results =
        outputs.map { o =>
          val vs = inputs.map { i => coefficient(i, o) }
          Seq(o.name) ++ vs.map(v => context(v))
        }

      def allVals = Seq(Val[String]("output")) ++ inputs
      (results.transpose(using identity) zip allVals).map { (value, v) => Variable.unsecure(v.array, value) }
    }

  }

  /**
   * Decorator of the Morris method to implicitely call MorrisHook in the DSL with hook.
   * @param dsl
   */
  implicit class MorrisHookDecorator[M](m: M)(using method: ExplorationMethod[M, SensitivityMorris.Method]) extends MethodHookDecorator[M, SensitivityMorris.Method](m):
    def hook(output: WritableOutput)(using ScriptSourceData): Hooked[M] =
      val dsl = method(m)
      implicit val defScope = dsl.scope
      Hooked(m, SensitivityMorris.MorrisHook(dsl.method, output))

  /**
   * Decorator of the Saltelli method to implicitely call SaltelliHook in the DSL with hook.
   * @param dsl
   */
  implicit class SaltelliHookDecorator[M](m: M)(implicit method: ExplorationMethod[M, SensitivitySaltelli.Method]) extends MethodHookDecorator[M, SensitivitySaltelli.Method](m):
    def hook(output: WritableOutput)(using ScriptSourceData): Hooked[M] =
      val dsl = method(m)
      implicit val defScope = dsl.scope
      Hooked(m, SensitivitySaltelli.SaltelliHook(dsl.method, output))


}

