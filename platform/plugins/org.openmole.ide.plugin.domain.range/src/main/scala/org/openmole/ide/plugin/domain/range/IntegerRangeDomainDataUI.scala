/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.domain.range

import org.openmole.ide.core.model.data.IDomainDataUI
import org.openmole.ide.misc.exception.GUIUserBadDataError
import org.openmole.plugin.domain.range.IntegerRange

class IntegerRangeDomainDataUI(val name: String) extends IDomainDataUI {
  
  override def coreObject = new IntegerRange("","")

  override def coreClass = classOf[IntegerRange] 
  
  override def imagePath = "img/thumb/integerRangeDomain.png" 
  
  override def buildPanelUI = new IntegerRangeDomainPanelUI(this)
}
