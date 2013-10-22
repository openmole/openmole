/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.combine

import org.openmole.plugin.sampling.combine.CompleteSampling
import org.openmole.core.model.sampling._
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.misc.widget.{ URL, Helper }
import org.openmole.ide.core.implementation.sampling.{ FiniteUI, SamplingUtils }
import org.openmole.ide.core.implementation.data.{ SamplingDataUI, DomainDataUI }
import java.util.{ ResourceBundle, Locale }

import org.openmole.ide.core.implementation.panel.NoParameterSamplingPanelUI

class CompleteSamplingDataUI extends SamplingDataUI {
  val name = "Complete"

  def coreObject(factorOrSampling: List[Either[(Factor[_, _], Int), (Sampling, Int)]]) = util.Try {
    CompleteSampling(SamplingUtils.toUnorderedFactorsAndSamplings(factorOrSampling): _*)
  }

  def coreClass = classOf[CompleteSampling]

  override def imagePath = "img/completeSampling.png"

  def fatImagePath = "img/completeSampling_fat.png"

  def buildPanelUI = new NoParameterSamplingPanelUI(this) {
    val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))
    override lazy val help = new Helper(List(new URL(i18n.getString("completePermalinkText"),
      i18n.getString("completePermalink"))))
  }

  def isAcceptable(sampling: SamplingDataUI) = true

  override def isAcceptable(domain: DomainDataUI) = domain match {
    case f: FiniteUI ⇒ true
    case _ ⇒
      StatusBar().warn("A Finite Domain is required for a Complete Sampling")
      false
  }

  def preview = name
}
