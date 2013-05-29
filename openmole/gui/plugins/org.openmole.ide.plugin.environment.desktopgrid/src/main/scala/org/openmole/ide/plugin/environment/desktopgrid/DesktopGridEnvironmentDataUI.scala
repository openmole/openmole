/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.environment.desktopgrid

import org.openmole.plugin.environment.desktopgrid.DesktopGridEnvironment
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.model.data.IEnvironmentDataUI

class DesktopGridEnvironmentDataUI(val name: String = "",
                                   val login: String = "",
                                   val pass: String = "",
                                   val port: Int = 0) extends IEnvironmentDataUI {

  override def coreObject = {
    if (login != "" && pass != "") DesktopGridEnvironment(port, login, pass)
    else throw new UserBadDataError("The login and the password are required fore the environment " + name)
  }

  override def coreClass = classOf[DesktopGridEnvironment]

  override def imagePath = "img/desktop_grid.png"

  override def fatImagePath = "img/desktop_grid_fat.png"

  override def buildPanelUI = new DesktopGridEnvironmentPanelUI(this)
}
