package org.openmole.gui.client.core

import org.openmole.gui.ext.api.Api
import scalatags.JsDom.tags
import scalatags.JsDom.all._
import scaladget.bootstrapnative.bsn._
import rx._
import org.openmole.gui.ext.tool.client._
import org.openmole.gui.ext.data.SafePath
import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
import autowire._
import org.openmole.gui.ext.tool.client.omsheet
import scaladget.acediff._

class DiffPanel {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val diffDiv = div(height := 450, fontSize := "15px").render

  val diffSafePath: Var[Option[SafePath]] = Var(None)

  lazy val diff: Var[AceDiff] = Var(aceDiff().build)

  def displayDiff(baseContent: String, safePath: SafePath): Unit = {

    diffSafePath.update(Some(safePath))

    def updateDif(localContent: String) =
      diff.update(aceDiff().element(diffDiv).left(LR.content(localContent)).right(LR.content(baseContent).theme("ace/theme/github")).build)

    val localContent = panels.treeNodeTabs.content(safePath) match {
      case None ⇒
        FileManager.download(
          safePath,
          onLoadEnded = (content: String) ⇒ {
            updateDif(content)
          }
        )
      case Some(c: String) ⇒ updateDif(c)
    }

    open
  }

  def open = dialog.show

  val dialog = ModalDialog(omsheet.panelWidth(65))

  dialog.header(tags.span(tags.b("Diff")))

  val textArea = scrollableText("")

  dialog.body(div(height := 500)(
    tags.div(twoColumns)(tags.div("Local version"), tags.div("Repository version")),
    diffDiv
  ))

  val applyButton = button(btn_danger, "Apply", onclick := { () ⇒
    diffSafePath.now.foreach { sp ⇒
      val content = diff.now.getEditors().right.session.getValue
      post()[Api].saveFile(sp, diff.now.getEditors().right.session.getValue).call().foreach { _ ⇒
        panels.treeNodePanel.invalidCacheAndDraw
        panels.treeNodeTabs.tabs.now.filter { t ⇒ t.safePathTab.now == sp }.foreach { t ⇒
          t.setContent(content)
          t.refresh()
        }
        dialog.hide
      }
    }
  })

  dialog.footer(
    buttonGroup(Seq(width := 200, right := 100))(
      ModalDialog.closeButton(dialog, btn_default, "Cancel"),
      applyButton
    )
  )

}