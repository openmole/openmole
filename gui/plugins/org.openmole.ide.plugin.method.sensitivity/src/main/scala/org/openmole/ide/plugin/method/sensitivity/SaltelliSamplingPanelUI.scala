/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.method.sensitivity

import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.core.implementation.data.FactorDataUI
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.core.model.panel.ISamplingPanelUI
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.URL
import scala.swing.Label
import scala.swing.TabbedPane
import scala.swing.TextField

class SaltelliSamplingPanelUI(cud: SaltelliSamplingDataUI) extends PluginPanel("wrap 2", "", "") with ISamplingPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val sampleTextField = new TextField(cud.samples, 4)

  tabbedPane.pages += new TabbedPane.Page("Settings",
    new PluginPanel("wrap 2") {
      contents += new Label("Number of samples")
      contents += sampleTextField
    })

  def domains = KeyRegistry.domains.values.map { _.buildDataUI }.toList

  override def saveContent = new SaltelliSamplingDataUI(sampleTextField.text, cud.id)

  override val help = new Helper(List(new URL(i18n.getString("samplingPermalinkText"),
    i18n.getString("samplingPermalink")),
    new URL(i18n.getString("samplingPermalink1Text"),
      i18n.getString("samplingPermalink1")))) {
    add(sampleTextField, new Help(i18n.getString("sample"), i18n.getString("sampleEx")))
  }
}