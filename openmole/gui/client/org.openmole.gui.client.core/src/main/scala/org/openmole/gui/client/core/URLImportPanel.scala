package org.openmole.gui.client.core

import org.openmole.gui.client.ext.*
import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.client.core.files.{TreeNodeManager, TreeNodePanel}
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*
import org.openmole.gui.client.core.CoreUtils.*
import org.openmole.gui.shared.data.*
import Waiter.*
import org.openmole.core.market.MarketIndexEntry
import com.raquo.laminar.api.L.*
import Waiter.*
import com.raquo.laminar.nodes.ReactiveElement.isActive
import org.openmole.gui.client.tool.Component
import org.openmole.gui.shared.api.*

object URLImportPanel:
  def render(using api: ServerAPI, basePath: BasePath, panels: Panels) =

    val manager = panels.treeNodePanel.treeNodeManager

    lazy val downloading: Var[ProcessState] = Var(Processed())

    def download(url: String) =
      val sp = manager.directory.now()

      def doDownload(url: String) =
        downloading.set(Processing())
        api.downloadHTTP(url, sp, extractCheckBox.isChecked, overwriteSwitch.isChecked).foreach { d ⇒
          downloading.set(Processed())
          panels.treeNodePanel.refresh
          panels.closeExpandable
        }

      overwriteSwitch.isChecked match {
        case true => doDownload(url)
        case false =>
          api.exists(sp).foreach {
            _ match
              case true =>
                panels.notifications.showGetItNotification(NotificationLevel.Error, s"${sp.name}/${url.split("/").last} already exists", div("Turn overwrite to true to fix this problem"))
              case false => doDownload(url)
          }
      }


    def deleteFileAndDownloadURL(sp: SafePath, url: String) =
      api.deleteFiles(Seq(sp)).foreach { d ⇒
        download(url)
      }

    lazy val urlInput = inputTag().amend(placeholder := "Project URL (.oms / .tar.gz)", width := "400px", marginTop := "20")

    lazy val extractCheckBox = Component.Switch("Extract archive (where applicable)", true, "importURL")
    lazy val overwriteSwitch = Component.Switch("Overwrite exitsting files", true, "importURL")

    val downloadButton = button(
      cls := "btn btn-purple",
      downloading.withTransferWaiter() { _ ⇒ span("Download") },
      height := "38", width := "150", marginTop := "20",
      onClick --> { _ ⇒ download(urlInput.ref.value) }
    )

    div(flexColumn,
      urlInput,
      extractCheckBox.element.amend(marginTop := "50"),
      overwriteSwitch.element,
      downloadButton
    )