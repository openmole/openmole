package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.alert.AbsolutePositioning.{ FileZone, RelativeCenterPosition }
import org.openmole.gui.client.core.alert.AlertPanel
import org.openmole.gui.client.core.files.FileToolBar.{ FilterTool, PluginTool, TrashTool }
import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.client.core.Waiter._
import org.openmole.gui.ext.data._
import org.openmole.gui.client.core.panels._
import org.openmole.gui.ext.client._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw._
import org.openmole.gui.client.core._
import scalatags.JsDom.all._
import scalatags.JsDom.{ TypedTag, tags }

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import TreeNode._
import autowire._
import rx._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.client.FileManager
import org.scalajs.dom
import scaladget.bootstrapnative.Popup
import scaladget.bootstrapnative.Popup.Manual

/*
 * Copyright (C) 16/04/15 // mathieu.leclaire@openmole.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

class TreeNodePanel {

  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()
  val selectionMode = Var(false)
  val treeWarning = Var(true)
  val draggedNode: Var[Option[SafePath]] = Var(None)

  selectionMode.trigger {
    if (!selectionMode.now) panels.treeNodeManager.clearSelection
  }

  def turnSelectionTo(b: Boolean) = selectionMode() = b

  val fileToolBar = new FileToolBar(this)
  val tree: Var[TypedTag[HTMLElement]] = Var(tags.div())

  val editNodeInput: Input = inputTag()(
    placeholder := "Name",
    width := "240px",
    height := "24px",
    autofocus
  ).render

  lazy val fileControler = Rx {
    val current = panels.treeNodeManager.current()
    div(ms("tree-path"))(
      goToDirButton(panels.treeNodeManager.root, glyph_home +++ floatLeft +++ "treePathItems"),
      Seq(current.parent, current).filterNot { sp ⇒
        sp.isEmpty || sp == panels.treeNodeManager.root
      }.map { sp ⇒
        goToDirButton(sp, "treePathItems", s"| ${sp.name}")
      }
    )
  }

  lazy val labelArea =
    div(
      Rx {
        div(
          if (panels.treeNodeManager.copied().isEmpty) tags.div
          else
            buttonGroup(omsheet.pasteLabel)(
              button(btn_danger, "Paste", onclick := { () ⇒ paste(panels.treeNodeManager.copied(), panels.treeNodeManager.current()) }),
              button(btn_default, "Cancel", onclick := { () ⇒
                panels.treeNodeManager.emptyCopied
                fileToolBar.unselectTool
                drawTree
              }
              )
            ),
          fileToolBar.sortingGroup.div
        )
      }
    )

  lazy val view = {
    drawTree
    tags.div(
      Rx {
        tree()
      }
    ).render
  }

  private def paste(safePaths: Seq[SafePath], to: SafePath) = {
    def refreshWithNoError = {
      panels.treeNodeManager.noError
      invalidCacheAndDraw
    }

    def onpasted = {
      panels.treeNodeManager.emptyCopied
      // unselectTool
    }

    val same = safePaths.filter { sp ⇒
      sp == to
    }
    if (same.isEmpty) {
      CoreUtils.testExistenceAndCopyProjectFilesTo(safePaths, to).foreach { existing ⇒
        if (existing.isEmpty) {
          refreshWithNoError
          onpasted
        }
        else panels.treeNodeManager.setFilesInError(
          "Some files already exists, overwrite ?",
          existing,
          () ⇒ CoreUtils.copyProjectFilesTo(safePaths, to).foreach { b ⇒
            refreshWithNoError
            onpasted
          }, () ⇒ {
            refreshWithNoError
            // unselectTool
          }
        )
      }
    }
    else panels.treeNodeManager.setFilesInComment(
      "Paste a folder in itself is not allowed",
      same,
      () ⇒ panels.treeNodeManager.noError
    )
  }

  def filter: FileFilter = fileToolBar.fileFilter.now

  def downloadFile(safePath: SafePath, saveFile: Boolean, onLoaded: String ⇒ Unit = (s: String) ⇒ {}) = {
    FileManager.download(
      safePath,
      (p: ProcessState) ⇒ {
        fileToolBar.transferring() = p
      },
      onLoaded
    )
  }

  def goToDirButton(safePath: SafePath, ck: ModifierSeq, name: String = ""): TypedTag[_ <: HTMLElement] =
    span(ck)(name)(
      onclick := {
        () ⇒
          fileToolBar.clearMessage
          panels.treeNodeManager.switch(safePath)
          drawTree
      },
      dropPairs,
      ondragenter := {
        (e: DragEvent) ⇒
          false
      },
      ondragleave := {
        (e: DragEvent) ⇒
          false
      },
      ondrop := { (e: DragEvent) ⇒
        e.dataTransfer
        e.preventDefault()
        dropAction(safePath, true)
      }
    )

  def invalidCacheAndDraw = {
    invalidCacheAnd(() ⇒ {
      drawTree
    })
  }

  def invalidCacheAnd(todo: () ⇒ Unit) = {
    panels.treeNodeManager.invalidCurrentCache
    todo()
  }

  def refreshAnd(todo: () ⇒ Unit) = invalidCacheAnd(todo)
  def refreshAndDraw = invalidCacheAndDraw

  def computePluggables = fileToolBar.selectedTool.now match {
    case Some(PluginTool) ⇒ panels.treeNodeManager.computePluggables(() ⇒ if (!panels.treeNodeManager.pluggables.now.isEmpty) turnSelectionTo(true))
    case _                ⇒
  }

  def drawTree: Unit = {
    computePluggables
    tree() = panels.treeNodeManager.computeCurrentSons(filter).withFutureWaiter("Get files", (sons: ListFiles) ⇒ {

      tags.table(
        if (panels.treeNodeManager.isRootCurrent && panels.treeNodeManager.isProjectsEmpty) {
          div("Create a first OpenMOLE script (.oms)")(ms("message"))
        }
        else {
          tbody(
            backgroundColor := Rx {
              if (selectionMode()) omsheet.BLUE else omsheet.DARK_GREY
            },
            omsheet.fileList,
            Rx {
              if (sons.list.length < sons.nbFilesOnServer && treeWarning()) {
                div(omsheet.moreEntries)(
                  div(
                    omsheet.moreEntriesText,
                    div(
                      s"Only 1000 files maximum (${100000 / sons.nbFilesOnServer}%) can be displayed.",
                      div(
                        "Use the ",
                        span(
                          "Filter tool",
                          pointer +++ omsheet.color(omsheet.BLUE),
                          onclick := { () ⇒ fileToolBar.selectTool(FilterTool) }
                        ), " to refine your search"
                      )
                    )
                  )
                )
              }
              else div()
            },
            for (tn ← sons.list) yield {
              drawNode(tn).render
            },
            onscroll := { () ⇒
              Popover.hide
            }

          )
        }
      )
    })

  }

  def drawNode(node: TreeNode) = node match {
    case fn: FileNode ⇒
      ReactiveLine(fn, TreeNodeType.file, () ⇒ {
        displayNode(fn)
      })
    case dn: DirNode ⇒ ReactiveLine(dn, TreeNodeType.folder, () ⇒ {
      panels.treeNodeManager switch (dn.name.now)
      fileToolBar.clearMessage
      fileToolBar.unselectTool
      treeWarning() = true
      drawTree
    })
  }

  def displayNode(tn: TreeNode) = tn match {
    case fn: FileNode ⇒
      val ext = FileExtension(tn.name.now)
      val tnSafePath = panels.treeNodeManager.current.now ++ tn.name.now
      if (ext.displayable) {
        downloadFile(tnSafePath, false, (content: String) ⇒ {
          fileDisplayer.display(tnSafePath, content, ext)
          invalidCacheAndDraw
        })
      }
    case _ ⇒
  }

  def stringAlert(message: String, okaction: () ⇒ Unit) =
    AlertPanel.string(message, okaction, transform = RelativeCenterPosition, zone = FileZone)

  def stringAlertWithDetails(message: String, detail: String) =
    AlertPanel.detail(message, detail, transform = RelativeCenterPosition, zone = FileZone)

  var currentSafePath: Var[Option[SafePath]] = Var(None)

  object ReactiveLine {
    def apply(tn: TreeNode, treeNodeType: TreeNodeType, todo: () ⇒ Unit) = new ReactiveLine(tn, treeNodeType, todo)
  }

  class ReactiveLine(tn: TreeNode, treeNodeType: TreeNodeType, todo: () ⇒ Unit) {

    val tnSafePath = panels.treeNodeManager.current.now ++ tn.name.now

    case class TreeStates(settingsSet: Boolean, edition: Boolean, replication: Boolean, selected: Boolean = panels.treeNodeManager.isSelected(tn)) {
      def settingsOn = treeStates() = copy(settingsSet = true)

      def editionOn = treeStates() = copy(edition = true)

      def replicationOn = treeStates() = copy(replication = true)

      def settingsOff = treeStates() = copy(settingsSet = false)

      def editionOff = treeStates() = copy(edition = false)

      def replicationOff = treeStates() = copy(replication = false)

      def editionAndReplicationOn = treeStates() = copy(edition = true, replication = true)

      def setSelected(b: Boolean) = treeStates() = copy(selected = b)
    }

    private val treeStates: Var[TreeStates] = Var(TreeStates(false, false, false))

    val clickablePair = {
      val style = floatLeft +++ pointer +++ Seq(
        draggable := true,
        onclick := { (e: MouseEvent) ⇒
          if (!selectionMode.now) {
            todo()
          }
        }
      )

      tn match {
        case fn: FileNode ⇒ span(span(paddingTop := 4), omsheet.file +++ style)(div(omsheet.fileNameOverflow)(tn.name.now))
        case dn: DirNode ⇒
          span(
            span(ms(dn.isEmpty, emptyMod, omsheet.fileIcon +++ glyph_plus)),
            (omsheet.dir +++ style)
          )(div(omsheet.fileNameOverflow +++ (paddingLeft := 22))(tn.name.now))
      }
    }

    def timeOrSize(tn: TreeNode): String = fileToolBar.fileFilter.now.fileSorting match {
      case TimeSorting() ⇒ CoreUtils.longTimeToString(tn.time)
      case _             ⇒ CoreUtils.readableByteCountAsString(tn.size)
    }

    def clearSelectionExecpt(safePath: SafePath) = {
      treeStates.now.setSelected(true)
      panels.treeNodeManager.clearSelectionExecpt(safePath)
    }

    def addToSelection(b: Boolean): Unit = {
      treeStates.now.setSelected(b)
      panels.treeNodeManager.setSelected(tnSafePath, treeStates.now.selected)
    }

    def addToSelection: Unit = addToSelection(!treeStates.now.selected)

    val toolBox = FileToolBox(tnSafePath)

    def inPopover(element: HTMLElement) = {
      val popClass = "popover"

      def inPopover0(e: HTMLElement, depth: Int): Boolean = {
        if (e != null) {
          val b = e.className.contains(popClass)
          if (b || depth > 2) b
          else inPopover0(e.parentElement, depth + 1)
        }
        else false
      }

      inPopover0(element, 0)
    }

    dom.document.body.onclick = { (e: Event) ⇒
      val element = e.target.asInstanceOf[HTMLElement]
      if (!toolBox.actions(element)) {
        if (!inPopover(element))
          Popover.hide
      }
      else
        e.preventDefault()
    }

    def buildManualPopover(trigger: TypedTag[HTMLElement]) = {
      lazy val pop = trigger.popover(toolBox.contentRoot.toString, Popup.Right, Manual, title = Some(toolBox.titleRoot.toString))
      val popRender = pop.render

      popRender.onclick = { (e: Event) ⇒
        if (Popover.current.now == Some(pop)) Popover.hide
        else {
          Popover.current.now match {
            case Some(p) ⇒ Popover.toggle(p)
            case _       ⇒
          }
          Popover.toggle(pop)
        }
        currentSafePath() = Some(tnSafePath)
        e.stopPropagation
      }

      popRender
    }

    val render: TypedTag[dom.html.TableRow] = {
      val settingsGlyph = ms("glyphitem") +++ glyph_settings +++ omsheet.color(WHITE) +++ (paddingLeft := 4)

      tr(
        Rx {
          td(
            onclick := { (e: MouseEvent) ⇒
              {
                if (selectionMode.now) {
                  addToSelection
                  if (e.ctrlKey) clearSelectionExecpt(tnSafePath)
                }
              }
            },
            dropPairs,
            ondragstart := { (e: DragEvent) ⇒
              e.dataTransfer.setData("text/plain", "nothing") //  FIREFOX TRICK
              draggedNode() = Some(tnSafePath)
            },
            ondrop := { (e: DragEvent) ⇒
              e.dataTransfer
              e.preventDefault()
              dropAction(panels.treeNodeManager.current.now ++ tn.name.now, tn match {
                case _: DirNode ⇒ true
                case _          ⇒ false
              })
            },
            ondragenter := {
              (e: DragEvent) ⇒
                false
            },
            clickablePair, {
              div(fileInfo)(
                span(omsheet.fileSize)(
                  tags.i(timeOrSize(tn)),
                  buildManualPopover(
                    div(settingsGlyph, onclick := { () ⇒ Popover.hide })
                  )
                )
              )
            },
            div(
              width := "100%",
              if (treeStates().selected) {
                fileToolBar.selectedTool() match {
                  case Some(TrashTool) ⇒ omsheet.fileSelectedForDeletion
                  case Some(PluginTool) if panels.treeNodeManager.pluggables().contains(tn) ⇒ omsheet.fileSelected
                  case _ ⇒ omsheet.fileSelected
                }
              }
              else omsheet.fileSelectionOverlay,
              span(omsheet.fileSelectionMessage)
            )
          )
        }
      )
    }
  }

  def dropPairs: ModifierSeq = Seq(
    draggable := true,
    ondragenter := {
      (e: DragEvent) ⇒
        val el = e.target.asInstanceOf[HTMLElement]
        val style = new CSSStyleDeclaration()
        style.backgroundColor = "red"
        el.style = style
        false
    },
    ondragleave := {
      (e: DragEvent) ⇒
        val style = new CSSStyleDeclaration
        style.backgroundColor = "transparent"
        e.target.asInstanceOf[HTMLElement].style = style
        false
    },
    ondragover := {
      (e: DragEvent) ⇒
        e.dataTransfer.dropEffect = "move"
        e.preventDefault
        false
    }
  )

  def dropAction(to: SafePath, isDir: Boolean) = {
    draggedNode.now.map {
      dragged ⇒
        if (isDir) {
          if (dragged != to) {
            treeNodeTabs.saveAllTabs(() ⇒ {
              post()[Api].move(dragged, to).call().foreach {
                b ⇒
                  panels.treeNodeManager.invalidCache(to)
                  panels.treeNodeManager.invalidCache(dragged)
                  panels.treeNodePanel.refreshAndDraw
                  treeNodeTabs.checkTabs
              }
            })
          }
        }
    }
    draggedNode() = None
    false
  }

}
