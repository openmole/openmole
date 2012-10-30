/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.lhs

import org.openmole.ide.core.model.dataproxy._
import org.openmole.core.model.domain.Bounds
import org.openmole.core.model.domain.Domain
import org.openmole.core.model.sampling.Factor
import org.openmole.ide.misc.tools.Counter
import org.openmole.ide.core.model.data._
import org.openmole.plugin.sampling.lhs._
import org.openmole.core.model.data.Prototype
import scala.collection.JavaConversions._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.sampling.Sampling
import org.openmole.ide.core.implementation.sampling._

class LHSSamplingDataUI(val samples: String = "1",
                        val id: String = "sampling" + Counter.id.getAndIncrement) extends ISamplingDataUI {

  implicit def string2Int(s: String): Int = augmentString(s).toInt

  def coreObject(factors: List[IFactorDataUI],
                 samplings: List[Sampling]) =
    new LHS(
      try samples
      catch {
        case e: NumberFormatException ⇒ throw new UserBadDataError("An integer is exepected as number of samples")
      },
      factors.flatMap { f ⇒
        f.prototype match {
          case Some(p: IPrototypeDataProxyUI) ⇒ f.domain match {
            case Some(d: IDomainDataUI[_]) ⇒
              val proto = p.dataUI.coreObject.asInstanceOf[Prototype[Double]]
              List(Factor(proto,
                d.coreObject(p).asInstanceOf[Domain[Double] with Bounds[Double]]))
            case _ ⇒ Nil
          }
          case _ ⇒ Nil
        }
      }.toSeq: _*)

  def coreClass = classOf[LHS]

  def imagePath = "img/lhsSampling.png"

  override def fatImagePath = "img/lhsSampling_fat.png"

  def buildPanelUI = new LHSSamplingPanelUI(this)

  //FIXME 2.10
  def isAcceptable(factor: IFactorDataUI) = false

  def isAcceptable(sampling: ISamplingDataUI) = true

  def preview = "LHS with " + samples + "samples"
}
