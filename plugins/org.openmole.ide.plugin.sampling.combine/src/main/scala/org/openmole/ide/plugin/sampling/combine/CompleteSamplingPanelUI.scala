/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.plugin.sampling.combine

import scala.swing._
import org.openmole.ide.plugin.sampling.tools.MultiGenericSamplingPanel
import org.openmole.ide.plugin.sampling.tools.MultiGenericSamplingPanel._
import org.openmole.ide.core.implementation.dataproxy.DomainDataProxyFactory
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.factory._
import org.openmole.ide.core.model.panel._
import org.openmole.ide.misc.widget.PluginPanel

class CompleteSamplingPanelUI(cud: CompleteSamplingDataUI) extends PluginPanel("wrap", "", "[]40[]") with ISamplingPanelUI {

  //val panel = new GenericSamplingPanel(cud.factors, KeyRegistry.domains.map { _._2.toString }.toList)
  val samplingPanel = new MultiGenericSamplingPanel(Proxys.prototypes.toList,
    domains,
    cud.factors.map { f ⇒
      new GenericSamplingPanel(Proxys.prototypes.toList,
        domains,
        new GenericSamplingData(Some(f._1),
          Some(f._2.toString),
          Some(f._3)))
    })

  tabbedPane.pages += new TabbedPane.Page("Settings", samplingPanel.panel)

  def domains = KeyRegistry.domains.values.map { f ⇒ new DomainDataProxyFactory(f).buildDataProxyUI }.toList

  override def saveContent(name: String) = new CompleteSamplingDataUI(name, samplingPanel.content.map { c ⇒
    (c.prototypeProxy.get,
      c.domainProxy.get,
      c.domainDataUI.get)
  })
}
