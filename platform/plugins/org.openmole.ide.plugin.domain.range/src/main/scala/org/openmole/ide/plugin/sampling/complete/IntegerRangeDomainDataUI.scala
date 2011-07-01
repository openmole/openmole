/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.domain.range


import java.io.File
import org.openmole.ide.core.exception.GUIUserBadDataError

class IntegerRangeDomainDataUI(val name: String) extends IDomainDataUI {
  
  override def coreObject = new IntegerRange

  override def coreClass = classOf[IntegerRange] 
  
  override def imagePath = "img/thumb/integerRangeDomain.png" 
  
  override def buildPanelUI = new IntegerRangeDomainPanelUI(this)
}
