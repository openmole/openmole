/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.combine

import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.sampling._
import org.openmole.core.model.sampling.Sampling
import org.openmole.ide.core.model.data._
import org.openmole.ide.misc.tools.Counter
import org.openmole.plugin.sampling.combine.CompleteSampling
import org.openmole.core.model.sampling._
import org.openmole.core.model.data.Prototype
import org.openmole.core.model.domain.Domain
import org.openmole.core.model.domain.Discrete
import scala.collection.JavaConversions._

class CompleteSamplingDataUI(val id: String = "sampling" + Counter.id.getAndIncrement) extends ISamplingDataUI {

  def coreObject(factors: List[IFactorDataUI],
                 samplings: List[Sampling]) = {
    new CompleteSampling(
      (factors.flatMap(f ⇒
        f.prototype match {
          case Some(p: IPrototypeDataProxyUI) ⇒ f.domain match {
            case Some(d: IDomainDataUI[_]) ⇒ List(DiscreteFactor(Factor(p.dataUI.coreObject.asInstanceOf[Prototype[Any]],
              d.coreObject(p).asInstanceOf[Domain[Any] with Discrete[Any]])))
            case _ ⇒ Nil
          }
          case _ ⇒ Nil
        }) ::: samplings): _*)
  }
  //      new CompleteSampling(
  //      factors.map(f ⇒ DiscreteFactor(
  //          f._1.dataUI.coreObject.asInstanceOf[Prototype[Any]],
  //          f._3.coreObject(f._1.dataUI.coreObject).asInstanceOf[Domain[Any] with IIterable[Any]])).toSeq: _*)

  def coreClass = classOf[CompleteSampling]

  def imagePath = "img/completeSampling.png"

  def fatImagePath = "img/completeSampling_fat.png"

  def buildPanelUI = new CompleteSamplingPanelUI(this)

  //FIXME 2.10
  def isAcceptable(factor: IFactorDataUI) = true

  def isAcceptable(sampling: ISamplingDataUI) = true

  def preview = "complete sampling"
}
