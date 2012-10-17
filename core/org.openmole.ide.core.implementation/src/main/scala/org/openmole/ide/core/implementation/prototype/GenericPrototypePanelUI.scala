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
import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.core.model.data.IPrototypeDataUI
import org.openmole.ide.core.implementation.panel.BasePanel._
import org.openmole.ide.core.model.panel.IPrototypePanelUI
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.URL
import scala.reflect.runtime.universe._
import scala.swing.Component
import scala.swing.Label
import scala.swing.MyComboBox
import scala.swing.Publisher
import scala.swing.TextField
import scala.swing.event.ActionEvent
import scala.swing.event.SelectionChanged

class GenericPrototypePanelUI[T](dataUI: GenericPrototypeDataUI[_ <: T]) extends PluginPanel("wrap 2") with IPrototypePanelUI[T] {

  val typeValues = GenericPrototypeDataUI.base
  val typeComboBox = new MyComboBox(typeValues)
  typeComboBox.selection.item = typeValues.filter {
    _.typeClassString == dataUI.typeClassString
  }.head

  listenTo(`typeComboBox`)
  typeComboBox.selection.reactions += {
    case SelectionChanged(`typeComboBox`) ⇒
      publish(new IconChanged(typeComboBox, typeComboBox.selection.item.fatImagePath))
    case _ ⇒
  }

  val dimTextField = new TextField(if (dataUI.dim >= 0) dataUI.dim.toString else "0", 2)

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))
  contents += new Label("Type")
  contents += typeComboBox
  contents += new Label("Dimension")
  contents += dimTextField

  def dim = dimTextField.text

  override val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink")))) {
    add(dimTextField, new Help(i18n.getString("dimension"), i18n.getString("dimensionEx")))
  }
  override def saveContent(name: String) =
    typeComboBox.selection.item.newInstance(name,
      { if (dim.isEmpty) 0 else dim.toInt })
}