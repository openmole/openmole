/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.model.workflow

import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.model.data.ICapsuleDataUI
import org.openmole.ide.core.model.dataproxy._
import scala.collection.mutable.HashMap

trait ICapsuleUI {
  override def toString = dataUI.task match {
    case Some(x : ITaskDataProxyUI)=> x.dataUI.toString
    case _=> ""
  }
  
  def dataUI : ICapsuleDataUI
  
  def defineAsStartingCapsule(b: Boolean): Unit
  
  def scene: IMoleScene
  
  def encapsule(dpu: ITaskDataProxyUI)
  
  def removeInputSlot: Unit
  
  def nbInputSlots: Int
  
  def widget: Widget
  
  def copy(sc: IMoleScene): (ICapsuleUI,HashMap[IInputSlotWidget,IInputSlotWidget])
  
  def setTask(dpu: ITaskDataProxyUI)
  
  def addEnvironment(env : Option[IEnvironmentDataProxyUI])
  
  def addSampling(env : Option[ISamplingDataProxyUI])
  
  def addInputSlot(startingCapsule: Boolean): IInputSlotWidget
  
  def x: Double
  
  def y: Double
  
  def islots: Iterable[IInputSlotWidget]
}