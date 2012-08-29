/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.plugin.method.sensitivity

import java.util.Locale
import java.util.ResourceBundle
import org.openmole.ide.core.implementation.dataproxy.BoundedDomainDataProxyFactory
import org.openmole.ide.core.implementation.dataproxy.Proxys
import org.openmole.ide.core.implementation.registry.KeyRegistry
import org.openmole.ide.core.model.panel.ISamplingPanelUI
import org.openmole.ide.misc.widget.Help
import org.openmole.ide.misc.widget.Helper
import org.openmole.ide.misc.widget.PluginPanel
import org.openmole.ide.misc.widget.URL
import org.openmole.ide.plugin.sampling.tools.MultiGenericBoundedSamplingPanel
import org.openmole.ide.plugin.sampling.tools.MultiGenericBoundedSamplingPanel._
import scala.swing.Label
import scala.swing.TabbedPane
import scala.swing.TextField

class SaltelliSamplingPanelUI(cud: SaltelliSamplingDataUI) extends PluginPanel("wrap 2", "", "") with ISamplingPanelUI {

  val i18n = ResourceBundle.getBundle("help", new Locale("en", "EN"))

  val sampleTextField = new TextField(cud.samples, 4)
  val multiPanel = new MultiGenericBoundedSamplingPanel(Proxys.prototypes.toList,
    domains,
    cud.factors.map { f ⇒
      new GenericBoundedSamplingPanel(Proxys.prototypes.toList,
        domains,
        new GenericBoundedSamplingData(Some(f._1),
          Some(f._2.toString),
          Some(f._3)))
    })

  tabbedPane.pages += new TabbedPane.Page("Settings",
    new PluginPanel("wrap 2") {
      contents += new Label("Number of samples")
      contents += sampleTextField
      contents += multiPanel.panel
    })

  def domains = KeyRegistry.boundedDomains.values.map { f ⇒ new BoundedDomainDataProxyFactory(f).buildDataProxyUI }.toList

  override def saveContent(name: String) = new SaltelliSamplingDataUI(name,
    sampleTextField.text,
    multiPanel.content.map { c ⇒
      (c.prototypeProxy.get,
        c.boundedDomainProxy.get,
        c.boundedDomainDataUI.get)
    })

  override val help = new Helper(List(new URL(i18n.getString("samplingPermalinkText"),
    i18n.getString("samplingPermalink")),
    new URL(i18n.getString("samplingPermalink1Text"),
      i18n.getString("samplingPermalink1")))) {
    add(sampleTextField, new Help(i18n.getString("sample"), i18n.getString("sampleEx")))
  }
}