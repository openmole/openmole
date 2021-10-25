package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.alert.AbsolutePositioning.{ FileZone, RelativeCenterPosition }
import org.openmole.gui.client.core.alert.AlertPanel
import org.openmole.gui.client.core.files.FileToolBar.{ FilterTool, PluginTool, TrashTool }
import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.client.core.Waiter._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.client._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.scalajs.dom.html.Input
import org.scalajs.dom.raw._
import org.openmole.gui.client.core._

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import TreeNode._
import autowire._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.client.FileManager
import org.scalajs.dom
import scaladget.bootstrapnative.Popup
import scaladget.bootstrapnative.Popup.Manual
import com.raquo.laminar.api.L._
import org.openmole.gui.client.tool.OMTags

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

class TreeNodePanel(val treeNodeManager: TreeNodeManager, fileDisplayer: FileDisplayer, showExecution: () ⇒ Unit, treeNodeTabs: TreeNodeTabs, services: PluginServices) {

  val selectionMode = Var(false)
  val treeWarning = Var(true)
  val draggedNode: Var[Option[SafePath]] = Var(None)

  val selectionModeObserver = Observer[Boolean] { b ⇒
    if (!b) treeNodeManager.clearSelection
  }

  def turnSelectionTo(b: Boolean) = selectionMode.set(b)

  val fileToolBar = new FileToolBar(this)
  val tree: Var[HtmlElement] = Var(div())

  val editNodeInput = inputTag("").amend(
    placeholder := "Name",
    width := "240px",
    height := "24px",
    onMountFocus
  )

  lazy val fileControler =
    // val current = treeNodeManager.current()
    div(
      cls := "file-content tree-path",
      child <-- treeNodeManager.current.signal.map { curr ⇒
        div(
          goToDirButton(treeNodeManager.root).amend(OMTags.glyph_house, float.left),
          Seq(curr.parent, curr).filterNot { sp ⇒
            sp.isEmpty || sp == treeNodeManager.root
          }.map { sp ⇒
            goToDirButton(sp, s"| ${sp.name}")
          }
        )
      },
      treeNodeManager.error --> treeNodeManager.errorObserver,
      treeNodeManager.comment --> treeNodeManager.commentObserver
    )

  lazy val labelArea =
    div(
      cls := "file-content",
      child <-- treeNodeManager.copied.signal.combineWith(treeNodeManager.current.signal).map {
        case (copied, curr) ⇒
          div(
            div(
              if (copied.isEmpty) div()
              else
                buttonGroup.amend(
                  omsheet.pasteLabel,
                  button(btn_danger, "Paste", onClick --> { _ ⇒ paste(copied, curr) })
                ),
              button(btn_secondary, "Cancel", onClick --> { _ ⇒
                treeNodeManager.emptyCopied
                fileToolBar.unselectTool
                drawTree
              })
            ),
            fileToolBar.sortingGroup
          )
      }
    )

  lazy val view = {
    drawTree
    div(child <-- tree.signal, cls := "file-scrollable-content")
  }

