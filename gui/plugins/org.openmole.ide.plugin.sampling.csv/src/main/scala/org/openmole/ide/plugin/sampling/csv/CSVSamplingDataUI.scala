/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.csv

import java.io.File
import org.openmole.misc.exception.UserBadDataError
import org.openmole.ide.core.model.dataproxy._
import org.openmole.ide.core.model.data._
import org.openmole.plugin.sampling.csv.CSVSampling
import org.openmole.ide.core.implementation.data.EmptyDataUIs._
import org.openmole.core.model.sampling.{ Factor, Sampling }
import org.openmole.ide.core.implementation.dialog.StatusBar

class CSVSamplingDataUI(val csvFilePath: String = "",
                        val prototypeMapping: List[(String, IPrototypeDataProxyUI)] = List.empty) extends ISamplingDataUI {
  def name = "CSV"

  def coreObject(factorOrSampling: List[Either[(Factor[_, _], Int), (Sampling, Int)]]) = try {
    val fi = new File(csvFilePath)
    val sampling = CSVSampling(fi)
    prototypeMapping.filter(!_._2.dataUI.isInstanceOf[EmptyPrototypeDataUI]).foreach {
      m ⇒ sampling addColumn (m._1, m._2.dataUI.coreObject)
    }
    sampling
  } catch {
    case e: Throwable ⇒ throw new UserBadDataError("CSV file path is not correct for the CSV Sampling")
  }

  def coreClass = classOf[CSVSampling]

  def imagePath = "img/csvSampling.png"

  override def fatImagePath = "img/csvSampling_fat.png"

  def buildPanelUI = new CSVSamplingPanelUI(this)

  override def isAcceptable(factor: IDomainDataUI) = {
    StatusBar().warn("A CSV Sampling does not accept any Domain or Sampling as input")
    false
  }

  def isAcceptable(sampling: ISamplingDataUI) = {
    StatusBar().warn("A CSV Sampling does not accept any Domain or Sampling as input")
    false
  }

  def preview = "from " + {
    val n = new File(csvFilePath).getName
    if (n.isEmpty) "CSV file"
    else n
  }
}
