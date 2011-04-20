/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.sampling.csv

import org.openmole.plugin.sampling.csv.CSVSampling
import org.openmole.ide.core.properties.PanelUI

class CSVSamplingPanelUI extends PanelUI{
  override def name= "toto"
  
  override def entityType = classOf[CSVSampling] 
}
