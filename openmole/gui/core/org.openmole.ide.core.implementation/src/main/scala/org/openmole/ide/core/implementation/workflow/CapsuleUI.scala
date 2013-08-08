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
import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.implementation.dialog.MasterCapsulePrototypeDialog
import org.openmole.ide.core.implementation.data.{ IExplorationTaskDataUI, CapsuleDataUI, CheckData }
import org.openmole.ide.core.implementation.provider.CapsuleMenuProvider
import org.openmole.ide.core.implementation.commons.{ StrainerCapsuleType, SimpleCapsuleType, CapsuleType, Constants }
import scala.collection.mutable.ListBuffer
import org.openmole.core.implementation.validation.DataflowProblem
import scala.swing.{ Label, Action }
import scala.swing.Alignment._
import org.openmole.core.model.mole._
import org.openmole.ide.misc.tools.util._
import org.openmole.ide.misc.tools.image.Images
import org.openmole.ide.core.implementation.dataproxy._
import org.openmole.ide.core.implementation.registry.PrototypeKey
import org.openmole.ide.core.implementation.commons.Constants._
import scala.Some

object CapsuleUI {
  def imageWidget(scene: MoleScene, img: ImageIcon, x: Int, y: Int, action: Action) = new LinkedImageWidget(scene, img, x, y, action)
  def withMenu(ms: BuildMoleScene, dataUI: CapsuleDataUI = CapsuleDataUI()) = {
    val capsuleUI = CapsuleUI(ms, dataUI)
    val capsuleMenuProvider = new CapsuleMenuProvider(ms, capsuleUI)
    capsuleUI.getActions.addAction(ActionFactory.createPopupMenuAction(capsuleMenuProvider))
    capsuleUI
  }

