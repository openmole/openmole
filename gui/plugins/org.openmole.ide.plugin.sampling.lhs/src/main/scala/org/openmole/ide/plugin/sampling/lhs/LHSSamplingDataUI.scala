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
import org.openmole.ide.core.implementation.dialog.StatusBar

class LHSSamplingDataUI(val samples: String = "1") extends ISamplingDataUI {

  implicit def string2Int(s: String): Int = augmentString(s).toInt

  def coreObject(factors: List[IFactorDataUI],
                 samplings: List[Sampling]) =
    new LHS(
      try samples
      catch {
        case e: NumberFormatException ⇒ throw new UserBadDataError("An integer is exepected as number of samples")
      },
      factors.map {
        f ⇒
          Factor(f.prototype.dataUI.coreObject.asInstanceOf[Prototype[Double]],
            f.domain.coreObject(None).asInstanceOf[Domain[Double] with Bounds[Double]])
      }.toSeq: _*)

  def coreClass = classOf[LHS]

  def imagePath = "img/lhsSampling.png"

  override def fatImagePath = "img/lhsSampling_fat.png"

  def buildPanelUI = new LHSSamplingPanelUI(this)

  //FIXME 2.10
  def isAcceptable(domain: IDomainDataUI[_]) =
    domain.coreObject(None) match {
      case x: Domain[Double] with Bounds[Double] ⇒ true
      case _ ⇒
        StatusBar.warn("A Bounded range of Double is required for a LHS Sampling")
        false
    }

  def isAcceptable(sampling: ISamplingDataUI) = true

  def preview = "LHS with " + samples + "samples"
}
