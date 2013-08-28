/*
 * Copyright (C) 2011 Mathieu Mathieu Leclaire <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.workflow

import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.implementation.commons._
import org.openmole.core.model.transition._
import org.openmole.core.implementation.transition._
import org.openmole.core.model.mole.ICapsule
import org.openmole.ide.misc.tools.util.ID

class TransitionUI(
    val source: CapsuleUI,
    val target: InputSlotWidget,
    var transitionType: TransitionType,
    var condition: Option[String] = None,
    var filteredPrototypes: List[PrototypeDataProxyUI] = List.empty) extends ConnectorUI with ID {

  def coreObject(source: ICapsule,
                 target: Slot,
                 condition: ICondition,
                 filtered: List[String]) = transitionType match {
    case SimpleTransitionType      ⇒ new Transition(source, target, condition, Block(filtered: _*))
    case AggregationTransitionType ⇒ new AggregationTransition(source, target, condition, Block(filtered: _*))
    case ExplorationTransitionType ⇒ new ExplorationTransition(source, target, condition, Block(filtered: _*))
    case EndTransitionType         ⇒ new EndExplorationTransition(source, target, condition, Block(filtered: _*))
  }
}