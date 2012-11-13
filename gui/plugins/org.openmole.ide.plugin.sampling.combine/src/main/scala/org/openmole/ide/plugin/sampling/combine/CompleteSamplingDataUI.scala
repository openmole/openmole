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
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.misc.exception.UserBadDataError

class CompleteSamplingDataUI extends ISamplingDataUI {

  def coreObject(factors: List[IFactorDataUI],
                 samplings: List[Sampling]) = {
    new CompleteSampling(
      (factors.map(f ⇒ DiscreteFactor(Factor(f.prototype.dataUI.coreObject.asInstanceOf[Prototype[Any]],
        f.domain.coreObject.asInstanceOf[Domain[Any] with Discrete[Any]])))
        ::: samplings): _*)
  }

  def coreClass = classOf[CompleteSampling]

  def imagePath = "img/completeSampling.png"

  def fatImagePath = "img/completeSampling_fat.png"

  def buildPanelUI = new CompleteSamplingPanelUI(this)

  def isAcceptable(domain: IDomainDataUI[_]) = try {
    domain.coreObject match {
      case x: Domain[Any] with Discrete[Any] ⇒ true
      case _ ⇒
        StatusBar.warn("A Discrete Domain is required for a Complete Sampling")
        false
    }
  } catch {
    case u: UserBadDataError ⇒
      StatusBar.warn("This factor is not valid : " + u.getMessage)
      false
  }

  def isAcceptable(sampling: ISamplingDataUI) = true

  def preview = "complete sampling"
}
