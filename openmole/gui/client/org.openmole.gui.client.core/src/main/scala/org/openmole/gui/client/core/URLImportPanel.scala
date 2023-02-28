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
import org.openmole.gui.client.core.Panels.closeExpandable
import org.openmole.gui.shared.api.*

object URLImportPanel:
  def render(using api: ServerAPI, basePath: BasePath, panels: Panels) =

    val manager = panels.treeNodePanel.treeNodeManager

    case class URLFile(name: String, extension: String) {
      def file = s"$name.$extension"
    }

    //private val downloadedFile: Var[Option[SafePath]] = Var(None)

    lazy val downloading: Var[ProcessState] = Var(Processed())

    val overwriteAlert: Var[Option[SafePath]] = Var(None)

    def exists(sp: SafePath, ifNotExists: () ⇒ {})(using api: ServerAPI, basePath: BasePath) =
      api.exists(sp).foreach { b ⇒
        if (b) overwriteAlert.set(Some(sp))
        else ifNotExists()
      }

    def download(url: String) = {
      downloading.set(Processing())
      api.downloadHTTP(url, manager.dirNodeLine.now(), extractCheckBox.ref.checked).foreach { d ⇒
        downloading.set(Processed())
        d match {
          case None ⇒
            manager.invalidCurrentCache
            Panels.closeExpandable
          case Some(ex) ⇒ //FIXME: bannerAlert.registerWithDetails("Download failed", ErrorData.stackTrace(ex))
        }
      }
    }

    def deleteFileAndDownloadURL(sp: SafePath, url: String) =
      api.deleteFiles(Seq(sp)).foreach { d ⇒
        download(url)
      }

    lazy val urlInput = input(placeholder := "Project URL (.oms / .tar.gz)", width := "400px", marginTop := "20")

    lazy val extractCheckBox = checkbox(false)

    val downloadButton = button(
      cls := "btn btn-purple",
      downloading.withTransferWaiter { _ ⇒ span("Download") },
      height := "38", width := "150", marginTop:= "20",
      onClick --> { _ ⇒ download(urlInput.ref.value) }
    )

    def alertObserver = Observer[Option[SafePath]] { osp ⇒
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

    div(flexColumn,
      urlInput,
      div(flexRow, marginTop:= "20", extractCheckBox, div("Extract archive (where applicable)", paddingLeft := "10", color := "#222")),
      downloadButton,
      //FIXME
      // overwriteAlert --> alertObserver
    )