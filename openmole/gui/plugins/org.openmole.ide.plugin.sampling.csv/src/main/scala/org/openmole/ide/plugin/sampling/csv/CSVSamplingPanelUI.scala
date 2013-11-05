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
import au.com.bytecode.opencsv.CSVReader
import java.io.File
import java.io.FileReader
import org.openmole.ide.core.implementation.dataproxy.{ PrototypeDataProxyUI, Proxies }
import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.misc.widget.CSVChooseFileTextField
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.URL
import org.openmole.ide.misc.widget.multirow.MultiWidget._
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos._
import org.openmole.ide.core.implementation.panelsettings.ISamplingPanelUI
import org.openmole.ide.misc.tools.util.Converters._
import org.openmole.ide.misc.widget.DialogClosedEvent
import scala.Some
import org.openmole.ide.misc.tools.image.Images

class CSVSamplingPanelUI(pud: CSVSamplingDataUI2)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends ISamplingPanelUI with Publisher {

  val csvTextField = new CSVChooseFileTextField(pud.csvFilePath)
  val separatorTextField = new TextField(pud.separator.toString, 3)
  var comboMulti: Option[MultiTwoCombos[String, PrototypeDataProxyUI]] = None

  val csvpanel = new PluginPanel("wrap 2", "[grow]", "") {
    contents += new Label("CSV file")
    contents += csvTextField
    contents += new Label("Separator")
    contents += new PluginPanel("wrap 2") {
      contents += separatorTextField
      contents += new Button(new Action("") {
        icon = Images.REFRESH
        def apply = readFile(csvTextField.text)
      })
    }
    contents += comboMulti.getOrElse(new Label("<html><i>Nothing to be mapped yet</html></i>"))
  }
  val components = List(("", csvpanel))

  readFile(pud.csvFilePath)

  listenTo(csvTextField, separatorTextField)
  reactions += {
    case DialogClosedEvent(csvTextField) ⇒ readFile(csvTextField.text)
  }

  def readFile(s: String): Unit = {
    if (new File(s).isFile) {
      val reader = new CSVReader(new FileReader(s), separatorTextField.text.head)
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
          }, CLOSE_IF_EMPTY))
      help.add(comboMulti.get, new Help(i18n.getString("mapping")))
      csvpanel.contents.remove(4)
      csvpanel.contents += comboMulti.get.panel
      reader.close
    }
  }

  override def saveContent = {
    val csv = csvTextField.text
    val sep = separatorTextField.text.head
    if (comboMulti.isDefined)
      new CSVSamplingDataUI2(csv,
        sep,
        comboMulti.get.content.map {
          c ⇒ (c.comboValue1, c.comboValue2)
        })
    else new CSVSamplingDataUI2(csv, sep, List[(String, PrototypeDataProxyUI)]())
  }

  def comboContent: List[PrototypeDataProxyUI] = Proxies.instance.prototypes.toList

  override lazy val help = new Helper(List(new URL(i18n.getString("permalinkText"), i18n.getString("permalink"))))
  add(csvTextField, new Help(i18n.getString("csvPath"), i18n.getString("csvPathEx")))

}
