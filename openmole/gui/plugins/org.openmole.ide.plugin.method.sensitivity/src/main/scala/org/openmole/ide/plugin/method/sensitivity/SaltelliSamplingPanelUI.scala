/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.method.sensitivity

import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.URL
import scala.swing.Label
import scala.swing.TabbedPane
import scala.swing.TextField
import org.openmole.ide.core.implementation.panelsettings.ISamplingPanelUI

class SaltelliSamplingPanelUI(cud: SaltelliSamplingDataUI)(implicit val i18n: ResourceBundle = ResourceBundle.getBundle("help", new Locale("en", "EN"))) extends ISamplingPanelUI {

  val sampleTextField = new TextField(cud.samples, 4)

  val components = List(("", new PluginPanel("wrap 2", "", "") {
    contents += new Label("Number of samples")
    contents += sampleTextField
  }))

  def domains = KeyRegistry.domains.values.map {
    _.buildDataUI
  }.toList

  override def saveContent = new SaltelliSamplingDataUI(sampleTextField.text)

  override lazy val help = new Helper(List(new URL(i18n.getString("saltelliPermalinkText"),
    i18n.getString("saltelliPermalink")),
    new URL(i18n.getString("saltelliPermalink1Text"),
      i18n.getString("saltelliPermalink1"))))

  add(sampleTextField, new Help(i18n.getString("sample"), i18n.getString("sampleEx")))

}