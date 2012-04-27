/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.method.sensitivity

import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.core.model.panel.ISamplingPanelUI
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.plugin.sampling.tools.GenericBoundedSamplingPanel
import scala.swing.ComboBox
import scala.swing.Label
import scala.swing.TextField

class SaltelliSamplingPanelUI(cud: SaltelliSamplingDataUI) extends PluginPanel("wrap 2","","") with ISamplingPanelUI {
  
  val sampleTextField = new TextField(cud.samples,4) 
  val stringPrototypesComboBox = new ComboBox(Proxys.stringPrototypes)
   val panel = new GenericBoundedSamplingPanel(cud.factors, KeyRegistry.boundedDomains.map{_._2.displayName}.toList)
  
  contents += new Label("Number of samples")
  contents += sampleTextField
  contents += new Label("Matrix name")
  contents += stringPrototypesComboBox
  contents += panel 
  
  
  override def saveContent(name: String) = new SaltelliSamplingDataUI(name,
                                                                 sampleTextField.text,
                                                                 stringPrototypesComboBox.selection.item,
                                                                 panel.factors)
}