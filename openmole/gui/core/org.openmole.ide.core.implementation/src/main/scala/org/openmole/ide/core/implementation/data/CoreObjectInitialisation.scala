/*
 * Copyright (C) 10/07/13 Romain Reuillon
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

package org.openmole.ide.core.implementation.data

import org.openmole.core.implementation.tools.InputOutputBuilder
import org.openmole.core.model.data._
import org.openmole.core.implementation.data._
import org.openmole.ide.misc.tools.check.TypeCheck

trait CoreObjectInitialisation extends InputPrototype with OutputPrototype {

  def initialise(co: InputOutputBuilder) = {
    co addInput DataSet(inputs.map(_.dataUI.coreObject.get))
    co addOutput DataSet(outputs.map(_.dataUI.coreObject.get))
    co addParameter parameterSet
  }

  def parameterSet =
    ParameterSet(inputParameters.flatMap {
      case (protoProxy, v) ⇒
        if (!v.isEmpty) {
          val proto = protoProxy.dataUI.coreObject.get
          val (msg, obj) = TypeCheck(v, proto)
          obj match {
            case Some(x: Object) ⇒ Some(Parameter(proto.asInstanceOf[Prototype[Any]], x))
            case _               ⇒ None
          }
        }
        else None
    }.toList)

}
