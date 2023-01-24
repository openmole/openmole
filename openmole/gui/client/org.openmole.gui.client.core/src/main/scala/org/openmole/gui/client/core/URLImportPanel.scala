package org.openmole.gui.client.core

import org.openmole.gui.client.core.alert.{AbsolutePositioning, AlertPanel, BannerAlert}
import AbsolutePositioning.CenterPagePosition
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
import org.openmole.gui.shared.api.ServerAPI

class URLImportPanel(manager: TreeNodeManager, bannerAlert: BannerAlert) {

  case class URLFile(name: String, extension: String) {
    def file = s"$name.$extension"
  }

  //private val downloadedFile: Var[Option[SafePath]] = Var(None)

  lazy val downloading: Var[ProcessState] = Var(Processed())

  val overwriteAlert: Var[Option[SafePath]] = Var(None)

  def exists(sp: SafePath, ifNotExists: () ⇒ {})(using api: ServerAPI) =
    api.exists(sp).foreach { b ⇒
      if (b) overwriteAlert.set(Some(sp))
      else ifNotExists()
    }

  def download(url: String)(using api: ServerAPI, panels: Panels) = {
    downloading.set(Processing())
    api.downloadHTTP(url, manager.dirNodeLine.now(), extractCheckBox.ref.checked).foreach { d ⇒
      downloading.set(Processed())
      urlDialog.hide
      d match {
        case None   ⇒ manager.invalidCurrentCache
        case Some(ex) ⇒ bannerAlert.registerWithDetails("Download failed", ErrorData.stackTrace(ex))
      }
    }
  }

  def deleteFileAndDownloadURL(sp: SafePath, url: String)(using api: ServerAPI, panels: Panels) =
    api.deleteFiles(Seq(sp)).foreach { d ⇒
      download(url)
    }

  lazy val urlInput = input(placeholder := "Project URL (.oms / .tar.gz)", width := "100%")

  lazy val extractCheckBox = checkbox(false)

  def downloadButton(using api: ServerAPI, panels: Panels) = button(
    btn_primary,
    downloading.withTransferWaiter { _ ⇒ span("Download") },
    height := "20",
    onClick --> { _ ⇒ download(urlInput.ref.value) }
  )

  def alertObserver(using api: ServerAPI, panels: Panels) = Observer[Option[SafePath]] { osp ⇒
    osp match {
      case Some(sp: SafePath) ⇒
        panels.alertPanel.string(
          sp.name + " already exists. Overwrite ? ",
          () ⇒ {
            overwriteAlert.set(None)
            deleteFileAndDownloadURL(manager.dirNodeLine.now(), urlInput.ref.value)
          }, () ⇒ {
            overwriteAlert.set(None)
          }, CenterPagePosition
        )
        div
      case _ ⇒
    }
  }

  def dialogBody(using api: ServerAPI, panels: Panels) = div(
    urlInput,
    span(display.flex, flexDirection.row, alignItems.flexEnd, paddingTop := "20",
      extractCheckBox,
      span("Extract archive (where applicable)", paddingLeft := "10", fontWeight.bold)
    ),
    overwriteAlert --> alertObserver
  )

  def urlDialog(using api: ServerAPI, panels: Panels): ModalDialog = ModalDialog(
    span(b("Import project from URL")),
    dialogBody,
    buttonGroup.amend(downloadButton, closeButton("Close")),
    omsheet.panelWidth(92),
    onopen = () ⇒ {},
    onclose = () ⇒ {}
  )

}
