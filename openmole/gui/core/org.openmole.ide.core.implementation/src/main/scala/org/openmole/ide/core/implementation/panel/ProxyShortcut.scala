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
import org.openmole.ide.core.implementation.sampling.SamplingCompositionDataUI

trait ProxyShortcut {

  def proxyShorcut(base: Base, dataUI: DataUI): NewConceptPanel = {
    proxyShorcut(base: Base, dataUI match {
      case x: TaskDataUI with IExplorationTaskDataUI ⇒ x
      case x: TaskDataUI                             ⇒ x
      case x: SourceDataUI                           ⇒ x
      case x: HookDataUI                             ⇒ x
      case x: CapsuleDataUI                          ⇒ x
      case x: SamplingCompositionDataUI              ⇒ x
    })
  }

  def proxyShorcut(base: Base, dataUI: TaskDataUI) = new NewConceptPanel(base.savePanel) {
    addPrototype
    addSamplingComposition
  }

  def protoProxyShortcut(base: Base) = new NewConceptPanel(base.savePanel) {
    addPrototype
  }

  def proxyShorcut(base: Base, dataUI: SourceDataUI, index: Int) = protoProxyShortcut(base)

  def proxyShorcut(base: Base, dataUI: HookDataUI, index: Int) = protoProxyShortcut(base)

  def proxyShorcut(base: Base, dataUI: SamplingCompositionDataUI, index: Int) = protoProxyShortcut(base)

  def proxyShorcut(base: Base, dataUI: CapsuleDataUI, index: Int) = new NewConceptPanel(base.savePanel) {
    addEnvironment
    addSource
    addHook
  }

}