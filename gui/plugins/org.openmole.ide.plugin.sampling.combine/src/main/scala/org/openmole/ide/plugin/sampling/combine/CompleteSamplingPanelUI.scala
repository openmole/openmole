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

package org.openmole.ide.plugin.sampling.combine

import scala.swing._
import org.openmole.ide.misc.widget.URL
import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.core.model.sampling._
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.sampling._
import org.openmole.ide.core.model.factory._
import org.openmole.ide.core.model.panel._
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel

class CompleteSamplingPanelUI(cud: CompleteSamplingDataUI) extends PluginPanel("wrap", "", "[]40[]") with ISamplingPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  contents += new Label("<html><i>No more information is required for this Sampling</i></html>")

  override def saveContent = new CompleteSamplingDataUI(cud.id)

  override val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink"))))
}
