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
import org.openmole.ide.core.implementation.dialog.MasterCapsulePrototypeDialog
import org.openmole.ide.core.implementation.data.CapsuleDataUI
import org.openmole.ide.core.implementation.data.CheckData
import org.openmole.ide.core.implementation.dataproxy.ProxyFreezer
import org.openmole.ide.core.implementation.provider.CapsuleMenuProvider
import org.openmole.ide.core.model.commons._
import org.openmole.ide.core.model.commons.Constants._
import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.workflow._
import org.openmole.ide.core.model.data.IExplorationTaskDataUI
import org.openmole.ide.core.model.workflow.IMoleScene
import org.openmole.ide.misc.tools.image.Images
import org.openmole.ide.misc.widget.LinkLabel
import scala.collection.mutable.HashMap
import scala.collection.mutable.ListBuffer
import org.openmole.core.implementation.validation.DataflowProblem
import scala.swing.Action
import org.openmole.core.model.mole.{ ICapsule, IMole }
import org.openmole.core.model.data.Prototype
import org.openmole.ide.core.implementation.builder.SceneFactory
import org.openmole.ide.misc.tools.util._

object CapsuleUI {
  def imageWidget(scene: IMoleScene, img: ImageIcon, x: Int, y: Int, action: Action) = new LinkedImageWidget(scene, img, x, y, action)
  def withMenu(ms: IBuildMoleScene, dataUI: ICapsuleDataUI = new CapsuleDataUI) = {
    val capsuleUI = CapsuleUI(ms, dataUI)
    val capsuleMenuProvider = new CapsuleMenuProvider(ms, capsuleUI)
    capsuleUI.getActions.addAction(ActionFactory.createPopupMenuAction(capsuleMenuProvider))
    capsuleUI
  }

  def apply(
    scene: IMoleScene,
    dataUI: ICapsuleDataUI = new CapsuleDataUI) = {
    val caps = new CapsuleUI(scene, dataUI)
    dataUI.task match {
      case Some(t: ITaskDataProxyUI) ⇒
        caps.encapsule(t)
        caps.setAsValid
      case _ ⇒
        caps.decapsule
        caps.setAsInvalid("Empty Capsule")
    }
    caps
  }

}

import CapsuleUI._

