/*
 * Copyright (C) 2012 mathieu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.dialog

import scala.swing.Frame
import java.awt.Point
import java.awt.Toolkit
import javax.swing.ImageIcon
import org.openmole.ide.misc.tools.image.Images._
import scala.swing.Label

class SplashScreen extends Frame{
  
  peer.setUndecorated(true)
  
  iconImage = new ImageIcon ( this.getClass.getClassLoader.getResource("/openmole.png") ).getImage
  title = "OpenMOLE"
  
  val screenSize = Toolkit.getDefaultToolkit.getScreenSize
  contents = new Label { icon = SPLASH_SCREEN }
  
  location = new Point((screenSize.width - size.width) / 2 , (screenSize.height - size.height) / 2 )
}