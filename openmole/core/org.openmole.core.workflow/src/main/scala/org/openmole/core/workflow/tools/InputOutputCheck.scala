/*
 * Copyright (C) 17/02/13 Romain Reuillon
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
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.core.workflow.tools

import org.openmole.core.context._
import org.openmole.core.exception._
import org.openmole.core.expansion.FromContext
import org.openmole.core.fileservice.FileService
import org.openmole.core.preference.Preference
import org.openmole.core.workspace.TmpDirectory
import org.openmole.tool.random._

/**
 * Methods for the validation of inputs/outputs
 */
object InputOutputCheck {

  trait InputError

  /**
   * Missing input
   * @param input
   */
  case class InputNotFound(input: Val[_]) extends InputError {
    override def toString = s"Input data '$input' has not been found"
  }

  /**
   * Wrong type for an input
   * @param input
   * @param found
   */
  case class InputTypeMismatch(input: Val[_], found: Val[_]) extends InputError {
    override def toString = s"Input data named '$found' is of an incompatible with the required '$input'"
  }

  trait OutputError

  /**
   * Missing output
   * @param output
   */
  case class OutputNotFound(output: Val[_]) extends OutputError {
    override def toString = s"Output data '$output' has not been found"
  }

  /**
   * Wrong type for an output
   * @param output
   * @param variable
   */
  case class OutputTypeMismatch(output: Val[_], variable: Variable[_]) extends OutputError {
    override def toString = s"""Type mismatch the content of the output value '${output.name}' of type '${variable.value.getClass}' is incompatible with the output variable '${output}'."""
  }

  protected def verifyInput(inputs: PrototypeSet, context: Context): Iterable[InputError] =
    (for {
      p ← inputs.toList
    } yield context.variable(p.name) match {
      case None    ⇒ Some(InputNotFound(p))
      case Some(v) ⇒ if (!p.isAssignableFrom(v.prototype)) Some(InputTypeMismatch(p, v.prototype)) else None
    }).flatten

  protected def verifyOutput(outputs: PrototypeSet, context: Context): Iterable[OutputError] =
    outputs.flatMap {
      d ⇒
        context.variable(d) match {
          case None ⇒ Some(OutputNotFound(d))
          case Some(v) ⇒
            if (!d.accepts(v.value))
              Some(OutputTypeMismatch(d, v))
            else None
        }
    }

  /**
   * Given a prototype set and a context, construct a [[Context]] with these prototypes only
   *
   * @param outputs
   * @param context
   * @return
   */
  def filterOutput(outputs: PrototypeSet, context: Context): Context =
    Context(outputs.toList.flatMap(o ⇒ context.variable(o): Option[Variable[_]]): _*)

  /**
   * Extend a context with default values (taken into account if overriding is activated or variable is missing in previous context)
   *
   * @param defaults default value
   * @param context context to be extended
   * @return the new context
   */
  def initializeInput(defaults: DefaultSet, context: Context)(implicit randomProvider: RandomProvider, newFile: TmpDirectory, fileService: FileService): Context =
    context ++
      defaults.flatMap {
        parameter ⇒
          if (parameter.`override` || !context.contains(parameter.prototype.name)) Some(parameter.toVariable.from(context))
          else Option.empty[Variable[_]]
      }

  def perform(obj: Any, inputs: PrototypeSet, outputs: PrototypeSet, defaults: DefaultSet, process: FromContext[Context])(implicit preference: Preference) = FromContext { p ⇒
    import p._
    val initializedContext = initializeInput(defaults, context)
    val inputErrors = verifyInput(inputs, initializedContext)
    if (!inputErrors.isEmpty) throw new InternalProcessingError(s"Input errors have been found in ${obj}: ${inputErrors.mkString(", ")}.")

    val result =
      try initializedContext + process.from(initializedContext)
      catch {
        case e: Throwable ⇒
          throw new InternalProcessingError(e, s"Error in ${obj} for context values ${initializedContext.prettified}")
      }

    val outputErrors = verifyOutput(outputs, result)
    if (!outputErrors.isEmpty) throw new InternalProcessingError(s"Output errors in ${obj}: ${outputErrors.mkString(", ")}.")
    filterOutput(outputs, result)
  } validate { p ⇒
    import p._
    process.validate(p.inputs)
  }

}
