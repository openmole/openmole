/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.domain.range

import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.plugin.domain.range.DoubleRange

class DoubleRangeDomainDataUI(val name: String,val min: String,val max: String,val step: String) extends IDomainDataUI[Double] {
  def this(n: String) = this(n,"","","")
  
  override def coreObject = new DoubleRange(min,max,step)

  override def coreClass = classOf[DoubleRange] 
  
  override def imagePath = "img/domain_range.png"
  
  override def buildPanelUI = new DoubleRangeDomainPanelUI(this)
}
