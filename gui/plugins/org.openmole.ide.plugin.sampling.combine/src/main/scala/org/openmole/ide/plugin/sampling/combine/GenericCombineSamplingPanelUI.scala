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
import org.openmole.ide.core.model.panel._
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import swing.TabbedPane.Page
import org.openmole.ide.core.model.data.ISamplingDataUI

class GenericCombineSamplingPanelUI(cud: ISamplingDataUI) extends PluginPanel("wrap") with ISamplingPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  contents += new Label("<html><i>No more information is required for this Sampling</i></html>")

  def saveContent = cud
}