  private def paste(safePaths: Seq[SafePath], to: SafePath) = {
    def refreshWithNoError = {
      treeNodeManager.noError
      invalidCacheAndDraw
    }

    def onpasted = {
      treeNodeManager.emptyCopied
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
        else treeNodeManager.setFilesInError(
          "Some files already exists, overwrite?",
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
    else treeNodeManager.setFilesInComment(
      "Pasting a folder in itself is not allowed",
      same,
      () ⇒ treeNodeManager.noError
    )
  }

  def filter: FileFilter = fileToolBar.fileFilter.now

  def downloadFile(safePath: SafePath, onLoaded: (String, Option[String]) ⇒ Unit, saveFile: Boolean, hash: Boolean) = {

    //    if (FileExtension.isOMS(safePath.name))
    //      OMPost()[Api].hash(safePath).call().foreach { h ⇒
    //        HashService.set(safePath, h)
    //      }

    FileManager.download(
      safePath,
      (p: ProcessState) ⇒ {
        fileToolBar.transferring.set(p)
      },
      hash = hash,
      onLoaded = onLoaded
    )
  }

  def goToDirButton(safePath: SafePath, name: String = ""): HtmlElement =
    span(cls := "treePathItems", name,
      onClick --> { _ ⇒
        fileToolBar.clearMessage
        treeNodeManager.switch(safePath)
        drawTree
      },
      dropPairs,
      onDragEnter --> { _ ⇒
        false
      },
      onDragLeave --> { _ ⇒
        false
      },
      onDrop --> { e ⇒
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
    treeNodeManager.invalidCurrentCache
    todo()
  }

  def refreshAnd(todo: () ⇒ Unit) = invalidCacheAnd(todo)

  def refreshAndDraw = invalidCacheAndDraw

  def computePluggables = fileToolBar.selectedTool.now match {
    case Some(PluginTool) ⇒ treeNodeManager.computePluggables(() ⇒ if (!treeNodeManager.pluggables.now.isEmpty) turnSelectionTo(true))
    case _                ⇒
  }

  //  def fileTable(rows: Seq[Seq[HtmlElement]]) = {
  //    div(
  //      rows.map { r ⇒
  //        //        div(display.flex, alignItems.center,
  //        //          r.zipWithIndex.map {
  //        //            case (e, c) ⇒
  //        //              e.amend(cls := s"file$c")
  //        //          }
  //        //        )
  //      }
  //    )
  //  }

  def dirBox(tn: TreeNode) = {
    tn match {
      case _: DirNode ⇒ div(
        cls := "dir",
        div(cls := "plus bi-plus")
      )
      case _ ⇒ div()
    }
  }

  def drawTree: Unit = {
    computePluggables
    tree.set(treeNodeManager.computeCurrentSons(filter).withFutureWaiter("Get files", (sons: ListFiles) ⇒ {

      if (treeNodeManager.isRootCurrent && treeNodeManager.isProjectsEmpty) {
        div("Create a first OpenMOLE script (.oms)", cls := "message")
      }
      else {
        //          tbody(
        //            backgroundColor <-- selectionMode.signal.map { sM ⇒
        //              if (sM) omsheet.BLUE else omsheet.DARK_GREY
        //            },
        //            omsheet.fileList,
        //            child <-- treeWarning.signal.map { tW ⇒
        //              if (sons.list.length < sons.nbFilesOnServer && tW) {
        //                div(
        //                  omsheet.moreEntries,
        //                  div(
        //                    omsheet.moreEntriesText,
        //                    div(
        //                      s"Max of 1,000 files (${100000 / sons.nbFilesOnServer}%) displayed simultaneously",
        //                      div(
        //                        "Use the ",
        //                        span(
        //                          "Filter tool",
        //                          cursor.pointer, color := omsheet.BLUE,
        //                          onClick --> { _ ⇒ fileToolBar.selectTool(FilterTool) }
        //                        ), " to refine your search"
        //                      )
        //                    )
        //                  )
        //                )
        //              }
        //              else div()
        //            },
        //            for (tn ← sons.list) yield {
        //              drawNode(tn).render
        //            },
        //            //            onscroll := { () ⇒
        //            //              Popover.hide
        //            //            },
        //            selectionMode --> selectionModeObserver
        //          )
        div(
          sons.list.map { tn ⇒
            Seq(drawNode(tn).render)
          }
        )
      }
    })
    )

  }

  def drawNode(node: TreeNode) = node match {
    case fn: FileNode ⇒
      ReactiveLine(fn, TreeNodeType.file, () ⇒ {
        displayNode(fn)
      })
    case dn: DirNode ⇒ ReactiveLine(dn, TreeNodeType.folder, () ⇒ {
      treeNodeManager switch (dn.name)
      fileToolBar.clearMessage
      fileToolBar.unselectTool
      treeWarning.set(true)
      drawTree
    })
  }

  def displayNode(tn: TreeNode) = tn match {
    case fn: FileNode ⇒
      val ext = FileExtension(tn.name)
      val tnSafePath = treeNodeManager.current.now ++ tn.name
      if (ext.displayable) {
        downloadFile(
          tnSafePath,
          saveFile = false,
          hash = true,
          onLoaded = (content: String, hash: Option[String]) ⇒ {
            fileDisplayer.display(tnSafePath, content, hash.get, ext, services)
            invalidCacheAndDraw
          }
        )
      }
    case _ ⇒
  }

  def stringAlert(message: String, okaction: () ⇒ Unit) =
    panels.alertPanel.string(message, okaction, transform = RelativeCenterPosition, zone = FileZone)

  def stringAlertWithDetails(message: String, detail: String) =
    panels.alertPanel.detail(message, detail, transform = RelativeCenterPosition, zone = FileZone)

  var currentSafePath: Var[Option[SafePath]] = Var(None)

  object ReactiveLine {
    def apply(tn: TreeNode, treeNodeType: TreeNodeType, todo: () ⇒ Unit) = new ReactiveLine(tn, treeNodeType, todo)
  }

  class ReactiveLine(tn: TreeNode, treeNodeType: TreeNodeType, todo: () ⇒ Unit) {

    val tnSafePath = treeNodeManager.current.now ++ tn.name

    case class TreeStates(settingsSet: Boolean, edition: Boolean, replication: Boolean, selected: Boolean = treeNodeManager.isSelected(tn)) {
      def settingsOn = treeStates.set(copy(settingsSet = true))

      def editionOn = treeStates.set(copy(edition = true))

      def replicationOn = treeStates.set(copy(replication = true))

      def settingsOff = treeStates.set(copy(settingsSet = false))

      def editionOff = treeStates.set(copy(edition = false))

      def replicationOff = treeStates.set(copy(replication = false))

      def editionAndReplicationOn = treeStates.set(copy(edition = true, replication = true))

      def setSelected(b: Boolean) = treeStates.set(copy(selected = b))
    }

    private val treeStates: Var[TreeStates] = Var(TreeStates(false, false, false))

    //    val clickablePair = {
    //      val style = Seq(
    //        float.left,
    //        cursor.pointer,
    //        draggable := true,
    //        onClick --> { e ⇒
    //          if (!selectionMode.now) {
    //            todo()
    //          }
    //        }
    //      )
    //      println("TN " + tn.name)
    //      tn match {
    //        case fn: FileNode ⇒ span(span(paddingTop := "4", omsheet.file, style, div(omsheet.fileNameOverflow, tn.name)))
    //        case dn: DirNode ⇒
    //          div(omsheet.dir,
    //            div(
    //              if (dn.isEmpty) emptySetters
    //              else glyph_plus
    //            ),
    //            , style, div(omsheet.fileNameOverflow, paddingLeft := "22"), tn.name
    //          )
    //      }
    //    }

    def timeOrSize(tn: TreeNode): String = fileToolBar.fileFilter.now.fileSorting match {
      case TimeSorting() ⇒ CoreUtils.longTimeToString(tn.time)
      case _             ⇒ CoreUtils.readableByteCountAsString(tn.size)
    }

    def clearSelectionExecpt(safePath: SafePath) = {
      treeStates.now.setSelected(true)
      treeNodeManager.clearSelectionExecpt(safePath)
    }

    def addToSelection(b: Boolean): Unit = {
      treeStates.now.setSelected(b)
      treeNodeManager.setSelected(tnSafePath, treeStates.now.selected)
    }

    def addToSelection: Unit = addToSelection(!treeStates.now.selected)

    val toolBox = new FileToolBox(tnSafePath, showExecution, treeNodeTabs)

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

    //    dom.document.body.onlick = { (e: Event) ⇒
    //      val element = e.target.asInstanceOf[HTMLElement]
    //      if (!toolBox.actions(element)) {
    //        if (!inPopover(element))
    //          Popover.hide
    //      }
    //      else
    //        e.preventDefault()
    //    }

    def buildManualPopover(trigger: HtmlElement) = {
      lazy val pop = trigger.popover(div(toolBox.contentRoot.toString), Popup.Right, Manual, title = Some(toolBox.titleRoot.toString))
      val popRender = pop.render.amend(
        onClick --> { e ⇒
          //          if (Popover.current.now == Some(pop)) Popover.hide
          //          else {
          //            Popover.current.now match {
          //              case Some(p) ⇒ Popover.toggle(p)
          //              case _ ⇒
          //            }
          //            Popover.toggle(pop)
          //          }
          currentSafePath.set(Some(tnSafePath))
          e.stopPropagation
        }
      )

      popRender
    }

    def fileClick =
      onClick --> { e ⇒
        if (!selectionMode.now) {
          todo()
        }
      }

    val render: HtmlElement = {
      // val settingsGlyph = Seq(cls := "glyphitem", glyph_settings, color := WHITE, paddingLeft := "4")

      div(display.flex, alignItems.center, margin := "3",
        //        child <-- selectionMode.signal.combineWith(treeStates.signal, fileToolBar.selectedTool.signal, treeNodeManager.pluggables.signal).map {
        //          case (sM, tS, sTools, pluggables) ⇒
        //            onClick --> { e ⇒ {
        //              if (sM) {
        //                addToSelection
        //                if (e.ctrlKey) clearSelectionExecpt(tnSafePath)
        //              }
        //            }
        //            },
        //          dropPairs
        //          ,
        //          onDragStart --> { e ⇒
        //            e.dataTransfer.setData("text/plain", "nothing") //  FIREFOX TRICK
        //            draggedNode.set(Some(tnSafePath))
        //          }
        //          ,
        //          onDrop --> { e ⇒
        //            e.dataTransfer
        //            e.preventDefault()
        //            dropAction(treeNodeManager.current.now ++ tn.name, tn match {
        //              case _: DirNode ⇒ true
        //              case _ ⇒ false
        //            })
        //          }
        //          ,
        //          onDragEnter --> { _ ⇒
        //            false
        //          }
        //,
        // clickablePair,
        //              {
        //                div(
        //                  fileInfo,
        //                  span(
        //                    omsheet.fileSize,
        //                    i(timeOrSize(tn)),
        //                    buildManualPopover(
        //                      div(settingsGlyph, onClick --> { _ ⇒ /*FIXME*/
        //                        /*Popover.hide*/
        //                      })
        //                    )
        //                  )
        //                )
        //              },
        dirBox(tn).amend(cls := "file0", fileClick),
        div(tn.name, cls := "file1", fileClick),
        i(timeOrSize(tn), cls := "file2"),
        buildManualPopover(
          div(cls := "bi-gear-fill", onClick --> { _ ⇒ /*FIXME*/
            /*Popover.hide*/
          })
        )
      //
      //              div(
      //                width := "100%",
      //                if (tS.selected) {
      //                  sTools match {
      //                    case Some(TrashTool) ⇒ omsheet.fileSelectedForDeletion
      //                    case Some(PluginTool) if pluggables.contains(tn) ⇒ omsheet.fileSelected
      //                    case _ ⇒ omsheet.fileSelected
      //                  }
      //                }
      //                else omsheet.fileSelectionOverlay,
      //                span(omsheet.fileSelectionMessage)
      //              )
      )
      //}
      //)
    }
  }

  def dropPairs = Seq(
    draggable := true,
    onDragEnter --> { e ⇒
      val el = e.target.asInstanceOf[HTMLElement]
      val style = new CSSStyleDeclaration()
      style.backgroundColor = "red"
      el.style = style
      false
    },
    onDragLeave --> { e ⇒
      val style = new CSSStyleDeclaration
      style.backgroundColor = "transparent"
      e.target.asInstanceOf[HTMLElement].style = style
      false
    },
    onDragOver --> { e ⇒
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
            //treeNodeTabs.saveAllTabs(() ⇒ {
            Post()[Api].move(dragged, to).call().foreach {
              b ⇒
                treeNodeManager.invalidCache(to)
                treeNodeManager.invalidCache(dragged)
                refreshAndDraw
                treeNodeTabs.checkTabs
            }
            //})
          }
        }
    }
    draggedNode.set(None)
    false
  }

}
