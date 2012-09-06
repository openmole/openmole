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

package org.openmole.ide.plugin.sampling.lhs

import scala.swing._
import swing.Swing._
import swing.ListView._
import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.core.implementation.data._
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.factory._
import org.openmole.ide.core.model.panel._
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import scala.swing.BorderPanel.Position._
import org.openmole.ide.misc.widget.URL
import org.openmole.ide.plugin.sampling.tools.MultiGenericSamplingPanel
import org.openmole.ide.plugin.sampling.tools.MultiGenericSamplingPanel._
import scala.collection.JavaConversions._

class LHSSamplingPanelUI(cud: LHSSamplingDataUI) extends PluginPanel("wrap 2", "", "") with ISamplingPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val sampleTextField = new TextField(cud.samples, 4)
  val multiPanel = new MultiGenericSamplingPanel(Proxys.prototypes.toList,
    domains,
    cud.factors.map { f ⇒
      new GenericSamplingPanel(Proxys.prototypes.toList,
        domains,
        new GenericSamplingData(f))
    })

  tabbedPane.pages += new TabbedPane.Page("Settings", new PluginPanel("wrap 2") {
    contents += new Label("Number of samples")
    contents += sampleTextField
    contents += multiPanel.panel
  })

  def domains = KeyRegistry.domains.values.map { _.buildDataUI }.toList

  override def saveContent(name: String) = new LHSSamplingDataUI(name,
    sampleTextField.text,
    multiPanel.content.map { d ⇒
      new FactorDataUI(d.factor.prototype, d.factor.domain)
    })

  override val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink")))) {
    add(sampleTextField, new Help(i18n.getString("sample"), i18n.getString("sampleEx")))
  }
}
