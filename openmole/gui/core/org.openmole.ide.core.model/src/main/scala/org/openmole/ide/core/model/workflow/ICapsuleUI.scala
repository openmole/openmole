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
import org.openmole.core.model.mole.{ ICapsule, IMole }
import org.openmole.core.model.data.Prototype
import org.openmole.ide.misc.tools.util._

trait ICapsuleUI {
  override def toString = dataUI.task match {
    case Some(x: ITaskDataProxyUI) ⇒ x.dataUI.toString
    case _                         ⇒ ""
  }

  def id: ID.Type

  def starting: Boolean

  def scene: IMoleScene

  def dataUI: ICapsuleDataUI

  def dataUI_=(d: ICapsuleDataUI)

  def encapsule(dpu: ITaskDataProxyUI)

  def decapsule: Unit

  def removeInputSlot: Unit

  def nbInputSlots: Int

  def widget: Widget

  def copy(sc: IBuildMoleScene): (ICapsuleUI, Map[IInputSlotWidget, IInputSlotWidget])

  def capsuleType_=(cType: CapsuleType)

  def environment_=(environment: Option[IEnvironmentDataProxyUI])

  def addInputSlot: IInputSlotWidget

  def inputs: List[IPrototypeDataProxyUI]

  def inputs(mole: IMole, cMap: Map[ICapsuleUI, ICapsule], pMap: Map[IPrototypeDataProxyUI, Prototype[_]]): List[IPrototypeDataProxyUI]

  def outputs: List[IPrototypeDataProxyUI]

  def outputs(mole: IMole, cMap: Map[ICapsuleUI, ICapsule], pMap: Map[IPrototypeDataProxyUI, Prototype[_]]): List[IPrototypeDataProxyUI]

  def update: Unit

  def updateErrors(errors: Iterable[DataflowProblem]): Unit

  def x: Int

  def y: Int

  def islots: Buffer[IInputSlotWidget]

  def setAsValid: Unit

  def setAsInvalid(error: String): Unit

  def valid: Boolean

  def selected: Boolean

  def selected_=(b: Boolean)

}