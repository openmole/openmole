package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.alert.AbsolutePositioning.{ FileZone, RelativeCenterPosition }
import org.openmole.gui.client.core.files.FileToolBar._
import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.client.core.Waiter._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.client._
import scaladget.bootstrapnative.bsn._
import org.scalajs.dom.raw._
import org.openmole.gui.client.core._

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import TreeNode._
import autowire._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.client.FileManager
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

  // New file tool
  val newNodeInput = inputTag().amend(
    placeholder := "File name",
    width := "130px",
    marginLeft := "10px",
    onMountFocus
  )

  lazy val addRootDirButton = {

    val folder = ToggleState("Folder", s"btn folder-or-file", () ⇒ {})
    val file = ToggleState("File", s"btn folder-or-file", () ⇒ {})

    toggle(folder, true, file, () ⇒ {})
  }

  def createNewNode = {
    val newFile = newNodeInput.ref.value
    val currentDirNode = treeNodeManager.current
    addRootDirButton.toggled.now match {
      case true  ⇒ CoreUtils.addDirectory(currentDirNode.now, newFile, () ⇒ invalidCacheAndDraw)
      case false ⇒ CoreUtils.addFile(currentDirNode.now, newFile, () ⇒ invalidCacheAndDraw)
    }
  }

  //Upload tool
  val transferring: Var[ProcessState] = Var(Processed())

  def fInputMultiple(todo: Input ⇒ Unit) =
    inputTag().amend(cls := "upload", `type` := "file", multiple := true,
      inContext { thisNode ⇒
        onChange --> { _ ⇒
          todo(thisNode)
        }
      }
    )

  def upbtn(todo: Input ⇒ Unit): HtmlElement =
    span(aria.hidden := true, glyph_upload, cls := "fileUpload glyphmenu", margin := "10 0 10 160",
      fInputMultiple(todo)
    )

  private val upButton = upbtn((fileInput: Input) ⇒ {
    FileManager.upload(fileInput, treeNodeManager.current.now, (p: ProcessState) ⇒ transferring.set(p), UploadProject(), () ⇒ invalidCacheAndDraw)
  })

  lazy val createFileTool =
    form(flexRow, alignItems.center, height := "65px", color.white, margin := "0 10 0 10",
      addRootDirButton.element,
      newNodeInput.amend(marginLeft := "10px"),
      upButton.amend(justifyContent.flexEnd).tooltip("Upload a file"),
      transferring.withTransferWaiter {
        _ ⇒
          div()
      }.amend(marginLeft := "10px"),
      onSubmit.preventDefault --> { _ ⇒
        createNewNode
        newNodeInput.ref.value = ""
        plusFile.set(false)
      })

  val plusFile = Var(false)
  lazy val fileControler =
    div(
      child <-- treeNodeManager.current.signal.map { curr ⇒
        val parent = curr.parent
        div(
          cls := "file-content tree-path",
          goToDirButton(treeNodeManager.root).amend(OMTags.glyph_house, padding := "5"),
          Seq(parent.parent, parent, curr).filterNot { sp ⇒
            sp.isEmpty || sp == treeNodeManager.root
          }.map { sp ⇒
            goToDirButton(sp, s" ${sp.name} / ")
          },
          div(glyph_plus, cls <-- plusFile.signal.map { pf ⇒
            "plus-button" + { if (pf) " selected" else "" }
          }, onClick --> { _ ⇒ plusFile.update(!_) })
        )
      },
      plusFile.signal.expand(createFileTool),
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
            )
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
        transferring.set(p)
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
          sons.list.zipWithIndex.map {
            case (tn, id) ⇒
              Seq(drawNode(tn, id).render)
          }
        )
      }
    })
    )

  }

  def drawNode(node: TreeNode, i: Int) = node match {
    case fn: FileNode ⇒
      ReactiveLine(i, fn, TreeNodeType.file, () ⇒ {
        displayNode(fn)
      })
    case dn: DirNode ⇒ ReactiveLine(i, dn, TreeNodeType.folder, () ⇒ {
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

  val currentSafePath: Var[Option[SafePath]] = Var(None)
  val currentLine = Var(-1)

  object ReactiveLine {
    def apply(i: Int, tn: TreeNode, treeNodeType: TreeNodeType, todo: () ⇒ Unit) = new ReactiveLine(i, tn, treeNodeType, todo)
  }

  class ReactiveLine(id: Int, tn: TreeNode, treeNodeType: TreeNodeType, todo: () ⇒ Unit) {

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

    def fileClick =
      onClick --> { e ⇒
        plusFile.set(false)
        if (!selectionMode.now) {
          todo()
        }
      }

    val render: HtmlElement = {
      // val settingsGlyph = Seq(cls := "glyphitem", glyph_settings, color := WHITE, paddingLeft := "4")
      div(display.flex, flexDirection.column,
        div(display.flex, alignItems.center, lineHeight := "27px",
          //        child <-- selectionMode.signal.combineWith(treeStates.signal, fileToolBar.selectedTool.signal, treeNodeManager.pluggables.signal).map {
          //          case (sM, tS, sTools, pluggables) ⇒
          //            onClick --> { e ⇒ {
          //              if (sM) {
          //                addToSelection
          //                if (e.ctrlKey) clearSelectionExecpt(tnSafePath)
          //              }
          //            }
          //            },
          dropPairs,
          onDragStart --> { e ⇒
            e.dataTransfer.setData("text/plain", "nothing") //  FIREFOX TRICK
            draggedNode.set(Some(tnSafePath))
          },
          onDrop --> { e ⇒
            e.dataTransfer
            e.preventDefault()
            dropAction(treeNodeManager.current.now ++ tn.name, tn match {
              case _: DirNode ⇒ true
              case _          ⇒ false
            })
          },
          onDragEnter --> { _ ⇒
            false
          },
          dirBox(tn).amend(cls := "file0", fileClick, draggable := true),
          div(tn.name, cls := "file1", fileClick, draggable := true),
          i(timeOrSize(tn), cls := "file2"),
          button(cls := "bi-three-dots transparent-button", cursor.pointer, opacity := "0.5", onClick --> { _ ⇒
            currentSafePath.set(Some(tnSafePath))
            currentLine.set(
              if (id == currentLine.now) -1
              else id
            )
          })

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
        ),
        currentLine.signal.map { i ⇒ i == id }.expand(toolBox.contentRoot)
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
