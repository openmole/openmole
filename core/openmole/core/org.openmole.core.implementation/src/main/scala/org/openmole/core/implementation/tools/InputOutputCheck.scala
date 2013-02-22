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

package org.openmole.core.implementation.tools

import org.openmole.core.model.data._
import InputOutputCheck._
import org.openmole.misc.exception.InternalProcessingError

object InputOutputCheck {

  trait InputError
  case class InputNotFound(input: Prototype[_]) extends InputError {
    override def toString = s"Input data $input has not been found"
  }

  case class InputTypeMismatch(input: Prototype[_], found: Prototype[_]) extends InputError {
    override def toString = s"Input data named $found is of an incompatible with the required $input"
  }

  trait OutputError

  case class OutputNotFound(output: Prototype[_]) extends OutputError {
    override def toString = s"Output data $output has not found"
  }

  case class OutputTypeMismatch(output: Prototype[_], variable: Variable[_]) extends OutputError {
    override def toString = s"Output data type mismatch for $output the variable found is of type ${variable.prototype} the value type is ${variable.value.getClass}"
  }

}

trait InputOutputCheck {

  def inputs: DataSet
  def outputs: DataSet
  def parameters: ParameterSet

  protected def verifyInput(context: Context): Iterable[InputError] =
  (for {
      d ← inputs.toList
      if (!(d.mode is Optional))
      p = d.prototype
    } yield context.variable(p.name) match {
          case None ⇒ Some(InputNotFound(p))
          case Some(v) ⇒ if (!p.isAssignableFrom(v.prototype)) Some(InputTypeMismatch(p, v.prototype)) else None
  }).flatten

  protected def verifyOutput(context: Context): Iterable[OutputError] =
    outputs.flatMap {
      d ⇒
        context.variable(d.prototype) match {
          case None ⇒
            if (!(d.mode is Optional)) Some(OutputNotFound(d.prototype)) else None
          case Some(v) ⇒
            if (!d.prototype.accepts(v.value))
              Some(OutputTypeMismatch(d.prototype, v))
            else None
        }
    }

  protected def filterOutput(context: Context): Context =
    Context(outputs.toList.flatMap( o => context.variable(o.prototype): Option[Variable[_]]))

  protected def initializeInput(context: Context): Context =
      context ++
        parameters.flatMap {
          parameter ⇒
            if (parameter.`override` || !context.contains(parameter.variable.prototype.name)) Some(parameter.variable)
            else Option.empty[Variable[_]]
        }


  def perform(context: Context, process: (Context => Context)) = {
    val initializedContext = initializeInput(context)
    val inputErrors = verifyInput(initializedContext)
    if(!inputErrors.isEmpty) throw new InternalProcessingError(s"Input errors have been found in ${this}: ${inputErrors.mkString(", ")}.")

    val result =
      try context + process(initializedContext)
      catch {
        case e: Throwable ⇒
          throw new InternalProcessingError(e, s"Error for context values in ${this} ${context.prettified()}")
      }

    val outputErrors = verifyOutput(result)
    if (!outputErrors.isEmpty) throw new InternalProcessingError(s"Output errors in ${this}: ${outputErrors.mkString(", ")}.")
    filterOutput(result)
  }

}
