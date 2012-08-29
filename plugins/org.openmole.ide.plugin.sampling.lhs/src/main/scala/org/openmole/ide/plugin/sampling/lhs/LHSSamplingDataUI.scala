/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.lhs

import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.data._
import org.openmole.plugin.sampling.lhs._
import org.openmole.core.implementation.sampling.Factor
import org.openmole.core.model.data.IPrototype
import scala.collection.JavaConversions._
import org.openmole.misc.exception.UserBadDataError

class LHSSamplingDataUI(val name: String = "",
                        val samples: String = "1",
                        val factors: List[(IPrototypeDataProxyUI, String, IBoundedDomainDataUI)] = List.empty) extends ISamplingDataUI {

  implicit def string2Int(s: String): Int = augmentString(s).toInt

  def coreObject =
    new LHS(
      try samples
      catch {
        case e: NumberFormatException ⇒ throw new UserBadDataError("An integer is exepected as number of samples")
      }, factors.map {
        f ⇒
          val proto = f._1.dataUI.coreObject.asInstanceOf[IPrototype[Double]]
          new Factor(proto, f._3.coreObject(proto))
      }: _*)

  def coreClass = classOf[LHS]

  def imagePath = "img/lhsSampling.png"

  override def fatImagePath = "img/lhsSampling_fat.png"

  def buildPanelUI = new LHSSamplingPanelUI(this)
}
