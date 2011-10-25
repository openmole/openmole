/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.environment.desktopgrid


import java.io.File
import org.openmole.plugin.environment.desktopgrid.DesktopGridEnvironment
import org.openmole.ide.core.implementation.exception.GUIUserBadDataError
import org.openmole.ide.core.model.data.IEnvironmentDataUI

class DesktopGridEnvironmentDataUI(val name: String,val login: String,val pass: String,val port: Int) extends IEnvironmentDataUI {
  def this(n:String) = this(n,"","",0)

  override def coreObject = {
    if (login != "" && pass != "") new DesktopGridEnvironment(port,login,pass)
    else throw new GUIUserBadDataError("The login and the password are required fore the environment " + name)
  }

  override def coreClass = classOf[DesktopGridEnvironment] 
  
  override def imagePath = "img/desktop_grid.png" 
  
  override def buildPanelUI = new DesktopGridEnvironmentPanelUI(this)
}
