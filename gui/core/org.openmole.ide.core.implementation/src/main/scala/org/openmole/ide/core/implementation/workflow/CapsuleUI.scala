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

package org.openmole.ide.core.implementation.workflow

import java.awt.Color
import java.awt.Dimension
import java.awt.Point
import javax.imageio.ImageIO
import javax.swing.ImageIcon
import org.netbeans.api.visual.action.ActionFactory
import org.netbeans.api.visual.widget.ComponentWidget
import org.netbeans.api.visual.widget.ImageWidget
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.implementation.dialog.{ StatusBar, MasterCapsulePrototypeDialog }
import org.openmole.ide.core.implementation.data.CapsuleDataUI
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.implementation.dataproxy.ProxyFreezer
import org.openmole.ide.core.implementation.provider.CapsuleMenuProvider
import org.openmole.ide.core.model.commons._
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.workflow.IInputSlotWidget
import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.workflow._
import org.openmole.ide.core.model.data.IExplorationTaskDataUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.misc.tools.image.Images
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.misc.widget.LinkLabel
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import org.openmole.core.implementation.validation.DataflowProblem
import scala.swing.Action
import org.openmole.ide.core.implementation.builder.{ SceneFactory, MoleFactory }
import org.openmole.core.model.mole.{ Hooks, Sources, ICapsule, IMole }
import org.openmole.core.model.data.{ Prototype, DataSet }
import org.openmole.ide.core.implementation.registry.KeyPrototypeGenerator

