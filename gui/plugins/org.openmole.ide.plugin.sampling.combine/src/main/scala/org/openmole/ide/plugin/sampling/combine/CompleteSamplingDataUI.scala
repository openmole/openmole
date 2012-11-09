/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.combine

import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.data._
import org.openmole.ide.misc.tools.Counter
import org.openmole.plugin.sampling.combine.CompleteSampling
import org.openmole.core.model.sampling._
import org.openmole.core.model.data.Prototype
import org.openmole.core.model.domain.Domain
import org.openmole.core.model.domain.Discrete

class CompleteSamplingDataUI(val id: String = "sampling" + Counter.id.getAndIncrement) extends ISamplingDataUI {

  def coreObject(factors: List[IFactorDataUI],
                 samplings: List[Sampling]) = {
    new CompleteSampling(
      (factors.map(f ⇒ DiscreteFactor(Factor(f.prototype.dataUI.coreObject.asInstanceOf[Prototype[Any]],
        f.domain.coreObject(f.prototype).asInstanceOf[Domain[Any] with Discrete[Any]])))
        ::: samplings): _*)
  }

  def coreClass = classOf[CompleteSampling]

  def imagePath = "img/completeSampling.png"

  def fatImagePath = "img/completeSampling_fat.png"

  def buildPanelUI = new CompleteSamplingPanelUI(this)

  //FIXME 2.10
  def isAcceptable(factor: IFactorDataUI) =
    factor.domain.coreObject(factor.prototype) match {
      case x: Domain[Any] with Discrete[Any] ⇒ true
      case _ => false
    }

  def isAcceptable(sampling: ISamplingDataUI) = true

  def preview = "complete sampling"
}
