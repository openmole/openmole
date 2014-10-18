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

package org.openmole.ide.plugin.source.file

import org.openmole.ide.core.implementation.dataproxy.{ Proxies, PrototypeDataProxyUI }
import swing.Label
import au.com.bytecode.opencsv.CSVReader
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.URL
import org.openmole.ide.misc.widget.{ Help, DialogClosedEvent, CSVChooseFileTextField, PluginPanel }
import java.util.{ Locale, ResourceBundle }
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos
import java.io.{ FileReader, File }
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos.{ TwoCombosData, TwoCombosPanel }
import org.openmole.ide.core.implementation.panelsettings.SourcePanelUI
import org.openmole.ide.misc.tools.util.Converters
import org.openmole.ide.misc.tools.util.Converters._

class CSVSourcePanelUI(dataUI: CSVSourceDataUI010)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends PluginPanel("wrap") with SourcePanelUI {

  val csvTextField = new CSVChooseFileTextField(dataUI.csvFilePath)
  var comboMulti = new MultiTwoCombos[String, PrototypeDataProxyUI]("", List(), List(), "with", Seq())

  val mainPanel = new PluginPanel("wrap") {
    contents += new PluginPanel("wrap 2") {
      contents += new Label("CSV file")
      contents += (csvTextField, "span,growx")
    }
    contents += comboMulti
    //preferredSize = new Dimension(250, 100)
  }
  readFile(dataUI.csvFilePath)

  val components = List(("Settings", mainPanel))

  listenTo(csvTextField)
  reactions += {
    case DialogClosedEvent(csvTextField) ⇒ readFile(csvTextField.text)
  }

  def readFile(s: String) = {
    if (new File(s).isFile) {
      val reader = new CSVReader(new FileReader(s))
      val headers = reader.readNext
      comboMulti = new MultiTwoCombos(
        "Map columns to prototypes",
        headers.toList,
        comboContent,
        "with",
        dataUI.prototypeMapping.map {
          pm ⇒
            new TwoCombosPanel(
              headers.toList,
              comboContent,
              "with",
              new TwoCombosData(Some(pm._1), Some(pm._2)))
        })
      help.add(comboMulti, new Help(i18n.getString("mapping")))
      mainPanel.contents.remove(1)
      mainPanel.contents += comboMulti.panel
      reader.close
    }
  }

  def saveContent(name: String) = new CSVSourceDataUI010(name,
    csvTextField.text,
    Converters.flattenTuple2Options(comboMulti.content.map {
      c ⇒ (c.comboValue1, c.comboValue2)
    }))

  def comboContent: List[PrototypeDataProxyUI] = Proxies.instance.prototypes.toList

  override lazy val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink"))))

  add(csvTextField, new Help(i18n.getString("csvPath"), i18n.getString("csvPathEx")))

}