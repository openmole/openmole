/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.control

import org.openmole.ide.core.model.commons.MoleSceneType._
import org.apache.commons.collections15.bidimap.DualHashBidiMap
import org.openmole.ide.core.implementation.workflow.MoleScene
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.exception.GUIUserBadDataError
import scala.collection.JavaConversions._
import scala.swing.ScrollPane
import scala.swing.TabbedPane

object TabManager {
  var tabbedPane= new TabbedPane
  var sceneTabs = new DualHashBidiMap[IMoleScene,TabbedPane.Page]
  
  def currentScene: IMoleScene = {
    sceneTabs.getKey(tabbedPane.selection.page) match {
      case m: MoleScene => m
      case _=> throw new GUIUserBadDataError("The current scene is not a mole view, please first select a mole before build it")
    }
  }
  
  def removeSceneTab(ms: IMoleScene) = tabbedPane.pages-=sceneTabs.get(ms)
  
  def removeAllSceneTabs = tabbedPane.peer.removeAll
  
  def addTab(scene: IMoleScene,n: String,sp: ScrollPane): IMoleScene = {
    val name= scene.manager.name.getOrElse(n)
    val p = new TabbedPane.Page(name,sp)
    tabbedPane.pages += p
    sceneTabs.put(scene,p)
    scene.manager.name= Some(name)
    scene
  }
  
  def displayBuildMoleScene(displayed: IMoleScene): Unit = {
 //   if (!sceneTabs.containsKey(displayed)) addBuildMoleScene
    tabbedPane.selection.page= sceneTabs.get(displayed)
  }

  def displayExecutionMoleScene(displayed: IMoleScene) = tabbedPane.selection.page= sceneTabs.get(displayed)
}
