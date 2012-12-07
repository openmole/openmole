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
import org.openmole.ide.core.model.sampling.{ IFinite, ISamplingProxyUI }

class CompleteSamplingDataUI extends ISamplingDataUI {
  val name = "Complete"

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

  def isAcceptable(sampling: ISamplingDataUI) = true

  override def isAcceptable(domain: IDomainDataUI) = domain match {
    case f: IFinite ⇒ true
    case _ ⇒
      StatusBar().warn("A Finite Domain is required for a Complete Sampling")
      false
  }

  def preview = name
}
