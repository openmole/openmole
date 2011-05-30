/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.csv


import java.io.File
import org.openmole.ide.core.commons.Constants
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.properties.PanelUIData
import org.openmole.plugin.sampling.csv.CSVSampling

class CSVSamplingPanelUIData(name: String,var csvFilePath: String) extends PanelUIData(name,Constants.SAMPLING) {
  def this(n:String) = this(n,"")
  
  override def coreObject = {  
    if (csvFilePath != "") {
      val fi = new File(csvFilePath)
      if (fi.isFile) new CSVSampling(fi)
      else throw new GUIUserBadDataError("CSV file " + csvFilePath + " does not exist")
    }
    else throw new GUIUserBadDataError("CSV file path missing to instanciate the CSV sampling " + name)
  }

  override def coreClass = classOf[CSVSampling] 
  
  override def imagePath = "img/thumb/csvSampling.png" 
  
  override def buildPanelUI = new CSVSamplingPanelUI
}
