/*
 * Copyright (C) 2013 <mathieu.Mathieu Leclaire at openmole.org>
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
package org.openmole.ide.core.implementation.panel

import org.openmole.ide.misc.widget.{ MainLinkLabel, PluginPanel }
import swing.Action

class NewConceptPanel(index: Int) extends PluginPanel("wrap") {

  def addPrototype = add("> prototype", new Action("") { def apply = ConceptMenu.createAndDisplayPrototype(index) })

  def addHook = add("> hook", new Action("") { def apply = ConceptMenu.createAndDisplayHook(index) })

  def addSource = add("> source", new Action("") { def apply = ConceptMenu.createAndDisplaySource(index) })

  def addEnvironment = add("> environment", new Action("") { def apply = ConceptMenu.createAndDisplayEnvironment(index) })

  def addSamplingComposition = add("> sampling", new Action("") { def apply = ConceptMenu.createAndDisplaySamplingComposition(index) })

  def add(s: String, action: Action) = contents += new MainLinkLabel(s, action)
}