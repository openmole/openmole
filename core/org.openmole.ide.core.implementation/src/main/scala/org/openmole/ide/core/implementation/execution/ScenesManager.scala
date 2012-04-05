/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.execution

import org.openmole.ide.core.implementation.workflow.BuildMoleSceneContainer
import org.openmole.ide.core.implementation.workflow.ExecutionMoleSceneContainer
import java.util.concurrent.atomic.AtomicInteger
import org.openmole.ide.core.implementation.data.AbstractExplorationTaskDataUI
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.implementation.workflow.BuildMoleScene
import org.openmole.ide.core.model.dataproxy.ITaskDataProxyUI
import org.openmole.ide.core.model.workflow.ICapsuleUI
import org.openmole.ide.core.model.workflow.ISceneContainer
import org.openmole.ide.misc.widget.MigPanel
import scala.collection.JavaConversions._
import scala.swing.TabbedPane

object ScenesManager {

  val tabPane = new TabbedPane
  
  var countBuild = new AtomicInteger  
  var countExec = new AtomicInteger  
  var connectMode = true
  PasswordListner.apply
  
  def buildMoleSceneContainers = tabPane.pages.flatMap(_.content match {
      case x : BuildMoleSceneContainer => List(x)
      case _ => Nil
    }
  )
  
  def currentSceneContainer : Option[ISceneContainer] =  tabPane.selection.page match {
      case x : ISceneContainer => Some(x)
      case _ => None
  }
  
  def closeAll = tabPane.pages.clear
  
  def saveCurrentPropertyWidget = currentSceneContainer match {
      case Some(x : ISceneContainer) => x.scene.savePropertyPanel
      case _ => None
  }
  
  def moleScenes = buildMoleSceneContainers.map{_.scene}
  
  def capsules : List[ICapsuleUI] = moleScenes.map{_.manager.capsules.values}.toList.flatten
  
  def capsules(p : ITaskDataProxyUI) = moleScenes.flatMap{
    _.manager.capsules.values
  }.filter{
    _.dataUI.task.isDefined
  }.filter{
    p == _.dataUI.task.get
  }
  
  def explorationCapsules = moleScenes.flatMap{
    _.manager.capsules.values
  }.filter{
    _.dataUI.task.isDefined
  }.flatMap{c => c.dataUI.task.get.dataUI match {
      case x : AbstractExplorationTaskDataUI => List((c,x))
      case _ => Nil
    }
  }.toList
    
  def addBuildSceneContainer : BuildMoleSceneContainer = addBuildSceneContainer(new BuildMoleScene(""))
  
  def addBuildSceneContainer(name: String) : BuildMoleSceneContainer = addBuildSceneContainer(new BuildMoleScene(name))
  
  def addBuildSceneContainer(ms: BuildMoleScene) : BuildMoleSceneContainer = {
    val container = new BuildMoleSceneContainer(ms)
    tabPane.pages += new TabbedPane.Page(ms.manager.name,container)
    container
  }
  
  def addExecutionSceneContainer(bmsc : BuildMoleSceneContainer) = {
    CheckData.fullCheck(bmsc.scene.manager)
    val clone = bmsc.scene.copy
    clone.manager.name = { bmsc.scene.manager.name+"_"+countExec.incrementAndGet }
    val page = new TabbedPane.Page("Settings",new MigPanel(""))
    val container = new ExecutionMoleSceneContainer(clone,page)
    page.content = container
    bmsc.executionMoleSceneContainers += container
  }
}