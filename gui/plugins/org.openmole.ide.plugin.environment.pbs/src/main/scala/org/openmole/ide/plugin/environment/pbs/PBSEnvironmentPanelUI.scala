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

package org.openmole.ide.plugin.environment.pbs

import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.core.model.panel.IEnvironmentPanelUI
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.URL
import scala.swing.Label
import scala.swing.TabbedPane
import scala.swing.TextField
import swing.TabbedPane.Page

class PBSEnvironmentPanelUI(pud: PBSEnvironmentDataUI) extends PluginPanel("fillx,wrap 2", "", "") with IEnvironmentPanelUI {

  implicit def stringToStringOpt(s: String) = s.isEmpty match {
    case true ⇒ None
    case false ⇒ Some(s)
  }

  implicit def stringToIntOpt(s: String) = try {
    Some(s.toInt)
  } catch {
    case e: NumberFormatException ⇒ None
  }

  implicit def stringToInt(s: String) = try {
    s.toInt
  } catch {
    case e: NumberFormatException ⇒ 0
  }

  implicit def intToString(i: Option[Int]) = i match {
    case Some(ii: Int) ⇒ ii.toString
    case _ ⇒ ""
  }

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val loginTextField = new TextField(pud.login, 15)
  val hostTextField = new TextField(pud.host, 15)
  val portTextField = new TextField(pud.port.toString, 4)
  val pathTextField = new TextField(pud.path.getOrElse(""), 15)
  val queueTextField = new TextField(pud.queue.getOrElse(""), 15)
  val openMOLEMemoryTextField = new TextField(pud.openMOLEMemory, 4)
  val wallTimeTextField = new TextField(pud.wallTime.getOrElse(""), 4)
  val memoryTextField = new TextField(pud.memory, 4)
  val threadsTextField = new TextField(pud.threads, 4)
  val nodesTextField = new TextField(pud.nodes, 4)
  val coreByNodeTextField = new TextField(pud.coreByNode, 4)

  tabbedPane.pages += new TabbedPane.Page("Settings",
    new PluginPanel("wrap 2") {
      contents += (new Label("Login"), "gap para")
      contents += loginTextField

      contents += (new Label("Host"), "gap para")
      contents += hostTextField

      contents += (new Label("Port"), "gap para")
      contents += portTextField
    })

  tabbedPane.pages += new TabbedPane.Page("Options",
    new PluginPanel("wrap 2") {
      contents += new PluginPanel("wrap 2") {
        contents += (new Label("Path"), "gap para")
        contents += pathTextField

        contents += (new Label("Queue"), "gap para")
        contents += queueTextField

        contents += (new Label("OpenMOLE memory"), "gap para")
        contents += openMOLEMemoryTextField

        contents += (new Label("Memory"), "gap para")
        contents += memoryTextField
      }
      contents += new PluginPanel("wrap 2") {
        contents += (new Label("Wall time"), "gap para")
        contents += wallTimeTextField

        contents += (new Label("Threads"), "gap para")
        contents += threadsTextField

        contents += (new Label("Nodes"), "gap para")
        contents += nodesTextField

        contents += (new Label("Cores by Node"), "gap para")
        contents += coreByNodeTextField

      }
    })

  contents += tabbedPane

  override val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink")))) {
    //  requirementsPanelUI.requirementHelp.foreach { hm ⇒ add(hm._1, hm._2) }
    add(loginTextField, new Help(i18n.getString("login"), i18n.getString("loginEx")))
    add(hostTextField, new Help(i18n.getString("host"), i18n.getString("hostEx")))
    add(pathTextField, new Help(i18n.getString("dir"), i18n.getString("dirEx")))
    add(queueTextField, new Help(i18n.getString("queue"), i18n.getString("queueEx")))
    add(openMOLEMemoryTextField, new Help(i18n.getString("runtimeMemory"), i18n.getString("runtimeMemoryEx")))
  }

  override def saveContent(name: String) =
    new PBSEnvironmentDataUI(name,
      loginTextField.text,
      hostTextField.text,
      portTextField.text,
      queueTextField.text,
      openMOLEMemoryTextField.text,
      wallTimeTextField.text,
      memoryTextField.text,
      pathTextField.text,
      threadsTextField.text,
      nodesTextField.text,
      coreByNodeTextField.text)
}
