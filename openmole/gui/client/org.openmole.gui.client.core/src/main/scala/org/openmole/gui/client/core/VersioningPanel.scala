package org.openmole.gui.client.core

import org.openmole.gui.ext.data._
import scaladget.bootstrapnative.bsn._
import org.openmole.gui.ext.tool.client._
import scalatags.JsDom.all._
import scaladget.tools._
import scaladget.tools._
import rx._
import scaladget.bootstrapnative.Selector.Options

class VersioningPanel {

  val versioningSelector: Options[VersioningPluginFactory] = Plugins.versioningFactories.now.options(0, btn_primary, (a: VersioningPluginFactory) ⇒ a.name,
    onclose = () ⇒ currentPanel() = versioningSelector.content.now.map {
      _.buildEmpty
    }
  )

  val currentPanel: Var[Option[VersioningGUIPlugin]] = Var(versioningSelector.content.now.map { _.buildEmpty })
  val dialog = ModalDialog(omsheet.panelWidth(52))

  dialog header (b("Clone a repository"))

  dialog body (hForm()(
    versioningSelector.selector,
    Rx {
      currentPanel().map { _.panel }.getOrElse(div())
    }
  ))
}