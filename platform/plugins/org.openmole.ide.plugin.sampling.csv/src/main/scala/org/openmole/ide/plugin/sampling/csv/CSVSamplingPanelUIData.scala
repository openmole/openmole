/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.csv


 import org.openmole.ide.core.properties.PanelUIData

class CSVSamplingPanelUIData(var csvFilePath:String) extends PanelUIData {
  def this() = this("csv path")
}
