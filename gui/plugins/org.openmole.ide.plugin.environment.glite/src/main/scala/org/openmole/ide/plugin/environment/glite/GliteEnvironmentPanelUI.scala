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

package org.openmole.ide.plugin.environment.glite

import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.plugin.environment.tools._
import org.openmole.ide.core.model.panel.IEnvironmentPanelUI
import org.openmole.ide.misc.widget._
import scala.swing.CheckBox
import scala.swing.Label
import scala.swing.TabbedPane
import scala.swing.TextField
import scala.swing.event.ButtonClicked

class GliteEnvironmentPanelUI(pud: GliteEnvironmentDataUI) extends PluginPanel("fillx", "[left][grow,fill]", "") with IEnvironmentPanelUI {

  implicit def intToString(i: Option[Int]) = i match {
    case Some(ii: Int) ⇒ ii.toString
    case _ ⇒ ""
  }

  implicit def stringToInt(s: String) = s match {
    case "" ⇒ None
    case s: String ⇒ try { Some(s.toInt) } catch { case e: NumberFormatException ⇒ None }
    case _ ⇒ None
  }

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val voTextField = new TextField(pud.vo, 20)
  val vomsTextField = new TextField(pud.voms, 20)
  val bdiiTextField = new TextField(pud.bdii, 20)
  val runtimeMemoryTextField = new TextField(pud.runtimeMemory, 4)
  val proxyCheckBox = new CheckBox("MyProxy")
  listenTo(`proxyCheckBox`)
  reactions += {
    case ButtonClicked(`proxyCheckBox`) ⇒ showProxy(proxyCheckBox.selected)
  }

  val proxyTimeTextField = new TextField(pud.proxyTime, 18)
  val proxyTimeLabel = new Label("Time")
  val proxyHostTextField = new TextField(pud.proxyHost, 18)
  val proxyHostLabel = new Label("Host")
  val proxyPortTextField = new TextField(pud.proxyPort, 18)
  val proxyPortLabel = new Label("(Port)")

  val requirementsPanelUI = new RequirementPanelUI(pud.requirements)

  tabbedPane.pages += new TabbedPane.Page("Settings",
    new PluginPanel("wrap 2") {
      contents += (new Label("VO"), "gap para")
      contents += voTextField
      contents += (new Label("VOMS"), "gap para")
      contents += vomsTextField
      contents += (new Label("BDII"), "gap para")
      contents += bdiiTextField
      contents += (new Label("Runtime memory"), "gap para")
      contents += runtimeMemoryTextField
    })

  tabbedPane.pages += requirementsPanelUI
  tabbedPane.pages += new TabbedPane.Page("MyProxy", new PluginPanel("") {
    contents += (proxyCheckBox, "wrap")
    contents += (proxyTimeLabel, "gap para")
    contents += (proxyTimeTextField, "wrap")
    contents += (proxyHostLabel, "gap para")
    contents += (proxyHostTextField, "wrap")
    contents += (proxyPortLabel, "gap para")
    contents += proxyPortTextField
  })

  proxyCheckBox.selected = pud.proxy
  showProxy(pud.proxy)

  private def showProxy(b: Boolean) = {
    List(proxyTimeLabel, proxyTimeTextField, proxyHostLabel, proxyHostTextField, proxyPortLabel, proxyPortTextField).foreach {
      _.visible = b
    }
  }

  override val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink")))) {
    requirementsPanelUI.requirementHelp.foreach { hm ⇒ add(hm._1, hm._2) }
    add(voTextField, new Help(i18n.getString("vo"), i18n.getString("voEx")))
    add(vomsTextField, new Help(i18n.getString("voms"), i18n.getString("vomsEx")))
    add(bdiiTextField, new Help(i18n.getString("bdii"), i18n.getString("bdiiEx")))
    add(proxyCheckBox, new Help(i18n.getString("runtimeMemory"), i18n.getString("runtimeMemoryEx")))
    add(runtimeMemoryTextField, new Help(i18n.getString("myProxy")))
    // add(proxyURLTextField, new Help(i18n.getString("proxyURL"), i18n.getString("proxyURLEx")))
  }

  def saveContent(name: String) =
    new GliteEnvironmentDataUI(name,
      voTextField.text,
      vomsTextField.text,
      bdiiTextField.text,
      proxyCheckBox.selected,
      proxyTimeTextField.text,
      proxyHostTextField.text,
      proxyPortTextField.text.isEmpty match {
        case true ⇒ None
        case false ⇒ proxyPortTextField.text
      },
      runtimeMemoryTextField.text,
      new RequirementDataUI(
        requirementsPanelUI.architectureCheckBox.selected,
        requirementsPanelUI.workerNodeMemoryTextField.text,
        requirementsPanelUI.maxCPUTimeTextField.text,
        requirementsPanelUI.otherRequirementTextField.text))
}
