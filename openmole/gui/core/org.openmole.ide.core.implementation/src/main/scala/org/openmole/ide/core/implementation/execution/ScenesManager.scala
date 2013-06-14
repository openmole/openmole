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

package org.openmole.ide.core.implementation.execution

import org.openmole.ide.core.implementation.workflow._
import java.awt.Color
import java.awt.Dimension
import java.awt.Point
import java.util.concurrent.atomic.AtomicInteger
import org.openmole.ide.core.model.data.IExplorationTaskDataUI
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.model.workflow._
import org.openmole.ide.core.model.dataproxy.{ IDataProxyUI, ITaskDataProxyUI }
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.ide.misc.tools.util._
import org.openmole.ide.misc.tools.image.Images._
import scala.swing.Action
import scala.swing.Button
import scala.swing.Label
import scala.swing.TabbedPane
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.model.panel.ISamplingCompositionPanelUI
import org.openmole.ide.core.implementation.builder.SceneFactory
import util.{ Failure, Success }
import concurrent.stm._
import util.Failure
import scala.Some
import util.Success
import util.Failure
import scala.Some
import util.Success
import swing.event.ButtonClicked

object ScenesManager {

  val tabPane = new TabbedPane

  var countBuild = new AtomicInteger
  var countExec = new AtomicInteger
  val _selection: Ref[Option[List[ICapsuleUI]]] = Ref(None)

  def invalidateSelection = _selection.single() = None

  def invalidateMoles = moleScenes.foreach {
    _.manager.invalidateCache
  }

  def selection = atomic {
    implicit actx ⇒
      _selection() match {
        case Some(_) ⇒
        case None ⇒
          _selection() = Some(capsules.filter {
            _.selected
          })
      }
      _selection().get
  }

  PasswordListner.apply

  def isInSelection(capsule: ICapsuleUI) = selection.contains(capsule)

  def buildMoleSceneContainers = tabPane.pages.flatMap(_.content match {
    case x: BuildMoleSceneContainer ⇒ List(x)
    case _                          ⇒ Nil
  })

  def currentSceneContainer: Option[ISceneContainer] = {
    if (tabPane.peer.getTabCount == 0) None
    else tabPane.selection.page.content match {
      case x: ISceneContainer ⇒ Some(x)
      case _                  ⇒ None
    }
  }

  def currentScene = currentSceneContainer match {
    case Some(sc: ISceneContainer) ⇒ Some(sc.scene)
    case _                         ⇒ None
  }

  def displayExtraPropertyPanel(proxy: IDataProxyUI) = {
    currentScene.getOrElse(addBuildSceneContainer("Mole").scene).displayPropertyPanel(proxy, 1)
  }

  def currentPanelUI = currentScene match {
    case Some(s: IMoleScene) ⇒ s.currentPanelUI
    case _                   ⇒ throw new UserBadDataError("There is no current scene")
  }

  def currentSamplingCompositionPanelUI = currentPanelUI match {
    case scp: ISamplingCompositionPanelUI ⇒ scp
    case _                                ⇒ throw new UserBadDataError("There is no current samplingMap panel")
  }

  def closePropertyPanel = List(currentScene).flatten.foreach {
    _.closePropertyPanels
  }

  def changeSelection(widget: ICapsuleUI) = {
    widget.selected = !widget.selected
    invalidateSelection
  }

  def addToSelection(widget: ICapsuleUI) = {
    widget.selected = true
    invalidateSelection
  }

  def removeFromSelection(widget: ICapsuleUI) = {
    widget.selected = false
    invalidateSelection
  }

  def clearSelection = {
    selection.foreach {
      _.selected = false
    }
    invalidateSelection
  }

