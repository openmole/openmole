/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.csv


import java.io.File
import org.openmole.ide.core.implementation.exception.GUIUserBadDataError
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.model.data.ISamplingDataUI
import org.openmole.plugin.sampling.csv.CSVSampling
import org.openmole.ide.core.implementation.data.EmptyDataUIs._

class CSVSamplingDataUI(val name: String,var csvFilePath: String, var prototypeMapping: Map[String,PrototypeDataProxyUI]) extends ISamplingDataUI {
  def this(n:String) = this(n,"",Map())
  
  override def coreObject = {  
    if (csvFilePath != "") {
      val fi = new File(csvFilePath)
      if (fi.isFile) new CSVSampling(fi) {
//        prototypeMapping.foreach{m=> m._2.dataUI match {
//            case x:EmptyPrototypeDataUI =>
//            case _=> addColumnAs(m._1,m._2.dataUI.coreObject)
//          }
//        }
        
        prototypeMapping.filter(!_._2.dataUI.isInstanceOf[EmptyPrototypeDataUI]).foreach{m=>addColumnAs(m._1,m._2.dataUI.coreObject)}
      }
      else throw new GUIUserBadDataError("CSV file " + csvFilePath + " does not exist")
    }
    else throw new GUIUserBadDataError("CSV file path missing to instanciate the CSV sampling " + name)
  }

  override def coreClass = classOf[CSVSampling] 
  
  override def imagePath = "img/csvSampling.png" 
  
//  override def buildPanelUI = new CSVSamplingPanelUI(this)
  override def buildPanelUI = new CSVSamplingPanelUI(this)
}
