/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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
import org.openmole.ide.core.model.display._
import org.openmole.ide.core.model.panel.ISamplingPanelUI
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import org.openmole.ide.misc.widget.CSVChooseFileTextField
import org.openmole.ide.misc.widget.DialogClosedEvent
import org.openmole.ide.misc.widget.MigPanel
import org.openmole.ide.misc.widget.multirow.MultiTwoCombos
import scala.swing.BorderPanel.Position._

class CSVSamplingPanelUI(pud: CSVSamplingDataUI) extends MigPanel("","[][grow,fill]","") with ISamplingPanelUI {
  
  val csvTextField = new CSVChooseFileTextField(pud.csvFilePath)
  var comboMulti: Option[MultiTwoCombos[String,IPrototypeDataProxyUI]]= None
  
  contents+= new Label("CSV file")
  contents+= (csvTextField,"wrap")
  
  readFile(pud.csvFilePath)
  
  listenTo(csvTextField)
  reactions += {
    case DialogClosedEvent(csvTextField)=> readFile(csvTextField.text)}
  
  
  def readFile(s: String) = {
    if (new File(s).isFile){
      val reader = new CSVReader(new FileReader(s))
      val headers = reader.readNext
      comboMulti = 
        Some(new MultiTwoCombos[String,IPrototypeDataProxyUI](
          "Map columns to prototypes",
          "with",
          (headers.toList, comboContent),
           pud.prototypeMapping))
      if (contents.size == 3) contents.remove(2)
      contents+= (comboMulti.get.panel,"span,grow,wrap")
      reader.close}}
  
  override def saveContent(name: String) = {
    if (comboMulti.isDefined)
      new CSVSamplingDataUI(name,
                            csvTextField.text,
                            comboMulti.get.content)
    else new CSVSamplingDataUI(name,csvTextField.text, List[(String,PrototypeDataProxyUI)]())}
  
  def comboContent: List[IPrototypeDataProxyUI] = new PrototypeDataProxyUI(new EmptyPrototypeDataUI)::Proxys.prototype.toList
}
