/*
 * Copyright (C) 2011 <mathieu.Mathieu Leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.prototype

import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.core.implementation.dialog._
import org.openmole.ide.misc.widget._
import scala.swing._
import scala.swing.event.ActionEvent
import scala.swing.event.SelectionChanged
import java.io.File
import org.openmole.misc.tools.obj.ClassUtils._
import org.openmole.ide.core.implementation.panel.{ SaveSettings, Settings }

trait GenericPrototypePanelUI extends Settings with SaveSettings {
  protoPanel ⇒

  type DATAUI = GenericPrototypeDataUI[_]

  val dataUI: DATAUI

  val typeValues = GenericPrototypeDataUI.base ::: GenericPrototypeDataUI.extra

  lazy val customTypeLabel = new MainLinkLabel("More types", new Action("") {
    override def apply = PrototypeFromJarDialog.display(protoPanel)
  })

  lazy val dimTextField = new TextField(if (dataUI.dim >= 0) dataUI.dim.toString else "0", 2)

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val components = List(("Settings", new PluginPanel("wrap 2") {
    contents += new Label("Dimension")
    contents += dimTextField
    contents += customTypeLabel
  }))

  def dim = dimTextField.text

  override lazy val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink")))) {
    add(dimTextField, new Help(i18n.getString("dimension"), i18n.getString("dimensionEx")))
  }

  def saveContent(name: String): DATAUI = dataUI.newInstance(name, if (dim.isEmpty) 0 else dim.toInt)
}
