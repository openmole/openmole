/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.domain.distribution

import java.io.File
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.plugin.domain.distribution._
import org.openmole.plugin.domain.modifier._
import org.openmole.misc.workspace._

class FiniteUniformIntDistributionDataUI(
  val name: String="",
  val size: Int=1) extends IDomainDataUI {
  
  def coreObject(proto: IPrototype[_]) = new FiniteUniformIntDistribution(size)

  def coreClass = classOf[FiniteUniformIntDistribution] 
  
  def imagePath = "img/domain_uniform_distribution.png"
  
  def buildPanelUI = new FiniteUniformIntDistributionPanelUI(this)
}
