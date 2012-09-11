/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.method.sensitivity

import org.openmole.core.implementation.sampling.Factor
import org.openmole.core.model.data._
import org.openmole.core.model.domain.IBounded
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.data.EmptyDataUIs
import org.openmole.ide.core.implementation.workflow.sampling._
import org.openmole.ide.core.model.data._
import org.openmole.ide.core.model.dataproxy._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.plugin.method.sensitivity.SaltelliSampling

class SaltelliSamplingDataUI(val name: String = "",
                             val samples: String = "1",
                             val factors: List[IFactorDataUI] = List.empty) extends ISamplingDataUI {

  implicit def string2Int(s: String): Int = augmentString(s).toInt

  def coreObject =
    new SaltelliSampling(
      try samples
      catch {
        case e: NumberFormatException ⇒ throw new UserBadDataError("An integer is exepected as number of samples")
      },
      factors.flatMap { f ⇒
        f.prototype match {
          case Some(p: IPrototypeDataProxyUI) ⇒ f.domain match {
            case Some(d: IDomainDataUI) ⇒
              val proto = p.dataUI.coreObject.asInstanceOf[Prototype[Double]]
              List(new Factor(proto,
                d.coreObject(proto).asInstanceOf[IDomain[Double] with IBounded[Double]]))
            case _ ⇒ Nil
          }
          case _ ⇒ Nil
        }
      }.toSeq: _*)

  def coreClass = classOf[SaltelliSampling]

  def imagePath = "img/saltelliSampling.png"

  override def fatImagePath = "img/saltelliSampling_fat.png"

  def buildPanelUI = new SaltelliSamplingPanelUI(this)

  def inputs = new InputSampling(List(new InputFactorSlot),
    factors)

  //FIXME 2.10
  def isAcceptable(factor: IFactorDataUI) = false

  def isAcceptable(sampling: ISamplingDataUI) = true
}
