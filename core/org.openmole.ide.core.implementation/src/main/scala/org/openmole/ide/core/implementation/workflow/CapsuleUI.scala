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

package org.openmole.ide.core.implementation.workflow

import java.awt.Color
import java.awt.Dimension
import java.awt.Point
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import org.netbeans.api.visual.action.ActionFactory
import org.netbeans.api.visual.widget.ImageWidget
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.implementation.data.CapsuleDataUI
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.implementation.dataproxy.ProxyFreezer
import org.openmole.ide.core.implementation.provider.CapsuleMenuProvider
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.workflow._
import org.openmole.ide.core.model.data.IExplorationTaskDataUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.factory.IHookFactoryUI
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.misc.tools.image.Images
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.misc.widget.LinkLabel
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import org.openmole.core.implementation.validation.DataflowProblem
import scala.swing.Action

class CapsuleUI(val scene: IMoleScene,
                val dataUI: ICapsuleDataUI = new CapsuleDataUI) extends Widget(scene.graphScene) with ICapsuleUI {

  val taskComponentWidget = new TaskComponentWidget(scene, this, new TaskWidget(scene, this))
  var environmentWidget: Option[LinkedImageWidget] = None
  var samplingWidget: Option[LinkedImageWidget] = None
  var inputPrototypeWidget: Option[PrototypeWidget] = None
  var outputPrototypeWidget: Option[PrototypeWidget] = None

  val validationWidget = new ImageWidget(scene.graphScene, dataUI.task match {
    case Some(x: ITaskDataProxyUI) ⇒ Images.CHECK_VALID
    case _ ⇒ Images.CHECK_INVALID
  }) {
    setPreferredLocation(new Point(TASK_CONTAINER_WIDTH - 12, 2))
  }

  setEnvironment(dataUI.environment)
  setSampling(dataUI.sampling)

  addChild(taskComponentWidget)
  addChild(validationWidget)
  setPreferredSize(new Dimension(TASK_CONTAINER_WIDTH + 20, TASK_CONTAINER_HEIGHT + 20))
  taskComponentWidget.setPreferredLocation(new Point(10, 10))
  createActions(MOVE).addAction(ActionFactory.createMoveAction)

  var islots = ListBuffer.empty[IInputSlotWidget]
  val oslot = new OutputSlotWidget(scene, this)
  var nbInputSlots = 0

  addChild(oslot)
  val capsuleMenuProvider = new CapsuleMenuProvider(scene, this)

  getActions.addAction(ActionFactory.createPopupMenuAction(capsuleMenuProvider))

  val titleWidget = new LinkedWidget(scene, new LinkLabel(toString, new Action("") {
    def apply = {
      dataUI.task match {
        case Some(x: ITaskDataProxyUI) ⇒ scene.displayPropertyPanel(x, EDIT)
        case _ ⇒
      }
    }
  }, 6) { preferredSize = new Dimension(TASK_CONTAINER_WIDTH, TASK_TITLE_HEIGHT) }, 10, 10)

  addChild(titleWidget)

  def setAsValid = {
    validationWidget.setImage(Images.CHECK_VALID)
    validationWidget.setToolTipText("Runnable capsule")
  }

  def setAsInvalid(errorString: String) = {
    validationWidget.setImage(Images.CHECK_INVALID)
    validationWidget.setToolTipText(errorString)
  }

  override def paintWidget = {
    super.paintWidget
    dataUI.task match {
      case Some(x: ITaskDataProxyUI) ⇒
        titleWidget.linkLabel.foreground = Color.WHITE
        titleWidget.linkLabel.text = x.dataUI.name
      case None ⇒
    }
  }

  def widget = this

  def copy(sc: IMoleScene) = {
    var slotMapping = new HashMap[IInputSlotWidget, IInputSlotWidget]
    val c = new CapsuleUI(sc)
    islots.foreach(i ⇒ slotMapping += i -> c.addInputSlot(false))
    dataUI.task match {
      case Some(x: ITaskDataProxyUI) ⇒
        c.encapsule(ProxyFreezer.freeze(x))
        if (dataUI.environment.isDefined) c.setEnvironment(ProxyFreezer.freeze(dataUI.environment))
        if (dataUI.sampling.isDefined) c.setSampling(ProxyFreezer.freeze(dataUI.sampling))
      case _ ⇒
    }
    (c, slotMapping)
  }

  def defineAsStartingCapsule(b: Boolean) = {
    dataUI.startingCapsule = b
    islots.foreach { isw ⇒
      isw.setStartingSlot(b)
    }
    scene.validate
    scene.refresh
  }

  def decapsule = {
    dataUI.task = None
    removeChild(inputPrototypeWidget.get)
    removeChild(outputPrototypeWidget.get)
    removeChild(titleWidget)
    inputPrototypeWidget = None
    outputPrototypeWidget = None
  }

  def encapsule(dpu: ITaskDataProxyUI) = {
    setTask(dpu)
    inputPrototypeWidget = Some(PrototypeWidget.buildInput(scene, dpu))
    outputPrototypeWidget = Some(PrototypeWidget.buildOutput(scene, dpu))
    CheckData.checkMole(scene)
    addChild(inputPrototypeWidget.get)
    addChild(outputPrototypeWidget.get)
    capsuleMenuProvider.addTaskMenus
  }

  def setEnvironment(envtask: Option[IEnvironmentDataProxyUI]) = {
    dataUI.environment = envtask
    updateEnvironmentWidget
  }
  //
  //  def hook(hookFactory: IHookFactoryUI, activated: Boolean) = {
  //    if (activated) dataUI.hooks.getOrElseUpdate(hookFactory.coreClass)
  //    println("hook " + hookFactory.toString + " " + activated)
  //  }

  private def updateEnvironmentWidget = {
    environmentWidget match {
      case Some(y: LinkedImageWidget) ⇒ removeChild(y)
      case None ⇒
    }
    dataUI.environment match {
      case Some(x: IEnvironmentDataProxyUI) ⇒
        environmentWidget = Some(new LinkedImageWidget(scene,
          new ImageIcon(ImageIO.read(x.dataUI.getClass.getClassLoader.getResource(x.dataUI.imagePath))),
          TASK_CONTAINER_WIDTH - 10, TASK_CONTAINER_HEIGHT - 3,
          new Action("") { def apply = scene.displayPropertyPanel(x, EDIT) }))
        addChild(environmentWidget.get)
      case None ⇒ environmentWidget = None
    }
    scene.refresh
  }

  def setSampling(sampletask: Option[ISamplingDataProxyUI]) = {
    dataUI.sampling = sampletask
    updateSamplingWidget
  }

  private def updateSamplingWidget = {
    samplingWidget match {
      case Some(y: LinkedImageWidget) ⇒ removeChild(y)
      case None ⇒
    }

    dataUI.sampling match {
      case Some(x: ISamplingDataProxyUI) ⇒
        samplingWidget = Some(new LinkedImageWidget(scene,
          new ImageIcon(ImageIO.read(x.dataUI.getClass.getClassLoader.getResource(x.dataUI.imagePath))),
          0, TASK_CONTAINER_HEIGHT - 3,
          new Action("") { def apply = scene.displayPropertyPanel(x, EDIT) }))
        addChild(samplingWidget.get)
      case _ ⇒ samplingWidget = None
    }
    scene.refresh
  }

  def updateErrors(problems: Iterable[DataflowProblem]) = {
    dataUI.task match {
      case Some(x: ITaskDataProxyUI) ⇒
        inputPrototypeWidget match {
          case Some(x: PrototypeWidget) ⇒ x.updateErrors(problems.mkString("\n"))
          case _ ⇒
        }
      case _ ⇒
    }
    if (problems.size > 0) setAsInvalid("I / O errors")
  }

  def addInputSlot(on: Boolean): IInputSlotWidget = {
    if (on) dataUI.startingCapsule = on
    nbInputSlots += 1
    val im = new InputSlotWidget(scene, this, nbInputSlots, on)
    islots += im
    addChild(im)
    scene.refresh
    im
  }

  def removeInputSlot = {
    nbInputSlots -= 1
    val toBeRemoved = islots.tail.last
    removeChild(toBeRemoved.widget)
    islots -= toBeRemoved
  }

  def setTask(dpu: ITaskDataProxyUI) = {
    dataUI.task = Some(dpu)
    dpu.dataUI match {
      case x: IExplorationTaskDataUI ⇒ setSampling(x.sampling)
      case _ ⇒
    }
  }

  def x = convertLocalToScene(getLocation).getX

  def y = convertLocalToScene(getLocation).getY
}
