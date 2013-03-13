/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.model.workflow

import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.model.commons.CapsuleType
import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.core.implementation.validation.DataflowProblem
import org.openmole.ide.core.model.dataproxy._
import scala.collection.mutable.Buffer
import scala.collection.mutable.HashMap
import org.openmole.core.model.mole.IMole

trait ICapsuleUI {
  override def toString = dataUI.task match {
    case Some(x: ITaskDataProxyUI) ⇒ x.dataUI.toString
    case _ ⇒ ""
  }

  def dataUI: ICapsuleDataUI

  def dataUI_=(d: ICapsuleDataUI)

  def defineAsStartingCapsule(b: Boolean): Unit

  def scene: IMoleScene

  def encapsule(dpu: ITaskDataProxyUI)

  def decapsule: Unit

  def removeInputSlot: Unit

  def nbInputSlots: Int

  def widget: Widget

  def copy(sc: IMoleScene): (ICapsuleUI, HashMap[IInputSlotWidget, IInputSlotWidget])

  def deepcopy(sc: IMoleScene): (ICapsuleUI, HashMap[IInputSlotWidget, IInputSlotWidget])

  def ::(dpu: ITaskDataProxyUI)

  def --(cType: CapsuleType)

  def on(environment: Option[IEnvironmentDataProxyUI])

  def addInputSlot(startingCapsule: Boolean): IInputSlotWidget

  def inputs: List[IPrototypeDataProxyUI]

  def outputs: List[IPrototypeDataProxyUI]

  def update: Unit

  def updateErrors(errors: Iterable[DataflowProblem]): Unit

  def x: Double

  def y: Double

  def islots: Buffer[IInputSlotWidget]

  def setAsValid: Unit

  def setAsInvalid(error: String): Unit

  def selected: Boolean

  def selected_=(b: Boolean)
}