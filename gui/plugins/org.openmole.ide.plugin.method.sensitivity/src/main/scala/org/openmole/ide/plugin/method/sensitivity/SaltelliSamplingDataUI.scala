/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.method.sensitivity

import org.openmole.core.model.sampling.Factor
import org.openmole.core.model.data._
import org.openmole.core.model.domain._
import org.openmole.core.model.sampling.Sampling
import org.openmole.core.model.task._
import org.openmole.ide.core.implementation.data.EmptyDataUIs
import org.openmole.ide.core.implementation.sampling._
import org.openmole.ide.core.model.data._
import org.openmole.ide.core.model.dataproxy._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.plugin.method.sensitivity.SaltelliSampling
import org.openmole.ide.plugin.domain.range.RangeDomainDataUI
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.misc.widget.{ URL, Helper }

class SaltelliSamplingDataUI(val samples: String = "1") extends ISamplingDataUI {

  implicit def string2Int(s: String): Int = augmentString(s).toInt

  def name = "Saltelli"

  def coreObject(factorOrSampling: List[Either[(Factor[_, _], Int), (Sampling, Int)]]) =
    new SaltelliSampling(
      try samples
      catch {
        case e: NumberFormatException ⇒ throw new UserBadDataError("An integer is exepected as number of samples")
      },
      SamplingUtils.toFactors(factorOrSampling).map {
        f ⇒
          Factor(f.prototype.asInstanceOf[Prototype[Double]],
            f.domain.asInstanceOf[Domain[Double] with Bounds[Double]])
      }.toSeq: _*)

  def coreClass = classOf[SaltelliSampling]

  def imagePath = "img/saltelliSampling.png"

  override def fatImagePath = "img/saltelliSampling_fat.png"

  def buildPanelUI = new SaltelliSamplingPanelUI(this)

  override def isAcceptable(domain: IDomainDataUI) = domain match {
    case x: RangeDomainDataUI[_] ⇒ true
    case _ ⇒
      StatusBar().warn("A Bounded range of Double is required for a LHS Sampling")
      false
  }

  def isAcceptable(sampling: ISamplingDataUI) = false

  def preview = "Saltelli (" + samples + ")"
}
