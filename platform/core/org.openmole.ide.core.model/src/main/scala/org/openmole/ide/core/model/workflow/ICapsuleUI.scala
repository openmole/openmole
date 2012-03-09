/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.model.workflow

import org.netbeans.api.visual.widget.Widget
import org.openmole.ide.core.model.commons.CapsuleType
import org.openmole.ide.core.model.dataproxy._
import scala.collection.mutable.HashMap

trait ICapsuleUI {
  override def toString = dataProxy match {
    case Some(x : ITaskDataProxyUI)=> x.dataUI.name
    case _=> ""
  }
  
  def capsuleType: CapsuleType.Value
  
  def dataProxy: Option[ITaskDataProxyUI]
  
  def startingCapsule: Boolean
  
  def defineAsStartingCapsule(b: Boolean): Unit
  
  def scene: IMoleScene
  
  def encapsule(dpu: ITaskDataProxyUI)
  
  def addInputSlot(startingCapsule: Boolean): IInputSlotWidget
  
  def removeInputSlot: Unit
  
  def nbInputSlots: Int
  
  def setDataProxy(dpu: ITaskDataProxyUI)
  
  def widget: Widget
  
  def copy(sc: IMoleScene): (ICapsuleUI,HashMap[IInputSlotWidget,IInputSlotWidget])
  
  def environment: Option[IEnvironmentDataProxyUI]
  
  def addEnvironment(env : Option[IEnvironmentDataProxyUI])
  
  def addSampling(env : Option[ISamplingDataProxyUI])
  
  def x: Double
  
  def y: Double
  
  def islots: Iterable[IInputSlotWidget]
}