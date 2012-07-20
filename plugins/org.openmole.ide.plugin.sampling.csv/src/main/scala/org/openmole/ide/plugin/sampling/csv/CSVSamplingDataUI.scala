/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.csv

import java.io.File
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.model.dataproxy.IPrototypeDataProxyUI
import org.openmole.ide.core.model.data.ISamplingDataUI
import org.openmole.plugin.sampling.csv.CSVSampling
import org.openmole.ide.core.implementation.data.EmptyDataUIs._

class CSVSamplingDataUI(val name: String = "",
                        var csvFilePath: String = "",
                        var prototypeMapping: List[(String, IPrototypeDataProxyUI)] = List.empty) extends ISamplingDataUI {

  println("data " + csvFilePath)
  println("data " + prototypeMapping)

  def coreObject = {
    if (csvFilePath != "") {
      val fi = new File(csvFilePath)
      if (fi.isFile) {
        val sampling = CSVSampling(fi)
        prototypeMapping.filter(!_._2.dataUI.isInstanceOf[EmptyPrototypeDataUI]).foreach { m ⇒ sampling addColumn (m._1, m._2.dataUI.coreObject) }
        sampling
      } else throw new UserBadDataError("CSV file " + csvFilePath + " does not exist")
    } else throw new UserBadDataError("CSV file path missing to instanciate the CSV sampling " + name)
  }

  def coreClass = classOf[CSVSampling]

  def imagePath = "img/csvSampling.png"

  override def fatImagePath = "img/csvSampling_fat.png"

  def buildPanelUI = new CSVSamplingPanelUI(this)
}
