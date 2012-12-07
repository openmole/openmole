/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.implementation.preference

import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.core.model.panel.IAuthentificationPanelUI
import org.openmole.ide.core.model.preference.IAuthentificationPanel
import org.openmole.ide.misc.widget.PluginPanel
import scala.collection.JavaConversions._
import scala.collection.immutable.HashSet
import scala.swing.ScrollPane
import scala.swing.TabbedPane

class AuthentificationPanel extends PluginPanel("wrap", "[grow,fill]", "") with IAuthentificationPanel {
  val tabbedPane = new TabbedPane
  var auths = new HashSet[IAuthentificationPanelUI]()
  KeyRegistry.authentifications.values.foreach(a ⇒ {
    val p = try {
      Right(a.buildPanelUI)
    } catch {
      case e: Throwable ⇒
        StatusBar().block(Some(e.getMessage).getOrElse(""),
          stack = e.getStackTraceString,
          exceptionName = e.getClass.getCanonicalName)
        Left
    }
    p match {
      case Right(r: IAuthentificationPanelUI) ⇒
        auths += r
        tabbedPane.pages += new TabbedPane.Page(a.toString,
          new ScrollPane { peer.setViewportView(r.peer) })
      case Left ⇒
    }
  })
  contents += tabbedPane

  def save = auths.foreach { a ⇒ a.saveContent }
}