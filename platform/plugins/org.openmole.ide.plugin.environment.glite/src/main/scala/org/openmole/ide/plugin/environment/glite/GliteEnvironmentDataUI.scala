/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.environment.glite

import org.openmole.plugin.environment.glite.GliteEnvironment
import org.openmole.ide.core.implementation.exception.GUIUserBadDataError
import org.openmole.ide.core.model.data.IEnvironmentDataUI
import org.openmole.plugin.environment.glite.MyProxy

class GliteEnvironmentDataUI(val name: String,val vo: String,val voms: String,val bdii: String,
                             val proxy: Boolean= false,val proxyURL: String="", val proxyUser: String="") extends IEnvironmentDataUI {
  def this(n:String) = this(n,"","","",false,"","")
  
  override def coreObject = {
    if (vo != "" && voms != "" && bdii != "") 
      if (proxy && proxyURL != "" && proxyUser != "") new GliteEnvironment(vo,voms,bdii,new MyProxy(proxyURL,proxyUser))
      else new GliteEnvironment(vo,voms,bdii)
    else throw new GUIUserBadDataError("CSV file path missing to instanciate the CSV sampling " + name)
  }

  override def coreClass = classOf[GliteEnvironment] 
  
  override def imagePath = "img/glite.png" 
  
  override def buildPanelUI = new GliteEnvironmentPanelUI(this)
}