  def pasteCapsules(ms: IBuildMoleScene,
                    point: Point) = {
    val copied = selection.map {
      z ⇒
        z -> z.copy(ms)
    }.toMap

    val dx = (point.x - selection.map {
      _.widget.getPreferredLocation.x
    }.min).toInt
    val dy = (point.y - selection.map {
      _.widget.getPreferredLocation.y
    }.min).toInt

    copied.foreach {
      case (old, neo) ⇒
        val p = new Point((old.widget.getPreferredLocation.x + dx).toInt, (old.widget.getPreferredLocation.y + dy).toInt)
        ms.add(neo._1, p)
        neo._1.environment_=(old.dataUI.environment)
        old.dataUI.task match {
          case Some(t: ITaskDataProxyUI) ⇒ neo._1.encapsule(t)
          case _                         ⇒
        }
        ms.refresh
      case _ ⇒
    }

    val islots = selection.flatMap {
      _.islots
    }
    selection.headOption match {
      case Some(c: ICapsuleUI) ⇒
        val connectors = c.scene.manager.connectors.values.toList
        connectors.foreach {
          con ⇒
            if (selection.contains(con.source) && islots.contains(con.target)) {
              con match {
                case (t: ITransitionUI) ⇒
                  val transition = new TransitionUI(
                    copied(t.source)._1,
                    copied(t.target.capsule)._1.islots.find {
                      s ⇒ t.target.index == s.index
                    }.get,
                    t.transitionType,
                    t.condition,
                    t.filteredPrototypes)
                  ms.add(transition)
                case (t: IDataChannelUI) ⇒
                  val dc = new DataChannelUI(
                    copied(t.source)._1,
                    copied(t.target.capsule)._1.islots.find {
                      s ⇒ t.target.index == s.index
                    }.get,
                    t.filteredPrototypes)
                  ms.add(dc)
              }
            }
        }
        ms.refresh
      case _ ⇒
    }
  }

  def closeAll = tabPane.pages.clear

  def saveCurrentPropertyWidget = currentSceneContainer match {
    case Some(x: ISceneContainer) ⇒ x.scene.savePropertyPanel(0)
    case _                        ⇒ None
  }

  def moleScenes = buildMoleSceneContainers.map {
    _.scene
  }

  def capsules: List[ICapsuleUI] = moleScenes.map {
    _.manager.capsules.values
  }.toList.flatten

  def capsules(p: ITaskDataProxyUI) = moleScenes.flatMap {
    _.manager.capsules.values
  }.filter {
    _.dataUI.task.isDefined
  }.filter {
    p == _.dataUI.task.get
  }

  def explorationCapsules = moleScenes.flatMap {
    _.manager.capsules.values
  }.filter {
    _.dataUI.task.isDefined
  }.flatMap {
    c ⇒
      c.dataUI.task.get.dataUI match {
        case x: IExplorationTaskDataUI ⇒ List((c, x))
        case _                         ⇒ Nil
      }
  }.toList

  def transitions = moleScenes.flatMap {
    _.manager.connectors.values
  }.flatMap {
    _ match {
      case t: ITransitionUI ⇒ Some(t)
      case _                ⇒ None
    }
  }
    .toList

  def addBuildSceneContainer: BuildMoleSceneContainer = addBuildSceneContainer(BuildMoleScene(""))

  def addBuildSceneContainer(name: String): BuildMoleSceneContainer = addBuildSceneContainer(BuildMoleScene(name))

  def addBuildSceneContainer(ms: BuildMoleScene): BuildMoleSceneContainer = {
    val container = new BuildMoleSceneContainer(ms)
    val page = new TabbedPane.Page(ms.manager.name, container)
    addTab(page, ms.manager.name, new Action("") {
      def apply = {
        tabPane.pages.remove(page.index)
        // container.stopAndCloseExecutions
      }
    })
    container
  }

  def addExecutionSceneContainer(bmsc: BuildMoleSceneContainer) =
    CheckData.fullCheck(bmsc.scene) match {
      case Success(_) ⇒
        if (StatusBar().isValid) {
          val clone = bmsc.scene.copyScene
          clone.manager.name = {
            bmsc.scene.manager.name + "_" + countExec.incrementAndGet
          }
          val page = new TabbedPane.Page(clone.manager.name, new MigPanel(""))
          val container = new ExecutionMoleSceneContainer(clone, page, bmsc)
          page.content = container

          addTab(page, clone.manager.name, new Action("") {
            def apply = {
              container.stop
              tabPane.pages.remove(page.index)
            }
          })

          tabPane.selection.index = page.index
        }
        else
          StatusBar().block("The Mole can not be built due to the previous errors")
      case Failure(t: Throwable) ⇒ StatusBar().block(t.getMessage)
    }

  def addTab(page: TabbedPane.Page, title: String, action: Action) = {
    tabPane.pages += page
    tabPane.peer.setTabComponentAt(tabPane.peer.getTabCount - 1, new CloseableTab(title, action).peer)
    tabPane.selection.page = page
  }

  class CloseableTab(title: String,
                     action: Action) extends MigPanel("") {
    background = new Color(0, 0, 0, 0)
    contents += new Label(title)
    contents += new Button(action) {
      preferredSize = new Dimension(20, 20)
      maximumSize = new Dimension(20, 20)
      icon = CLOSE_TAB
    }
  }
}