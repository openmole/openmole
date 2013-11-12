/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.lhs

import org.openmole.ide.core.implementation.serializer.Update
import org.openmole.ide.core.implementation.data.{ DomainDataUI, SamplingDataUI }
import org.openmole.core.model.sampling.{ Sampling, Factor }
import org.openmole.plugin.sampling.lhs.LHS
import org.openmole.ide.core.implementation.sampling.SamplingUtils
import org.openmole.core.model.data.Prototype
import org.openmole.ide.plugin.domain.range.RangeDomainDataUI
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.plugin.domain.range.Range

class LHSSamplingDataUI010 extends SamplingDataUI {

  implicit def string2Int(s: String): Int = augmentString(s).toInt

  def name = "LHS"

  def coreObject(factorOrSampling: List[Either[(Factor[_, _], Int), (Sampling, Int)]]) = util.Try {
    LHS(
      SamplingUtils.toFactors(factorOrSampling).map {
        f ⇒
          Factor(f.prototype.asInstanceOf[Prototype[Double]],
            f.domain.asInstanceOf[Range[Double]])
      }.toSeq: _*)
  }

  def coreClass = classOf[LHS]

  override def imagePath = "img/lhsSampling.png"

  def fatImagePath = "img/lhsSampling_fat.png"

  def buildPanelUI = new LHSSamplingPanelUI(this)

  //FIXME 2.10
  override def isAcceptable(domain: DomainDataUI) =
    domain match {
      case x: RangeDomainDataUI[_] ⇒ x.step match {
        case Some(s: String) ⇒ falseReturn
        case _               ⇒ true
      }
      case _ ⇒ falseReturn
    }

  private def falseReturn = {
    StatusBar().warn("A Bounded range of Double is required for a LHS Sampling")
    false
  }

  def isAcceptable(sampling: SamplingDataUI) = false

  def preview = "LHS"
}

class LHSSamplingDataUI(val samples: String = "1") extends Update[LHSSamplingDataUI010] {
  def update = new LHSSamplingDataUI010
}