  def apply(
    scene: MoleScene,
    dataUI: CapsuleDataUI = CapsuleDataUI()) = {
    val caps = new CapsuleUI(scene, dataUI)
    dataUI.task match {
      case Some(t: TaskDataProxyUI) ⇒
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
    val scene: MoleScene,
    var dataUI: CapsuleDataUI = CapsuleDataUI()) extends Widget(scene.graphScene) with ID { capsuleUI ⇒

  override def toString = dataUI.task match {
    case Some(x: TaskDataProxyUI) ⇒ x.dataUI.toString
    case _                        ⇒ ""
  }

  var capsuleTypeWidget: Option[LinkedImageWidget] = None
  var environmentWidget: Option[LinkedImageWidget] = None
  var samplingWidget: Option[LinkedImageWidget] = None
  var inputPrototypeWidget: Option[PrototypeWidget] = None
  var outputPrototypeWidget: Option[PrototypeWidget] = None
  var selected = false
  val inputSlots = ListBuffer.empty[InputSlotWidget]
  val oslot = new OutputSlotWidget(scene)

  val taskComponentWidget = new SceneComponentWidget(
    scene,
    new TaskWidget(scene, this),
    TASK_CONTAINER_WIDTH,
    TASK_CONTAINER_HEIGHT)

  taskComponentWidget.setPreferredLocation(new Point(10, 10))

  val titleLabel = new Label(toString) {
    preferredSize = new Dimension(TASK_CONTAINER_WIDTH, TASK_TITLE_HEIGHT)
    xAlignment = Left
  }

  val titleWidget = new ComponentWidget(scene.graphScene, titleLabel.peer) {
    setPreferredLocation(new Point(33, 10))
  }

  setPreferredSize(new Dimension(TASK_CONTAINER_WIDTH + 20, TASK_CONTAINER_HEIGHT + 20))
  createActions(MOVE).addAction(ActionFactory.createMoveAction)

  private var _isValid = false

  addChild(taskComponentWidget)
  addChild(titleWidget)
  addChild(oslot)

  def nbInputSlots: Int = inputSlots.size

  def setAsValid = {
    _isValid = true
    updatePrototypeWidgets("")
  }

  def setAsInvalid(errorString: String) = {
    _isValid = false
    updatePrototypeWidgets(errorString)
  }

  def valid = _isValid

  def updatePrototypeWidgets(s: String) = List(inputPrototypeWidget, outputPrototypeWidget).flatten.map { _.updateErrors(s) }

  override def paintWidget = {
    super.paintWidget
    dataUI.task match {
      case Some(x: TaskDataProxyUI) ⇒
        titleLabel.foreground = Color.WHITE
        titleLabel.text = x.dataUI.name
      case None ⇒
    }
  }

  def widget = this

  def copy(sc: BuildMoleScene) = {
    val c = CapsuleUI.withMenu(sc)
    val slotMapping = inputSlots.map(i ⇒ i -> c.addInputSlot).toMap
    (c, slotMapping)
  }

  def starting = scene.dataUI.startingCapsule.map(_ == this).getOrElse(false)

  def defineAsStartingCapsule = {
    scene.dataUI.startingCapsule = Some(this)
    scene.refresh
  }

  def update = {
    inputSlots.foreach(_.refresh)
    updateEnvironmentWidget
    updateSamplingWidget
  }

  def decapsule = {
    dataUI = dataUI.copy(task = None)
    removeWidget(inputPrototypeWidget)
    removeWidget(outputPrototypeWidget)
    removeWidget(samplingWidget)
    samplingWidget = None
    titleLabel.text = ""
    inputPrototypeWidget = Some(PrototypeWidget.buildNoTaskSource(scene, this))
    addChild(inputPrototypeWidget.get)
    outputPrototypeWidget = Some(PrototypeWidget.buildNoTaskHook(scene, this))
    addChild(outputPrototypeWidget.get)
    scene.refresh
  }

  def encapsule(dpu: TaskDataProxyUI) = {
    decapsule
    dataUI = dataUI.copy(task = Some(dpu))
    inputPrototypeWidget = Some(PrototypeWidget.buildTaskSource(scene, this))
    outputPrototypeWidget = Some(PrototypeWidget.buildTaskHook(scene, this))
    CheckData.checkMole(scene)
    addChild(inputPrototypeWidget.get)
    addChild(outputPrototypeWidget.get)
    scene.refresh
  }

  def environment_=(env: Option[EnvironmentDataProxyUI]) = {
    dataUI = dataUI.copy(environment = env)
  }

  private def removeWidget(w: Option[ComponentWidget]) = {
    w match {
      case Some(y: ComponentWidget) ⇒ removeChild(y)
      case None                     ⇒
    }
  }

  def capsuleType_=(cType: CapsuleType) = {
    dataUI = dataUI.copy(capsuleType = cType)
    updateCapsuleTypeWidget
  }

  private def updateCapsuleTypeWidget = {
    removeWidget(capsuleTypeWidget)

    dataUI.capsuleType match {
      case SimpleCapsuleType ⇒ capsuleTypeWidget = None
      case x ⇒
        val img = x match {
          case StrainerCapsuleType ⇒ Images.STRAINER_CAPSULE
          case _                   ⇒ Images.MASTER_CAPSULE
        }
        capsuleTypeWidget = Some(imageWidget(scene, img,
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
      case Some(x: EnvironmentDataProxyUI) ⇒ new ImageIcon(ImageIO.read(x.dataUI.getClass.getClassLoader.getResource(x.dataUI.imagePath)))
      case _                               ⇒ new ImageIcon(ImageIO.read(dataUI.getClass.getClassLoader.getResource("img/noEnv.png")))
    }

    environmentWidget = Some(imageWidget(scene,
      img, TASK_CONTAINER_WIDTH - 10, TASK_CONTAINER_HEIGHT - 3,
      new Action("") {
        def apply = scene.displayCapsuleProperty(capsuleUI, 2)
      }))
    addChild(environmentWidget.get)
  }

  private def updateSamplingWidget = {
    removeWidget(samplingWidget)
    dataUI.task match {
      case Some(t: TaskDataProxyUI) ⇒
        t.dataUI match {
          case d: IExplorationTaskDataUI ⇒ d.sampling match {
            case Some(x: SamplingCompositionDataProxyUI) ⇒
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
  }

  def updateErrors(problems: Iterable[DataflowProblem]) = {
    dataUI.task match {
      case Some(x: TaskDataProxyUI) ⇒
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

  def addInputSlot: InputSlotWidget = {
    val im = new InputSlotWidget(scene, this, nbInputSlots)
    inputSlots += im
    addChild(im)
    scene.refresh
    im
  }

  def removeInputSlot = {
    val toBeRemoved = inputSlots.tail.last
    removeChild(toBeRemoved.widget)
    inputSlots -= toBeRemoved
    scene.refresh
  }

  def inputs(mole: IMole, cMap: Map[CapsuleUI, ICapsule]): List[PrototypeDataProxyUI] = {
    if (cMap.contains(this)) {
      val caps = cMap(this)
      caps.inputs(
        mole,
        Map(caps -> dataUI.sources.map { _.dataUI.coreObject.get }),
        Map(caps -> dataUI.hooks.map { _.dataUI.coreObject.get })).toList.map {
          ds ⇒ Proxies.instance.prototypeOrElseCreate(PrototypeKey(ds.prototype))
        }
    }
    else List()
  }

  def outputs(mole: IMole, cMap: Map[CapsuleUI, ICapsule]): List[PrototypeDataProxyUI] =
    if (cMap.contains(this)) {
      val caps = cMap(this)
      caps.outputs(
        mole,
        Map(caps -> dataUI.sources.map { _.dataUI.coreObject.get }),
        Map(caps -> dataUI.hooks.map { _.dataUI.coreObject.get })).toList.map {
          ds ⇒ Proxies.instance.prototypeOrElseCreate(PrototypeKey(ds.prototype))
        }
    }
    else List()

  def inputs: List[PrototypeDataProxyUI] = {
    val i = scene.dataUI.cacheMole match {
      case Some((m: IMole, cMap: Map[CapsuleUI, ICapsule])) ⇒ inputs(m, cMap)
      case _ ⇒ List()
    }
    i
  }

  def outputs: List[PrototypeDataProxyUI] = scene.dataUI.cacheMole match {
    case Some((m: IMole, cMap: Map[CapsuleUI, ICapsule])) ⇒ outputs(m, cMap)
    case _ ⇒ List()
  }

  def x = location.x
  def y = location.y
  def location = getLocation
}
