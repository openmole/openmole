/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.lhs

import org.openmole.ide.core.model.dataproxy._
import org.openmole.core.model.domain.IBounded
import org.openmole.core.model.domain.IDomain
import org.openmole.core.model.sampling.IFactor
import org.openmole.ide.core.model.data._
import org.openmole.plugin.sampling.lhs._
import org.openmole.core.implementation.sampling.Factor
import org.openmole.core.model.data.Prototype
import scala.collection.JavaConversions._
import org.openmole.misc.exception.UserBadDataError

class LHSSamplingDataUI(val name: String = "",
                        val samples: String = "1",
                        override val factors: List[IFactorDataUI] = List.empty,
                        val samplings: List[ISamplingDataProxyUI] = List.empty) extends ISamplingDataUI {

  implicit def string2Int(s: String): Int = augmentString(s).toInt

  def coreObject =
    new LHS(
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

  //  
  //      factors.flatMap(f ⇒
  //        f.prototype match {
  //          case Some(p: IPrototypeDataProxyUI) ⇒ f.domain match {
  //            case Some(d: IDomainDataUI) ⇒ List(new DiscreteFactor(p.dataUI.coreObject.asInstanceOf[Prototype[Any]],
  //              d.coreObject(p.dataUI.coreObject).asInstanceOf[IDomain[Any] with IIterable[Any]]))
  //            case _ ⇒ Nil
  //          }
  //          case _ ⇒ Nil
  //        }).toSeq: _*)

  def coreClass = classOf[LHS]

  def imagePath = "img/lhsSampling.png"

  override def fatImagePath = "img/lhsSampling_fat.png"

  def buildPanelUI = new LHSSamplingPanelUI(this)
}
