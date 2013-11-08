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

package org.openmole.ide.plugin.hook.file

import org.openmole.ide.core.implementation.dataproxy.{ PrototypeDataProxyUI, Proxies }
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.multirow.MultiComboTextField
import org.openmole.ide.misc.widget.multirow.MultiComboTextField._
import org.openmole.ide.core.implementation.registry._
import java.io.File
import swing.Label
import java.awt.Dimension
import org.openmole.ide.core.implementation.panelsettings.HookPanelUI
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.tools.util.Converters
import org.openmole.ide.misc.tools.util.Converters._
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.URL
import java.util.{ Locale, ResourceBundle }

class CopyFileHookPanelUI(dataUI: CopyFileHookDataUI010)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends PluginPanel("wrap") with HookPanelUI {

  val multiComboTextField = new MultiComboTextField("",
    comboContent,
    dataUI.prototypes.map {
      d ⇒
        new ComboTextFieldPanel(comboContent,
          new ComboTextFieldData(Some(d._1),
            d._2))
    }, minus = CLOSE_IF_EMPTY)

  minimumSize = new Dimension(300, 150)

  contents += new Label("<html><b>Files to be dumped</b></html>")
  contents += multiComboTextField.panel

  val components = List(("Prototypes", this))

  def comboContent = Proxies.instance.classPrototypes(classOf[File]) filter {
    _.dataUI.dim == 0
  }

  def saveContent(name: String) = new CopyFileHookDataUI010(name,
    Converters.flattenTupleOptionAny(
      multiComboTextField.content.filterNot {
        _.comboValue match {
          case Some(v: PrototypeDataProxyUI) ⇒ Proxies.check(List(v)).isEmpty
          case _                             ⇒ false
        }
      }.map { m ⇒ (m.comboValue, new File(m.textFieldValue)) }).map { m ⇒
        (KeyRegistry.protoProxyKeyMap(PrototypeKey(m._1)), m._2)
      })

  override lazy val help = new Helper(List(new URL(i18n.getString("copyFileHookPermalinkText"), i18n.getString("copyFileHookPermalink"))))
}