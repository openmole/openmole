/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.domain.distribution

import java.io.File
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.plugin.domain.distribution.SlicedUniformIntDistribution

class SlicedUniformIntegerDistributionDataUI(val name: String,val seed: Long=0,val size: Int=1) extends IDomainDataUI[Int] {
  
  override def coreObject = new SlicedUniformIntDistribution(seed,size)

  override def coreClass = classOf[SlicedUniformIntDistribution] 
  
  override def imagePath = "img/domain_uniform_distribution.png"
  
  override def buildPanelUI = new SlicedUniformIntegerDistributionPanelUI(this)
}
