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
      (factors.map(f ⇒ f.prototype match {
        case Some(p: IPrototypeDataProxyUI) ⇒
          DiscreteFactor(Factor(p.dataUI.coreObject.asInstanceOf[Prototype[Any]],
            f.domain.dataUI.coreObject.asInstanceOf[Domain[Any] with Discrete[Any]]))
        case _ ⇒ throw new UserBadDataError("No Prototype is define for the domain " + f.domain.dataUI.preview)
      }) ::: samplings): _*)
  }

  def coreClass = classOf[CompleteSampling]

  def imagePath = "img/completeSampling.png"

  def fatImagePath = "img/completeSampling_fat.png"

  def buildPanelUI = new CompleteSamplingPanelUI(this)

  def isAcceptable(domain: IDomainDataUI) = try {
    println("try sampling")
    domain.coreObject match {
      case x: Domain[Any] with Discrete[Any] ⇒
        println("good domain")
        true
      case _ ⇒
        println("_")
        StatusBar.warn("A Discrete Domain is required for a Complete Sampling")
        false
    }
  } catch {
    case u: UserBadDataError ⇒
      println("UBE : " + u.getMessage)
      StatusBar.warn("This domain is not valid : " + u.getMessage)
      false
  }

  def isAcceptable(sampling: ISamplingDataUI) = true

  def preview = "Complete"
}
