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

package org.openmole.ide.plugin.sampling.csv

import scala.swing._
import scala.swing.Label
import swing.Swing._
import swing.ListView._
import au.com.bytecode.opencsv.CSVReader
import java.io.File
import java.io.FileReader
import org.openmole.ide.core.model.panel.ISamplingPanelUI
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.core.implementation.data.EmptyDataUIs
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import org.openmole.ide.misc.widget.CSVChooseFileTextField
import org.openmole.ide.misc.widget.DialogClosedEvent
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.URL
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos._
import scala.swing.BorderPanel.Position._

class CSVSamplingPanelUI(pud: CSVSamplingDataUI) extends PluginPanel("wrap") with ISamplingPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val csvTextField = new CSVChooseFileTextField(pud.csvFilePath)
  var comboMulti: Option[MultiTwoCombos[String, IPrototypeDataProxyUI]] = None

  contents += new Label("CSV file")
  contents += csvTextField
  contents += comboMulti.getOrElse(new Label("<html><i>Nothing to be mapped yet</html></i>"))

  readFile(pud.csvFilePath)

  listenTo(csvTextField)
  reactions += {
    case DialogClosedEvent(csvTextField) ⇒ readFile(csvTextField.text)
  }

  def readFile(s: String) = {
    if (new File(s).isFile) {
      val reader = new CSVReader(new FileReader(s))
      val headers = reader.readNext
      comboMulti =
        Some(new MultiTwoCombos(
          "Map columns to prototypes",
          headers.toList,
          comboContent,
          "with",
          pud.prototypeMapping.map {
            pm ⇒
              new TwoCombosPanel(
                headers.toList,
                comboContent,
                "with",
                new TwoCombosData(Some(pm._1), Some(pm._2)))
          }))
      help.add(comboMulti.get, new Help(i18n.getString("mapping")))
      contents.remove(2)
      contents += comboMulti.get.panel
      reader.close
    }
  }

  override def saveContent = {
    if (comboMulti.isDefined)
      new CSVSamplingDataUI(csvTextField.text,
        comboMulti.get.content.map {
          c ⇒ (c.comboValue1.get, c.comboValue2.get)
        }, pud.id)
    else new CSVSamplingDataUI(csvTextField.text, List[(String, PrototypeDataProxyUI)](), pud.id)
  }

  def comboContent: List[IPrototypeDataProxyUI] = EmptyDataUIs.emptyPrototypeProxy :: Proxys.prototypes.toList

  override lazy val help =
    new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink")))) {
      add(csvTextField, new Help(i18n.getString("csvPath"), i18n.getString("csvPathEx")))
    }
}
