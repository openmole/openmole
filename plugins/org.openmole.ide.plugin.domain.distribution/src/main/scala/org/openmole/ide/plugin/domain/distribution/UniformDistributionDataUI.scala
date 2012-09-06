/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.domain.distribution

import java.io.File
import org.openmole.core.model.data.Prototype
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.plugin.domain.distribution._
import org.openmole.plugin.domain.modifier._
import org.openmole.misc.exception.UserBadDataError
import org.openmole.misc.workspace._

class UniformDistributionDataUI(
    val name: String = "",
    val size: Int = 1,
    val max: Option[Int] = None) extends IDomainDataUI {

  //FIXME with 2.10 : test if domain is finite for finiteUniformDistribution 
  def coreObject(proto: Prototype[_]) = proto.`type` match {
    case x: Manifest[Int] ⇒ new UniformIntDistribution(max)
    case x: Manifest[Long] ⇒ new UniformLongDistribution
    // case Finite with 2.10 => new FiniteUniformIntDistribution(size, max)
    case _ ⇒ throw new UserBadDataError("The prototype " + proto + " has to be an Int on a Integer domain")
  }

  //FIXME scala 2.10, return the correct class depending on the type
  def coreClass = classOf[FiniteUniformIntDistribution]

  def imagePath = "img/domain_uniform_distribution.png"

  def buildPanelUI = new UniformDistributionPanelUI(this)

  //FIXME : try to be changed in 2.10 test if domain is finite for finiteUniformDistribution 
  def isAcceptable(p: IPrototypeDataProxyUI) =
    p.dataUI.coreObject.`type`.erasure.isAssignableFrom(classOf[Int])

  override def toString = "Uniform distribution"
}
