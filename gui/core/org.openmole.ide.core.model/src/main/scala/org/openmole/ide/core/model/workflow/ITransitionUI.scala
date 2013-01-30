/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.model.workflow

import org.openmole.ide.core.model.commons.TransitionType
import org.openmole.core.model.transition.{ Filter, ICondition, Slot }
import org.openmole.core.implementation.transition._
import org.openmole.ide.core.model.commons.TransitionType._
import org.openmole.core.model.mole.ICapsule
import org.openmole.misc.exception.UserBadDataError

trait ITransitionUI extends IConnectorUI {
  def condition: Option[String]

  def condition_=(c: Option[String])

  def transitionType: TransitionType.Value

  def transitionType_=(t: TransitionType.Value)

  def coreObject(source: ICapsule,
                 target: Slot,
                 condition: ICondition,
                 filtered: List[String]) = transitionType match {
    case BASIC_TRANSITION ⇒ new Transition(source, target, condition, Filter(filtered: _*))
    case AGGREGATION_TRANSITION ⇒ new AggregationTransition(source, target, condition, Filter(filtered: _*))
    case EXPLORATION_TRANSITION ⇒ new ExplorationTransition(source, target, condition, Filter(filtered: _*))
    case END_TRANSITION ⇒ new EndExplorationTransition(source, target, condition, Filter(filtered: _*))
    case _ ⇒ throw new UserBadDataError("No matching type between capsule " + source + " and " + target + ". The transition can not be built")
  }
}