class CapsuleUI private (
    val scene: IMoleScene,
    var dataUI: ICapsuleDataUI = new CapsuleDataUI) extends Widget(scene.graphScene) with ICapsuleUI with ID { capsuleUI ⇒

  var capsuleTypeWidget: Option[LinkedImageWidget] = None
  var environmentWidget: Option[LinkedImageWidget] = None
  var samplingWidget: Option[LinkedImageWidget] = None
  var inputPrototypeWidget: Option[PrototypeWidget] = None
  var outputPrototypeWidget: Option[PrototypeWidget] = None
  var selected = false
  val islots = ListBuffer.empty[IInputSlotWidget]
  val oslot = new OutputSlotWidget(scene)

  val taskComponentWidget = new SceneComponentWidget(
    scene,
    new TaskWidget(scene, this),
    TASK_CONTAINER_WIDTH,
    TASK_CONTAINER_HEIGHT)

  taskComponentWidget.setPreferredLocation(new Point(10, 10))

  val titleWidget = new LinkedWidget(scene, new LinkLabel(toString, new Action("") {
    def apply = {
      dataUI.task match {
        case Some(x: ITaskDataProxyUI) ⇒ scene.displayPropertyPanel(x, 0)
        case _ ⇒
      }
    }
  }, 6) {
    preferredSize = new Dimension(TASK_CONTAINER_WIDTH, TASK_TITLE_HEIGHT)
  }, 10, 10)

  setPreferredSize(new Dimension(TASK_CONTAINER_WIDTH + 20, TASK_CONTAINER_HEIGHT + 20))
  createActions(MOVE).addAction(ActionFactory.createMoveAction)

  val validationWidget = new ImageWidget(scene.graphScene, Images.CHECK_INVALID) {
    setPreferredLocation(new Point(TASK_CONTAINER_WIDTH - 12, 2))
  }

  addChild(taskComponentWidget)
  addChild(titleWidget)
  addChild(oslot)
  addChild(validationWidget)
  updateEnvironmentWidget
  updateSamplingWidget

  def nbInputSlots: Int = islots.size

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

  def copy(sc: IBuildMoleScene) = {
    val c = CapsuleUI.withMenu(sc)
    val slotMapping = islots.map(i ⇒ i -> c.addInputSlot).toMap
    (c, slotMapping)
  }

  def starting = scene.manager.startingCapsule.map(_ == this).getOrElse(false)

  def defineAsStartingCapsule = {
    scene.manager.startingCapsule = Some(this)
    scene.refresh
  }

  def update = {
    islots.foreach(_.refresh)
  }

  def decapsule = {
    dataUI = dataUI.copy(task = None)
    removeWidget(inputPrototypeWidget)
    removeWidget(outputPrototypeWidget)
    removeWidget(samplingWidget)
    samplingWidget = None
    titleWidget.linkLabel.text = ""
    inputPrototypeWidget = Some(PrototypeWidget.buildNoTaskSource(scene, this))
    addChild(inputPrototypeWidget.get)
    outputPrototypeWidget = Some(PrototypeWidget.buildNoTaskHook(scene, this))
    addChild(outputPrototypeWidget.get)
    scene.refresh
  }

  def encapsule(dpu: ITaskDataProxyUI) = {
    decapsule
    dataUI = dataUI.copy(task = Some(dpu))
    inputPrototypeWidget = Some(PrototypeWidget.buildTaskSource(scene, this))
    outputPrototypeWidget = Some(PrototypeWidget.buildTaskHook(scene, this))
    CheckData.checkMole(scene)
    addChild(inputPrototypeWidget.get)
    addChild(outputPrototypeWidget.get)
    scene.refresh
  }

  def environment_=(env: Option[IEnvironmentDataProxyUI]) = {
    dataUI = dataUI.copy(environment = env)
    updateEnvironmentWidget
  }

  private def removeWidget(w: Option[ComponentWidget]) = {
    w match {
      case Some(y: ComponentWidget) ⇒ removeChild(y)
      case None ⇒
    }
  }

  def capsuleType_=(cType: CapsuleType) = {
    dataUI = dataUI.copy(capsuleType = cType)
    updateCapsuleTypeWidget
  }

  private def updateCapsuleTypeWidget = {
    removeWidget(capsuleTypeWidget)

    dataUI.capsuleType match {
      case x: BasicCapsuleType ⇒ capsuleTypeWidget = None
      case x: CapsuleType ⇒
        capsuleTypeWidget = Some(imageWidget(scene,
          new ImageIcon(ImageIO.read(dataUI.getClass.getClassLoader.getResource("img/" + x.toString.toLowerCase + "Capsule.png"))),
          -1, -1, new Action("") {
            def apply = MasterCapsulePrototypeDialog.display(capsuleUI)
          }))
        addChild(capsuleTypeWidget.get)
    }
    scene.refresh
  }

  private def updateEnvironmentWidget = {
    removeWidget(environmentWidget)

    val img = dataUI.environment match {
      case Some(x: IEnvironmentDataProxyUI) ⇒ new ImageIcon(ImageIO.read(x.dataUI.getClass.getClassLoader.getResource(x.dataUI.imagePath)))
      case _ ⇒ new ImageIcon(ImageIO.read(dataUI.getClass.getClassLoader.getResource("img/noEnv.png")))
    }

    environmentWidget = Some(imageWidget(scene,
      img, TASK_CONTAINER_WIDTH - 10, TASK_CONTAINER_HEIGHT - 3,
      new Action("") {
        def apply = scene.displayCapsuleProperty(capsuleUI, 2)
      }))
    addChild(environmentWidget.get)
  }

  def updateSamplingWidget = {
    removeWidget(samplingWidget)
    dataUI.task match {
      case Some(t: ITaskDataProxyUI) ⇒
        t.dataUI match {
          case d: IExplorationTaskDataUI ⇒ d.sampling match {
            case Some(x: ISamplingCompositionDataProxyUI) ⇒
              samplingWidget = Some(new LinkedImageWidget(scene,
                new ImageIcon(ImageIO.read(x.dataUI.getClass.getClassLoader.getResource(x.dataUI.imagePath))),
                0, TASK_CONTAINER_HEIGHT - 3,
                new Action("") {
                  def apply = scene.displayPropertyPanel(x, 0)
                }))
              addChild(samplingWidget.get)
            case _ ⇒ samplingWidget = None
          }
          case _ ⇒
        }
      case _ ⇒
    }
    scene.refresh
  }

  def updateErrors(problems: Iterable[DataflowProblem]) = {
    dataUI.task match {
      case Some(x: ITaskDataProxyUI) ⇒
        inputPrototypeWidget match {
          case Some(x: PrototypeWidget) ⇒
            x.updateErrors(problems.mkString("\n"))
          case _ ⇒
        }
        outputPrototypeWidget match {
          case Some(x: PrototypeWidget) ⇒
            x.updateErrors(problems.mkString("\n"))
          case _ ⇒
        }
      case _ ⇒
    }
    if (problems.size > 0) setAsInvalid("I / O errors")
  }

  def addInputSlot: IInputSlotWidget = {
    val im = new InputSlotWidget(scene, this, nbInputSlots)
    islots += im
    addChild(im)
    scene.refresh
    im
  }

  def removeInputSlot = {
    val toBeRemoved = islots.tail.last
    removeChild(toBeRemoved.widget)
    islots -= toBeRemoved
    scene.refresh
  }

  def inputs(mole: IMole, cMap: Map[ICapsuleUI, ICapsule], pMap: Map[IPrototypeDataProxyUI, Prototype[_]]): List[IPrototypeDataProxyUI] = {
    if (cMap.contains(this)) {
      val caps = cMap(this)
      caps.inputs(mole, Map(caps -> dataUI.sources.map { _.dataUI.coreObject(pMap) }), Map(caps -> dataUI.hooks.map { _.dataUI.coreObject(pMap) })).toList.map {
        ds ⇒
          SceneFactory.prototype(ds.prototype)
      }
    } else List()
  }

  def outputs(mole: IMole, cMap: Map[ICapsuleUI, ICapsule], pMap: Map[IPrototypeDataProxyUI, Prototype[_]]): List[IPrototypeDataProxyUI] =
    if (cMap.contains(this)) {
      val caps = cMap(this)
      caps.outputs(mole, Map(caps -> dataUI.sources.map { _.dataUI.coreObject(pMap) }), Map(caps -> dataUI.hooks.map { _.dataUI.coreObject(pMap) })).toList.map {
        ds ⇒ SceneFactory.prototype(ds.prototype)
      }
    } else List()

  def inputs: List[IPrototypeDataProxyUI] = {
    val i = scene.manager.cacheMole match {
      case Some((m: IMole, cMap: Map[ICapsuleUI, ICapsule], pMap: Map[IPrototypeDataProxyUI, Prototype[_]])) ⇒
        inputs(m, cMap, pMap)
      case _ ⇒ List()
    }
    i
  }

  def outputs: List[IPrototypeDataProxyUI] = scene.manager.cacheMole match {
    case Some((m: IMole, cMap: Map[ICapsuleUI, ICapsule], pMap: Map[IPrototypeDataProxyUI, Prototype[_]])) ⇒
      outputs(m, cMap, pMap)
    case _ ⇒ List()
  }

  def x = location.x
  def y = location.y
  def location = getLocation
}