class CapsuleUI(val scene: IMoleScene,
                val dataUI: ICapsuleDataUI = new CapsuleDataUI) extends Widget(scene.graphScene)
    with ICapsuleUI {
  capsuleUI ⇒
  val taskComponentWidget = new SceneComponentWidget(scene, new TaskWidget(scene, this),
    TASK_CONTAINER_WIDTH,
    TASK_CONTAINER_HEIGHT)
  var capsuleTypeWidget: Option[LinkedImageWidget] = None
  var environmentWidget: Option[LinkedImageWidget] = None
  var samplingWidget: Option[LinkedImageWidget] = None
  var inputPrototypeWidget: Option[PrototypeWidget] = None
  var outputPrototypeWidget: Option[PrototypeWidget] = None
  var selected = false

  val capsuleMenuProvider = new CapsuleMenuProvider(scene, this)

  addChild(taskComponentWidget)

  setEnvironment(dataUI.environment)
  setSampling(dataUI.sampling)
  setCapsuleType(dataUI.capsuleType)

  val validationWidget = new ImageWidget(scene.graphScene,
    dataUI.task match {
      case Some(t: ITaskDataProxyUI) ⇒
        encapsule(t)
        Images.CHECK_VALID
      case _ ⇒
        decapsule
        Images.CHECK_INVALID
    }) {
    setPreferredLocation(new Point(TASK_CONTAINER_WIDTH - 12, 2))
  }

  addChild(validationWidget)

  setPreferredSize(new Dimension(TASK_CONTAINER_WIDTH + 20, TASK_CONTAINER_HEIGHT + 20))
  taskComponentWidget.setPreferredLocation(new Point(10, 10))
  createActions(MOVE).addAction(ActionFactory.createMoveAction)

  var islots = ListBuffer.empty[IInputSlotWidget]
  val oslot = new OutputSlotWidget(scene, this)
  var nbInputSlots = 0

  addChild(oslot)

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

  def hooked(b: Boolean) = outputPrototypeWidget match {
    case Some(w: PrototypeWidget) ⇒ w.hooked = b
    case _ ⇒
  }

  def widget = this

  def deepcopy(sc: IMoleScene) = {
    val ret = copy(sc)
    dataUI.task match {
      case Some(x: ITaskDataProxyUI) ⇒
        ret._1.encapsule(ProxyFreezer.freeze(x))
        if (dataUI.environment.isDefined) ret._1.setEnvironment(ProxyFreezer.freeze(dataUI.environment))
        if (dataUI.sampling.isDefined) ret._1.setSampling(ProxyFreezer.freeze(dataUI.sampling))
      case _ ⇒
    }
    ret
  }

  def copy(sc: IMoleScene) = {
    var slotMapping = new HashMap[IInputSlotWidget, IInputSlotWidget]
    val c = new CapsuleUI(sc)
    islots.foreach(i ⇒ slotMapping += i -> c.addInputSlot(false))
    (c, slotMapping)
  }

  def defineAsStartingCapsule(b: Boolean) = {
    islots.foreach {
      _.setStartingSlot(b)
    }
    scene.validate
    scene.refresh
  }

  def decapsule = {
    dataUI.task = None
    dataUI.unhookAll
    removeWidget(inputPrototypeWidget)
    removeWidget(outputPrototypeWidget)
    removeWidget(environmentWidget)
    removeWidget(samplingWidget)
    environmentWidget = None
    samplingWidget = None
    inputPrototypeWidget = Some(PrototypeWidget.buildEmptySource(scene))
    addChild(inputPrototypeWidget.get)
    outputPrototypeWidget = Some(PrototypeWidget.buildEmptyHook(scene))
    addChild(outputPrototypeWidget.get)
  }

  def encapsule(dpu: ITaskDataProxyUI) = {
    decapsule
    setTask(dpu)
    inputPrototypeWidget = Some(PrototypeWidget.buildInput(scene, this))
    outputPrototypeWidget = Some(PrototypeWidget.buildOutput(scene, this))
    CheckData.checkMole(scene)
    addChild(inputPrototypeWidget.get)
    addChild(outputPrototypeWidget.get)
  }

  def setEnvironment(envtask: Option[IEnvironmentDataProxyUI]) = {
    dataUI.environment = envtask
    updateEnvironmentWidget
  }

  private def removeWidget(w: Option[ComponentWidget]) = {
    w match {
      case Some(y: ComponentWidget) ⇒
        removeChild(y)
      case None ⇒
    }
  }

  def setCapsuleType(cType: CapsuleType) = {
    dataUI.capsuleType = cType
    updateCapsuleTypeWidget
  }

  private def updateCapsuleTypeWidget = {
    removeWidget(capsuleTypeWidget)

    dataUI.capsuleType match {
      case x: BasicCapsuleType ⇒ capsuleTypeWidget = None
      case x: CapsuleType ⇒
        capsuleTypeWidget = Some(new LinkedImageWidget(scene,
          new ImageIcon(ImageIO.read(dataUI.getClass.getClassLoader.getResource("img/" + x.toString.toLowerCase + "Capsule.png"))),
          -1, -1,
          new Action("") {
            def apply = MasterCapsulePrototypeDialog.display(capsuleUI)
          }))
        addChild(capsuleTypeWidget.get)
    }
  }

  private def updateEnvironmentWidget = {
    removeWidget(environmentWidget)

    dataUI.environment match {
      case Some(x: IEnvironmentDataProxyUI) ⇒
        environmentWidget = Some(new LinkedImageWidget(scene,
          new ImageIcon(ImageIO.read(x.dataUI.getClass.getClassLoader.getResource(x.dataUI.imagePath))),
          TASK_CONTAINER_WIDTH - 10, TASK_CONTAINER_HEIGHT - 3,
          new Action("") {
            def apply = scene.displayPropertyPanel(x, EDIT)
          }))
        addChild(environmentWidget.get)
      case None ⇒ environmentWidget = None
    }
    scene.refresh
  }

  def setSampling(sampletask: Option[ISamplingCompositionDataProxyUI]) = {
    dataUI.sampling = sampletask
    updateSamplingWidget
  }

  private def updateSamplingWidget = {
    removeWidget(samplingWidget)

    dataUI.sampling match {
      case Some(x: ISamplingCompositionDataProxyUI) ⇒
        samplingWidget = Some(new LinkedImageWidget(scene,
          new ImageIcon(ImageIO.read(x.dataUI.getClass.getClassLoader.getResource(x.dataUI.imagePath))),
          0, TASK_CONTAINER_HEIGHT - 3,
          new Action("") {
            def apply = scene.displayPropertyPanel(x, EDIT)
          }))
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
    dataUI.environment = None
    dataUI.sampling = None
    dpu.dataUI match {
      case x: IExplorationTaskDataUI ⇒ setSampling(x.sampling)
      case _ ⇒
    }
  }

  def inputs(mole: IMole, cMap: Map[ICapsuleUI, ICapsule]): List[IPrototypeDataProxyUI] =
    cMap(this).inputs(mole, Sources.empty, Hooks.empty).toList.map {
      ds ⇒ SceneFactory.prototype(ds.prototype)
    }

  def outputs(mole: IMole, cMap: Map[ICapsuleUI, ICapsule]): List[IPrototypeDataProxyUI] =
    cMap(this).outputs(mole, Sources.empty, Hooks.empty).toList.map {
      ds ⇒ SceneFactory.prototype(ds.prototype)
    }

  def inputs: List[IPrototypeDataProxyUI] = scene.manager.cacheMole match {
    case Some((m: IMole, cMap: Map[ICapsuleUI, ICapsule])) ⇒ inputs(m, cMap)
    case _ ⇒ List()
  }

  def outputs: List[IPrototypeDataProxyUI] = scene.manager.cacheMole match {
    case Some((m: IMole, cMap: Map[ICapsuleUI, ICapsule])) ⇒ outputs(m, cMap)
    case _ ⇒ List()
  }

  def x = convertLocalToScene(getLocation).getX

  def y = convertLocalToScene(getLocation).getY

  def location = getLocation
}
