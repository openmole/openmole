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
import org.openmole.ide.misc.widget.{ URL, Helper }
import org.openmole.ide.core.model.sampling.ISamplingProxyUI

class CompleteSamplingDataUI extends ISamplingDataUI {

  def coreObject(factors: List[IFactorDataUI],
                 samplings: List[Sampling]) =
    new CompleteSampling((CombineSamplingCoreFactory(factors) ::: samplings): _*)

  def coreClass = classOf[CompleteSampling]

  def imagePath = "img/completeSampling.png"

  def fatImagePath = "img/completeSampling_fat.png"

  def buildPanelUI = new GenericCombineSamplingPanelUI(this) {
    override val help = new Helper(List(new URL(i18n.getString("completePermalinkText"),
      i18n.getString("completePermalink"))))
  }

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
      StatusBar.warn("This domain is not valid : " + u.getMessage)
      false
  }

  def isAcceptable(sampling: ISamplingDataUI) = true

  def preview = "Complete"
}
