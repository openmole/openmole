package org.openmole.gui.client.core

import org.openmole.gui.client.core.alert.{ AbsolutePositioning, AlertPanel, BannerAlert }
import AbsolutePositioning.CenterPagePosition
import org.openmole.gui.ext.client._

import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.client.core.files.{ TreeNodeManager, TreeNodePanel }
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.client.core.CoreUtils._
import org.openmole.gui.ext.data._
import Waiter._
import org.openmole.core.market.MarketIndexEntry
import com.raquo.laminar.api.L._
import Waiter._
import com.raquo.laminar.nodes.ReactiveElement.isActive

class URLImportPanel(manager: TreeNodeManager, bannerAlert: BannerAlert) {

  case class URLFile(name: String, extension: String) {
    def file = s"$name.$extension"
  }

  //private val downloadedFile: Var[Option[SafePath]] = Var(None)

  lazy val downloading: Var[ProcessState] = Var(Processed())

  val overwriteAlert: Var[Option[SafePath]] = Var(None)

  def exists(sp: SafePath, ifNotExists: () ⇒ {}) =
    Fetch.future(_.exists(sp).future).foreach { b ⇒
      if (b) overwriteAlert.set(Some(sp))
      else ifNotExists()
    }

  def download(url: String) = {
    downloading.set(Processing())
    Fetch.future(_.downloadHTTP(url, manager.dirNodeLine.now(), extractCheckBox.ref.checked).future).foreach { d ⇒
      downloading.set(Processed())
      urlDialog.hide
      d match {
        case None   ⇒ staticPanels.treeNodeManager.invalidCurrentCache
        case Some(ex) ⇒ bannerAlert.registerWithDetails("Download failed", ErrorData.stackTrace(ex))
      }
    }
  }

  def deleteFileAndDownloadURL(sp: SafePath, url: String) =
    Fetch.future(_.deleteFiles(Seq(sp)).future).foreach { d ⇒
      download(url)
    }

  lazy val urlInput = input(placeholder := "Project URL (.oms / .tar.gz)", width := "100%")

  lazy val extractCheckBox = checkbox(false)

  lazy val downloadButton = button(
    btn_primary,
    downloading.withTransferWaiter { _ ⇒
      span("Download")
    },
    height := "20",
    onClick --> { _ ⇒ download(urlInput.ref.value) }
  )

  val alertObserver = Observer[Option[SafePath]] { osp ⇒
    osp match {
      case Some(sp: SafePath) ⇒
        staticPanels.alertPanel.string(
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

  val dialogBody = div(
    urlInput,
    span(display.flex, flexDirection.row, alignItems.flexEnd, paddingTop := "20",
      extractCheckBox,
      span("Extract archive (where applicable)", paddingLeft := "10", fontWeight.bold)
    ),
    overwriteAlert --> alertObserver
  )

  val urlDialog: ModalDialog = ModalDialog(
    span(b("Import project from URL")),
    dialogBody,
    buttonGroup.amend(downloadButton, closeButton("Close")),
    omsheet.panelWidth(92),
    onopen = () ⇒ {},
    onclose = () ⇒ {}
  )

}
