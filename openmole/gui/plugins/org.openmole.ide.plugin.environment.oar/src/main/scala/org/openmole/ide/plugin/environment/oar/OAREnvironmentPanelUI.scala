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

package org.openmole.ide.plugin.environment.oar

import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.URL
import scala.swing.Label
import scala.swing.TabbedPane
import scala.swing.TextField
import swing.TabbedPane.Page
import java.awt.Dimension
import org.openmole.ide.core.implementation.panelsettings.EnvironmentPanelUI

class OAREnvironmentPanelUI(pud: OAREnvironmentDataUI)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends PluginPanel("fillx,wrap 2", "", "") with EnvironmentPanelUI {

  implicit def stringToStringOpt(s: String) = s.isEmpty match {
    case true  ⇒ None
    case false ⇒ Some(s)
  }

  implicit def stringToIntOpt(s: String) = try {
    Some(s.toInt)
  }
  catch {
    case e: NumberFormatException ⇒ None
  }

  implicit def stringToInt(s: String) = try {
    s.toInt
  }
  catch {
    case e: NumberFormatException ⇒ 0
  }

  implicit def intToString(i: Option[Int]) = i match {
    case Some(ii: Int) ⇒ ii.toString
    case _             ⇒ ""
  }

  val userTextField = new TextField(pud.user, 15)
  val hostTextField = new TextField(pud.host, 15)
  val portTextField = new TextField(pud.port.toString, 4)
  val queueTextField = new TextField(pud.queue.getOrElse(""), 15)
  val coreTextField = new TextField(pud.core, 4)
  val cpuTextField = new TextField(pud.cpu, 4)
  val wallTimeTextField = new TextField(pud.wallTime.getOrElse(""), 4)
  val openMOLEMemoryTextField = new TextField(pud.openMOLEMemory, 4)
  val workdirTextField = new TextField(pud.workDirectory.getOrElse(""), 15)
  val threadsTextField = new TextField(pud.threads, 4)

  val components = List(("Settings",
    new PluginPanel("wrap 2") {
      contents += (new Label("User"), "gap para")
      contents += userTextField

      contents += (new Label("Host"), "gap para")
      contents += hostTextField

      contents += (new Label("Port"), "gap para")
      contents += portTextField
    }), ("Options",
    new PluginPanel("wrap 2") {
      minimumSize = new Dimension(300, 300)

      contents += (new Label("Queue"), "gap para")
      contents += queueTextField

      contents += (new Label("Cores"), "gap para")
      contents += coreTextField

      contents += (new Label("CPU"), "gap para")
      contents += cpuTextField

      contents += (new Label("Wall time"), "gap para")
      contents += wallTimeTextField

      contents += (new Label("OpenMOLE memory"), "gap para")
      contents += openMOLEMemoryTextField

      contents += (new Label("Workdir"), "gap para")
      contents += workdirTextField

      contents += (new Label("Threads"), "gap para")
      contents += threadsTextField

    }))

  override lazy val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink"))))
  //  requirementsPanelUI.requirementHelp.foreach { hm ⇒ add(hm._1, hm._2) }
  add(userTextField, new Help(i18n.getString("login"), i18n.getString("loginEx")))
  add(hostTextField, new Help(i18n.getString("host"), i18n.getString("hostEx")))
  add(workdirTextField, new Help(i18n.getString("category"), i18n.getString("dirEx")))
  add(queueTextField, new Help(i18n.getString("queue"), i18n.getString("queueEx")))
  add(openMOLEMemoryTextField, new Help(i18n.getString("runtimeMemory"), i18n.getString("runtimeMemoryEx")))

  override def saveContent(name: String) =
    new OAREnvironmentDataUI(name,
      userTextField.text,
      hostTextField.text,
      portTextField.text,
      queueTextField.text,
      coreTextField.text,
      cpuTextField.text,
      wallTimeTextField.text,
      openMOLEMemoryTextField.text,
      workdirTextField.text,
      threadsTextField.text)
}
