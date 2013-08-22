/*
 * Copyright (C) 2013 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.core.implementation.panel

import org.openmole.ide.core.implementation.data._

trait ProxyShortcut {

  def proxyShorcut(dataUI: DataUI, index: Int): NewConceptPanel = proxyShorcut(dataUI match {
    case x: TaskDataUI with IExplorationTaskDataUI ⇒ x
    case x: TaskDataUI                             ⇒ x
    case x: SourceDataUI                           ⇒ x
    case x: HookDataUI                             ⇒ x
    case x: CapsuleDataUI                          ⇒ x
  }, index)

  def proxyShorcut(dataUI: TaskDataUI, index: Int) = new NewConceptPanel(index) {
    addPrototype
    addSamplingComposition
  }

  def proxyShorcut(dataUI: SourceDataUI, index: Int) = new NewConceptPanel(index) {
    addPrototype
  }

  def proxyShorcut(dataUI: HookDataUI, index: Int) = new NewConceptPanel(index) {
    addPrototype
  }

  def proxyShorcut(dataUI: CapsuleDataUI, index: Int) = new NewConceptPanel(index) {
    addEnvironment
    addSource
    addHook
  }
}