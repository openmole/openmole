/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.domain.distribution

import java.io.File
import org.openmole.core.model.data.IPrototype
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.plugin.domain.distribution.SlicedUniformIntDistribution

class SlicedUniformIntegerDistributionDataUI(val name: String="",val seed: Long=0,val size: Int=1) extends IDomainDataUI {
  
  def coreObject(proto: IPrototype[_]) = new SlicedUniformIntDistribution(seed,size)

  def coreClass = classOf[SlicedUniformIntDistribution] 
  
  def imagePath = "img/domain_uniform_distribution.png"
  
  def buildPanelUI = new SlicedUniformIntegerDistributionPanelUI(this)
}
