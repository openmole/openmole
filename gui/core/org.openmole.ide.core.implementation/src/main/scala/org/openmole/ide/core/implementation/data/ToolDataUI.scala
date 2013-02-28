/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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
package org.openmole.ide.core.implementation.data

import org.openmole.core.model.data.DataSet
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.implementation.registry.{ KeyGenerator, KeyPrototypeGenerator }
import org.openmole.core.model.mole.{ ICapsule, Hooks, Sources, IMole }
import org.openmole.core.model.transition.{ IAggregationTransition, ITransition }
import org.openmole.ide.core.implementation.dataproxy.{ Proxys, PrototypeDataProxyUI }
import org.openmole.ide.core.implementation.prototype.GenericPrototypeDataUI

object ToolDataUI {
  def implicitPrototypes(coreInputs: Unit ⇒ DataSet,
                         prototypesIn: List[IPrototypeDataProxyUI],
                         coreOutputs: Unit ⇒ DataSet,
                         prototypesOut: List[IPrototypeDataProxyUI]): (List[IPrototypeDataProxyUI], List[IPrototypeDataProxyUI]) = {

    def protoFilter(ds: DataSet, protos: List[IPrototypeDataProxyUI]) = {
      ds.map { i ⇒ KeyPrototypeGenerator(i.prototype) }.toList.diff(protos.map {
        p ⇒ KeyPrototypeGenerator(p)
      }).map { KeyPrototypeGenerator.prototype }
    }

    (protoFilter(coreInputs(), prototypesIn), protoFilter(coreOutputs(), prototypesOut))
  }

  def computePrototypeFromAggregation(mole: IMole) = {
    mole.transitions.foreach {
      _ match {
        //FIXME SOURCES AND HOOKS
        case t: ITransition with IAggregationTransition ⇒ t.data(mole, Sources.empty, Hooks.empty).foreach {
          d ⇒
            val (protoType, dim) = KeyGenerator.stripArrays(d.prototype.`type`)
            val proto = new PrototypeDataProxyUI(GenericPrototypeDataUI(d.prototype.name, dim + 1)(protoType), generated = true)
            if (!KeyPrototypeGenerator.isPrototype(proto))
              Proxys.prototypes += proto
        }
        case _ ⇒
      }
    }
  }

  def buildUnknownPrototypes(mole: IMole, coreCapsule: ICapsule) = {
    (coreCapsule.inputs(mole, Sources.empty, Hooks.empty).toList ++ coreCapsule.outputs(mole, Sources.empty, Hooks.empty)) foreach {
      d ⇒ KeyPrototypeGenerator.buildUnknownPrototype(d.prototype)
    }
  }
}