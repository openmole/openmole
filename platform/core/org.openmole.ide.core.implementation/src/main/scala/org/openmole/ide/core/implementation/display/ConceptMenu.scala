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

package org.openmole.ide.core.implementation.display

import java.awt.Dimension
import org.openmole.ide.core.implementation.action.EnvironmentDisplayAction
import org.openmole.ide.core.implementation.action.PrototypeDisplayAction
import org.openmole.ide.core.implementation.action.SamplingDisplayAction
import org.openmole.ide.core.implementation.action.TaskDisplayAction
import scala.swing.Menu
import scala.swing.MenuBar
import scala.swing.MenuItem
import org.openmole.ide.core.model.commons.Constants._

class ConceptMenu {

  val environmentMenu = new Menu("Environment")
  val environementClasses = new Menu("New")
  EnvironmentDisplay.implementationClasses.toList.sortBy(_.factory.displayName).foreach(
    d => environementClasses.contents += new MenuItem(new EnvironmentDisplayAction(d, ENVIRONMENT)))
  
  environmentMenu.contents += environementClasses
  
  
  val taskMenu = new Menu("Task")
  val taskClasses = new Menu("New")
  TaskDisplay.implementationClasses.toList.sortBy(_.factory.displayName).foreach(
    d => taskClasses.contents += new MenuItem(new TaskDisplayAction(d, TASK)))
  
  taskMenu.contents += taskClasses
  
  
  val prototypeMenu = new Menu("Prototype")
  val prototypeClasses = new Menu("New")
  PrototypeDisplay.implementationClasses.toList.sortBy(_.factory.displayName).foreach(
    d => prototypeClasses.contents += new MenuItem(new PrototypeDisplayAction(d, PROTOTYPE)))
  
  prototypeMenu.contents += prototypeClasses
  
  
  val samplingMenu = new Menu("Sampling")
  val samplingClasses = new Menu("New")
  SamplingDisplay.implementationClasses.toList.sortBy(_.factory.displayName).foreach(
    d => samplingClasses.contents += new MenuItem(new SamplingDisplayAction(d, SAMPLING)))
  
  samplingMenu.contents += samplingClasses
  
  
  
  def menuBar = new MenuBar{
    contents.append(prototypeMenu,taskMenu,samplingMenu,environmentMenu)
    minimumSize = new Dimension(size.width,50)
  }
}

