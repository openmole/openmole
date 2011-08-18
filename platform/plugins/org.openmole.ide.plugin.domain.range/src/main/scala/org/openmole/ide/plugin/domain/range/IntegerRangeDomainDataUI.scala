/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.domain.range

import java.io.File
import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.plugin.domain.range.IntegerRange

class IntegerRangeDomainDataUI(val name: String,val min: String,val max: String,val step: String) extends IDomainDataUI {
  def this(n: String) = this(n,"","","")
  
  override def coreObject = new IntegerRange(min,max,step)

  override def coreClass = classOf[IntegerRange] 
  
  override def imagePath = "img/thumb/completeSampling.png"
  
  override def buildPanelUI = new IntegerRangeDomainPanelUI(this)
}
