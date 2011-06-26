/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.csv


import java.io.File
import org.openmole.ide.core.exception.GUIUserBadDataError
import org.openmole.ide.core.palette.PrototypeDataProxyUI
import org.openmole.ide.core.properties.ISamplingDataUI
import org.openmole.plugin.sampling.csv.CSVSampling
import org.openmole.ide.core.properties.EmptyDataUIs._

class CSVSamplingDataUI(val name: String,var csvFilePath: String, var prototypeMapping: Map[String,PrototypeDataProxyUI]) extends ISamplingDataUI {
  def this(n:String) = this(n,"",Map())
  
  override def coreObject = {  
    if (csvFilePath != "") {
      val fi = new File(csvFilePath)
      if (fi.isFile) new CSVSampling(fi) {
        prototypeMapping.filter(!_._2.isInstanceOf[EmptyPrototypeDataUI]).foreach{m=> addColumnAs(m._2.dataUI.coreObject,m._1)}
      }
      else throw new GUIUserBadDataError("CSV file " + csvFilePath + " does not exist")
    }
    else throw new GUIUserBadDataError("CSV file path missing to instanciate the CSV sampling " + name)
  }

  override def coreClass = classOf[CSVSampling] 
  
  override def imagePath = "img/thumb/csvSampling.png" 
  
//  override def buildPanelUI = new CSVSamplingPanelUI(this)
  override def buildPanelUI = new CSVSamplingPanelUI(this)
}
