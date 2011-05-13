/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.plugin.sampling.csv

import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.properties.PanelUIData
import org.openmole.plugin.sampling.csv.CSVSampling
import java.io.File
import org.openmole.ide.core.properties.ISamplingFactoryUI
import org.openmole.ide.core.properties.PanelUI

class CSVSamplingFactoryUI extends ISamplingFactoryUI {
//class CSVSamplingFactoryUI extends SamplingFactoryUI(new CSVSamplingPanelUIData) {
  var panelData = new CSVSamplingPanelUIData
  
  override def panelUIData = panelData
  
  override def coreObject= {
    if (panelData.csvFilePath != "") {
      val fi = new File(panelData.csvFilePath)
      if (fi.isFile) new CSVSampling(fi)
      else throw new GUIUserBadDataError("CSV file " + panelData.csvFilePath + " does not exist")
    }
    else throw new GUIUserBadDataError("CSV file path missing to instanciate the CSV sampling " + panelData.name)
  }

  override def coreClass = classOf[CSVSampling] 
  
  override def imagePath = "img/thumb/csvSampling.png" 
  
  override def buildPanelUI = new CSVSamplingPanelUI(panelData)
}
