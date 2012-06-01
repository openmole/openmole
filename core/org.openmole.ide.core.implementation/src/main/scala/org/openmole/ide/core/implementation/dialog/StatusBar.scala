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

import java.awt.Color
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.core.model.panel.PanelMode._
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import org.openmole.ide.core.model.workflow.ISceneContainer
import org.openmole.ide.misc.widget.LinkLabel
import org.openmole.ide.misc.widget.MigPanel
import scala.swing.Action
import scala.swing.Label

object StatusBar extends MigPanel("wrap 2") { statusBar ⇒
  background = Color.WHITE
  opaque = true

  var strings = ""

  def inform(info: String,
             proxy: IDataProxyUI): Unit = genericProxy("[INFO] ", info, proxy)

  def inform(info: String) = generic("[INFO] ", info)

  def warn(warning: String,
           proxy: IDataProxyUI): Unit = genericProxy("[WARNING] ", warning, proxy)

  def warn(warning: String) = generic("[WARNING] ", warning)

  def block(b: String,
            proxy: IDataProxyUI): Unit = genericProxy("[CRITICAL] ", b, proxy)

  def block(b: String) = generic("[CRITICAL] ", b)

  def generic(header: String,
              error: String) = {
    if (!strings.contains(error)) {
      strings += error
      contents += new Label(header)
      contents += new Label(error)
      revalidate
      repaint
    }
  }

  def genericProxy(header: String,
                   error: String,
                   proxy: IDataProxyUI) = {
    if (!strings.contains(error)) {
      strings += error
      contents += new LinkLabel(header,
        new Action("") { override def apply = display(proxy) },
        4,
        "0088aa")
      contents += new Label(error)
      revalidate
      repaint
    }
  }

  def clear = {
    contents.clear
    revalidate
    repaint
    strings = ""
  }

  def isValid = strings.isEmpty

  def display(proxy: IDataProxyUI) =
    ScenesManager.currentSceneContainer match {
      case Some(sc: ISceneContainer) ⇒ sc.scene.displayPropertyPanel(proxy, EDIT)
      case None ⇒
    }
}
