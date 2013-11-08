/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.csv

import java.io.File
import org.openmole.misc.exception.UserBadDataError
import org.openmole.plugin.sampling.csv.CSVSampling
import org.openmole.core.model.sampling.{ Factor, Sampling }
import org.openmole.ide.core.implementation.dialog.StatusBar
import org.openmole.ide.core.implementation.data.{ SamplingDataUI, DomainDataUI }
import org.openmole.ide.core.implementation.dataproxy.PrototypeDataProxyUI
import org.openmole.ide.core.implementation.serializer.Update

class CSVSamplingDataUI010(val csvFilePath: String = "",
                           val separator: Char = ',',
                           val prototypeMapping: List[(String, PrototypeDataProxyUI)] = List.empty) extends SamplingDataUI {
  def name = "CSV"

  def coreObject(factorOrSampling: List[Either[(Factor[_, _], Int), (Sampling, Int)]]) = util.Try {
    try {
      val fi = new File(csvFilePath)
      val sampling = CSVSampling(fi, separator)
      prototypeMapping.foreach {
        m ⇒ sampling addColumn (m._1, m._2.dataUI.coreObject.get)
      }
      sampling
    }
    catch {
      case e: Throwable ⇒ throw new UserBadDataError("CSV file path is not correct for the CSV Sampling")
    }
  }

  def coreClass = classOf[CSVSampling]

  override def imagePath = "img/csvSampling.png"

  override def fatImagePath = "img/csvSampling_fat.png"

  def buildPanelUI = new CSVSamplingPanelUI(this)

  override def isAcceptable(factor: DomainDataUI) = {
    StatusBar().warn("A CSV Sampling does not accept any Domain or Sampling as input")
    false
  }

  def isAcceptable(sampling: SamplingDataUI) = {
    StatusBar().warn("A CSV Sampling does not accept any Domain or Sampling as input")
    false
  }

  def preview = "from " + {
    val n = new File(csvFilePath).getName
    if (n.isEmpty) "CSV file"
    else n
  }
}

class CSVSamplingDataUI extends Update[CSVSamplingDataUI010] {
  def update = new CSVSamplingDataUI010
}
