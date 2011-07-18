/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.environment.glite


import java.io.File
import org.openmole.plugin.environment.glite.GliteEnvironment
import org.openmole.ide.misc.exception.GUIUserBadDataError
import org.openmole.ide.core.model.data.IEnvironmentDataUI

class GliteEnvironmentDataUI(val name: String,val vo: String,val voms: String,val bdii: String) extends IEnvironmentDataUI {
  def this(n:String) = this(n,"","","")
  
  override def coreObject = {
    if (vo != "" && voms != "" && bdii != "") new GliteEnvironment(vo,voms,bdii)
    else throw new GUIUserBadDataError("CSV file path missing to instanciate the CSV sampling " + name)
  }

  override def coreClass = classOf[GliteEnvironment] 
  
  override def imagePath = "img/thumb/glite.png" 
  
  override def buildPanelUI = new GliteEnvironmentPanelUI(this)
}
