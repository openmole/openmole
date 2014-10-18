/*
 * Copyright (C) 2012 Mathieu Leclaire 
 * < mathieu.leclaire at openmole.org >
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
package org.openmole.ide.plugin.domain.modifier

import org.openmole.ide.misc.widget.{ Help, URL, Helper, PluginPanel }
import swing.{ Label, TextField }
import java.util.{ Locale, ResourceBundle }
import org.openmole.ide.core.implementation.execution.ScenesManager
import org.openmole.ide.misc.tools.util.Types._
import org.openmole.ide.core.implementation.data.DomainDataUI
import org.openmole.ide.core.implementation.panelsettings.IDomainPanelUI
import org.openmole.ide.core.implementation.sampling.SamplingCompositionPanelUI

class SlidingDomainPanelUI(val dataUI: SlidingDomainDataUI[_])(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends IDomainPanelUI {

  val sizeField = new TextField(dataUI.size, 5)
  val stepTextField = new TextField(dataUI.step, 5)

  val components = List(("", new PluginPanel("wrap 2") {
    contents += (new Label("Size"), "gap para")
    contents += sizeField
    contents += (new Label("Step"), "gap para")
    contents += stepTextField
  }))

  override def toString = dataUI.name

  def saveContent = {
    val classString =
      ScenesManager().currentSamplingCompositionPanelUI.headOption match {
        case Some(scp: SamplingCompositionPanelUI) ⇒ scp.firstNoneModifierDomain(dataUI) match {
          case Some(d: DomainDataUI) ⇒ d.domainType.toString.split('.').last
          case _                     ⇒ DOUBLE
        }
        case _ ⇒
          DOUBLE
      }
    SlidingDomainDataUI(sizeField.text, stepTextField.text, classString, dataUI.previousDomain)
  }

  override lazy val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink"))))

  add(sizeField, new Help(i18n.getString("size"), i18n.getString("sizeEx")))
  add(stepTextField, new Help(i18n.getString("step"), i18n.getString("stepEx")))

}