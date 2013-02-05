/*
 * Copyright (C) 2012 mathieu
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

package org.openmole.ide.plugin.builder.base

import org.openmole.ide.core.implementation.registry.{ BuilderActivator, OSGiActivator }
import org.openmole.ide.core.model.factory.IBuilderFactoryUI
import org.openmole.core.implementation.puzzle.Puzzle
import org.openmole.ide.core.model.workflow.IMoleSceneManager

class Activator extends OSGiActivator with BuilderActivator {

  override def builderFactories = List(new IBuilderFactoryUI {
    def name = "Exploration"
    def buildPanelUI(puzzle: Puzzle, manager: IMoleSceneManager) = new ExplorationBuilderPanelUI(puzzle, manager)
  })
}
