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
import org.openmole.ide.core.model.panel.IEnvironmentPanelUI
import org.openmole.ide.misc.widget._
import swing._
import event._
import event.ButtonClicked
import org.openmole.plugin.environment.glite.GliteAuthentication
import scala.Some

object GliteEnvironmentPanelUI {
  lazy val vomses = {
    val x = xml.XML.loadFile(GliteAuthentication.voCards)
    val cards = (x \ "IDCard")
    val names = cards map (_.attribute("Name").get.text)
    val urls = cards map (c ⇒ (c \ "EnrollmentUrl").head.text)
    names zip urls
  }.toMap
}

class GliteEnvironmentPanelUI(pud: GliteEnvironmentDataUI) extends PluginPanel("fillx", "[left][grow,fill]", "") with IEnvironmentPanelUI {

  implicit def intToString(i: Option[Int]) = i match {
    case Some(ii: Int) ⇒ ii.toString
    case _             ⇒ ""
  }

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

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val voComboBox = new MyComboBox[String]("" :: GliteEnvironmentPanelUI.vomses.keys.toList.sorted)
  voComboBox.selection.item = pud.vo

  val vomsTextField = new TextField(pud.voms, 20)
  val bdiiTextField = new TextField(pud.bdii, 20)
  var enrollmentURLLink = enrollmentLink
  val enrollmentURLLabel = new Label("")
  val runtimeMemoryTextField = new TextField(pud.openMOLEMemory, 4)

  val proxyTimeTextField = new TextField(pud.proxyTime.getOrElse(""), 18)
  val proxyTimeLabel = new Label("Time")
  val proxyHostTextField = new TextField(pud.proxyHost.getOrElse(""), 18)
  val proxyHostLabel = new Label("Host")
  val proxyPortTextField = new TextField(pud.proxyPort, 18)
  val proxyPortLabel = new Label("(Port)")

  val fqanTextField = new TextField(pud.fqan.getOrElse(""), 4)
  val workerNodeMemoryTextField = new TextField(pud.openMOLEMemory, 4)
  val memoryTextField = new TextField(pud.memory, 4)
  val maxCPUTimeTextField = new TextField(pud.cpuTime.getOrElse(""), 4)
  val wallTimeTextField = new TextField(pud.wallTime.getOrElse(""), 4)
  val cPUNumberTextField = new TextField(pud.cpuNumber, 4)
  val jobTypeTextField = new TextField(pud.jobType.getOrElse(""), 4)
  val smpGranularityTextField = new TextField(pud.smpGranularity, 4)
  val threadsTextField = new TextField(pud.threads, 4)
  val architectureCheckBox = new CheckBox("64 bits") {
    selected = pud.architecture
  }

  val proxyCheckBox = new CheckBox("MyProxy")

  listenTo(`proxyCheckBox`)

  reactions += {
    case ButtonClicked(`proxyCheckBox`) ⇒ showProxy(proxyCheckBox.selected)
  }

  voComboBox.selection.reactions += {
    case SelectionChanged(`voComboBox`) ⇒
      vomsTextField.text = GliteAuthentication.getVOMS(voComboBox.selection.item).getOrElse("")
      enrollmentURLLabel.text = ""
      enrollmentURLLink = enrollmentLink
  }

  val components = List(("Settings",
    new PluginPanel("wrap 2") {
      contents += (new Label("VO"), "gap para")
      contents += voComboBox
      contents += (new Label("VOMS"), "gap para")
      contents += vomsTextField
      contents += (new Label("BDII"), "gap para")
      contents += bdiiTextField
      contents += (new Label("Runtime memory"), "gap para")
      contents += runtimeMemoryTextField
      contents += enrollmentURLLink
      contents += enrollmentURLLabel
    }), ("Options",
    new PluginPanel("wrap 2") {
      contents += new PluginPanel("wrap 2") {
        contents += (new Label("Fqan"), "gap para")
        contents += fqanTextField
        contents += (new Label("Worker memory"), "gap para")
        contents += workerNodeMemoryTextField
        contents += (new Label("Memory"), "gap para")
        contents += memoryTextField
        contents += (new Label("Max CPU Time"), "gap para")
        contents += maxCPUTimeTextField
        contents += (new Label("Wall Time"), "gap para")
        contents += wallTimeTextField
      }
      contents += new PluginPanel("wrap 2") {
        contents += (new Label("CPU Number"), "gap para")
        contents += cPUNumberTextField
        contents += (new Label("Job type"), "gap para")
        contents += jobTypeTextField
        contents += (new Label("SMP Granularity"), "gap para")
        contents += smpGranularityTextField
        contents += (new Label("Threads"), "gap para")
        contents += threadsTextField
      }
    }), ("MyProxy", new PluginPanel("") {
    contents += (proxyCheckBox, "wrap")
    contents += (proxyTimeLabel, "gap para")
    contents += (proxyTimeTextField, "wrap")
    contents += (proxyHostLabel, "gap para")
    contents += (proxyHostTextField, "wrap")
    contents += (proxyPortLabel, "gap para")
    contents += proxyPortTextField
  }))

  proxyCheckBox.selected = pud.proxy
  showProxy(pud.proxy)

  private def enrollmentLink =
    new LinkLabel("VO enrollment",
      new Action("") {
        def apply = {
          enrollmentURLLabel.text = GliteEnvironmentPanelUI.vomses.getOrElse(voComboBox.selection.item, "")
        }
      },
      3,
      "#73a5d2",
      false
    )

  private def showProxy(b: Boolean) = {
    List(proxyTimeLabel, proxyTimeTextField, proxyHostLabel, proxyHostTextField, proxyPortLabel, proxyPortTextField).foreach {
      _.visible = b
    }
  }

  override val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink")))) {
    add(voComboBox, new Help(i18n.getString("vo"), i18n.getString("voEx")))
    add(vomsTextField, new Help(i18n.getString("voms"), i18n.getString("vomsEx")))
    add(bdiiTextField, new Help(i18n.getString("bdii"), i18n.getString("bdiiEx")))
    add(proxyCheckBox, new Help(i18n.getString("runtimeMemory"), i18n.getString("runtimeMemoryEx")))
    add(runtimeMemoryTextField, new Help(i18n.getString("myProxy")))
  }

  def saveContent(name: String) =
    new GliteEnvironmentDataUI(name,
      voComboBox.selection.item,
      vomsTextField.text,
      bdiiTextField.text,
      proxyCheckBox.selected,
      proxyTimeTextField.text,
      proxyHostTextField.text,
      proxyPortTextField.text,
      fqanTextField.text,
      workerNodeMemoryTextField.text,
      memoryTextField.text,
      cPUNumberTextField.text,
      wallTimeTextField.text,
      cPUNumberTextField.text,
      jobTypeTextField.text,
      smpGranularityTextField.text,
      architectureCheckBox.selected,
      threadsTextField.text)
}
