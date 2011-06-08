/*
 * Copyright (C) 2010 reuillon
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

package org.openmole.core.model.capsule

import org.openmole.core.model.task.IExplorationTask
import org.openmole.core.model.transition.IExplorationTransition

/**
 * A capsule containing an exploration task. This capsule should be followed
 * by an ExplorationTransition.
 */
trait IExplorationCapsule extends IGenericCapsule {
  
  /**
   * Get the Some(task) assigned to this capsule or None if not the task has not
   * been assigned.
   *
   * @return Some(task) inside this capsule or None if not the task has not been assigned
   */
  def task: Option[IExplorationTask]
    
  /**
   * Assing a task to this capsule.
   * 
   * @param task the task to assign to this capsule.
   */
  def task_=(task: IExplorationTask)
    
  /**
   * Assing an option of task to this capsule.
   * 
   * @param task the option of task to assign to this capsule.
   */
  def task_=(task: Option[IExplorationTask])

  /**
   * Add an output transition to this capsule.
   * 
   * @param transition the transition to add
   * @return the capsule itself
   */
  def addOutputTransition(transition: IExplorationTransition): this.type
    
  /**
   * Get all the output transitions plugged to this capsule.
   *
   * @return all the output transitions plugged to this capsule
   */
  def outputTransitions: Iterable[IExplorationTransition]
}
