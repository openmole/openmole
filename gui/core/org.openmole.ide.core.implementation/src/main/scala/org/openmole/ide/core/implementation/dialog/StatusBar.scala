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
import compat.Platform.EOL
import org.openmole.ide.core.implementation.workflow.{ ExecutionMoleSceneContainer, BuildMoleSceneContainer }
import util.Failure

object StatusBar {
  def apply() = ScenesManager.currentSceneContainer match {
    case Some(b: BuildMoleSceneContainer) ⇒ b.statusBar
    case Some(e: ExecutionMoleSceneContainer) ⇒ e.bmsc.statusBar
    case _ ⇒ new StatusBar
  }

  def displayErrors(f: ⇒ Traversable[Throwable]) = {
    StatusBar().clear
    f.foreach {
      t ⇒
        StatusBar().block(
          t.getMessage,
          stack = t.getMessage + "\n" + t.getStackTraceString)
    }
  }

}

class StatusBar extends MigPanel("wrap 3") {
  statusBar ⇒
  background = Color.WHITE
  opaque = true

  var strings = ""

  def inform(t: Throwable, proxy: Option[IDataProxyUI]): Unit =
    inform(t.getMessage, proxy, t.getClass.getCanonicalName + "\n" + errorStack(t))

  def inform(info: String,
             proxy: Option[IDataProxyUI] = None,
             stack: String = ""): Unit =
    printError("[INFO]", info, proxy, stack)

  def warn(t: Throwable, proxy: Option[IDataProxyUI]): Unit =
    warn(t.getMessage, proxy, t.getClass.getCanonicalName + "\n" + errorStack(t))

  def warn(warning: String,
           proxy: Option[IDataProxyUI] = None,
           stack: String = ""): Unit = printError("[WARNING] ", warning, proxy, stack)

  def block(t: Throwable): Unit = block(t, None)

  def block(t: Throwable, proxy: Option[IDataProxyUI]): Unit =
    block(t.getMessage, proxy, t.getClass.getCanonicalName + "\n" + errorStack(t))

  def block(b: String,
            proxy: Option[IDataProxyUI] = None,
            stack: String = ""): Unit = printError("[CRITICAL] ", b, proxy, stack)

  def errorStack(e: Throwable) =
    Iterator.iterate(e)(_.getCause).takeWhile(_ != null).map(e ⇒ e.getMessage + e.getStackTraceString).mkString(EOL)

  def printError(header: String,
                 error: String,
                 proxy: Option[IDataProxyUI],
                 stack: String) =
    if (error == null || !strings.contains(error)) {
      strings += error
      proxy match {
        case Some(x: IDataProxyUI) ⇒
          contents += new LinkLabel(header,
            new Action("") {
              override def apply = displayProxy(x)
            },
            3,
            "0088aa",
            true)

        case None ⇒ contents += new Label("<html><b>" + header + "</b></html>")
      }
      if (stack.isEmpty || stack == "\n") {
        contents += (new Label(error), "wrap")
      } else {
        contents += new Label(error)
        contents += new LinkLabel(" details",
          new Action("") {
            override def apply = DialogFactory.displayStack(stack)
          },
          3,
          "0088aa",
          true)
      }
      revalidate
      repaint
    }

  def clear = {
    contents.clear
    revalidate
    repaint
    strings = ""
  }

  def isValid = strings.isEmpty

  def displayProxy(proxy: IDataProxyUI) =
    ScenesManager.currentSceneContainer match {
      case Some(sc: ISceneContainer) ⇒ sc.scene.displayPropertyPanel(proxy, EDIT)
      case None ⇒
    }
}
