package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.alert.AbsolutePositioning.{FileZone, RelativeCenterPosition}
import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.client.core.Waiter._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.client._
import scaladget.bootstrapnative.bsn._
import org.scalajs.dom.raw._
import org.openmole.gui.client.core._

import scala.concurrent.ExecutionContext.Implicits.global
import TreeNode._
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

  val treeWarning = Var(true)
  val draggedNode: Var[Option[SafePath]] = Var(None)

  val selectionModeObserver = Observer[Boolean] { b ⇒
    if (!b) treeNodeManager.clearSelection
  }

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

    val folder = ToggleState("Folder", "btn blue-button", () ⇒ {})
    val file = ToggleState("File", "btn blue-button", () ⇒ {})

    toggle(folder, true, file, () ⇒ {})
  }

  def createNewNode = {
    val newFile = newNodeInput.ref.value
    val currentDirNode = treeNodeManager.dirNodeLine
    addRootDirButton.toggled.now() match {
      case true ⇒ CoreUtils.createDirectory(currentDirNode.now(), newFile, () ⇒ treeNodeManager.invalidCurrentCache)
      case false ⇒ CoreUtils.createFile(currentDirNode.now(), newFile, () ⇒ treeNodeManager.invalidCurrentCache)
    }
  }

  //Upload tool
  val transferring: Var[ProcessState] = Var(Processed())

  def fInputMultiple(todo: Input ⇒ Unit) = {
    val webkitdirectory: Prop[Boolean] = customProp("webkitdirectory", com.raquo.domtypes.generic.codecs.BooleanAsIsCodec)
    val mozdirectory: Prop[Boolean] = customProp("mozdirectory", com.raquo.domtypes.generic.codecs.BooleanAsIsCodec)

    inputTag().amend(cls := "upload", `type` := "file", multiple := true,
      inContext { thisNode ⇒
        onChange --> { _ ⇒
          todo(thisNode)
        }
      }
    )
  }

  def upbtn(todo: Input ⇒ Unit): HtmlElement =
    span(aria.hidden := true, glyph_upload, cls := "fileUpload glyphmenu", margin := "10 0 10 160",
      fInputMultiple(todo)
    )

  private val upButton = upbtn((fileInput: Input) ⇒ {
    val current = treeNodeManager.dirNodeLine.now()
    FileManager.upload(fileInput, current, (p: ProcessState) ⇒ transferring.set(p), UploadProject())
//    , () ⇒ {
//      val sp: SafePath = current / fileInput.ref.value.split("\\\\").last
//      CoreUtils.appendToPluggedIfPlugin(sp)
//    })
  })

  lazy val createFileTool =
    form(flexRow, alignItems.center, height := "70px", color.white, margin := "0 10 0 10",
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

  val confirmationDiv: Var[Option[Div]] = Var(None)

  def confirmation(text: String, okText: String, todo: () ⇒ Unit) =
    div(
      fileActions,
      div(text, width := "50%", margin := "10px"),
      div(fileItemCancel, "Cancel", onClick --> {
        _ ⇒
          closeMultiTool
      }),
      div(fileItemWarning, okText, onClick --> {
        _ ⇒
          todo()
      })
    )

  lazy val copyOrTrashTool = div(
    height := "70px", flexRow, alignItems.center, color.white, justifyContent.spaceBetween,
    children <-- confirmationDiv.signal.map { ac ⇒
      val selected = treeNodeManager.selected
      val isSelectionEmpty = selected.signal.map {
        _.isEmpty
      }
      ac match {
        case Some(c) ⇒ Seq(c)
        case None ⇒ Seq(
          button(cls := "btn blue-button", marginLeft := "80px", "Copy", onClick --> { _ ⇒
            multiTool.set(Paste)
            confirmationDiv.set(Some(confirmation(s"${selected.now().size} files copied. Browse to the target folder and press Paste", "Paste", () ⇒
              CoreUtils.copyProjectFiles(selected.now(), treeNodeManager.dirNodeLine.now(), overwrite = false).foreach { existing ⇒
                if (existing.isEmpty) {
                  treeNodeManager.invalidCurrentCache
                  closeMultiTool
                }
                else {
                  confirmationDiv.set(Some(confirmation(s"${existing.size} files have already the same name. Overwrite them ?", "Overwrite", () ⇒
                    CoreUtils.copyProjectFiles(selected.now(), treeNodeManager.dirNodeLine.now(), overwrite = true).foreach { b ⇒
                      treeNodeManager.invalidCurrentCache
                      closeMultiTool
                    })))
                }
              })))
          },
            disabled <-- isSelectionEmpty
          ),
          button(btn_danger, "Delete", marginRight := "80px", onClick --> { _ ⇒
            confirmationDiv.set(Some(confirmation(s"Delete ${treeNodeManager.selected.now().size} files ?", "OK", () ⇒
              CoreUtils.trashNodes(treeNodeManager.selected.now()) { () ⇒
                treeNodeManager.invalidCurrentCache
                closeMultiTool
              })))
          },
            disabled <-- isSelectionEmpty)
        )
      }
    }
  )

  def closeMultiTool = {
    multiTool.set(Off)
    confirmationDiv.set(None)
    treeNodeManager.clearSelection
  }

  val plusFile = Var(false)

  trait MultiTool

  object CopyOrTrash extends MultiTool

  object Paste extends MultiTool

  object Off extends MultiTool

  val multiTool: Var[MultiTool] = Var(Off)

  lazy val fileControler =
    div(
      cls := "file-content",
      child <-- treeNodeManager.dirNodeLine.signal.map { curr ⇒
        val parent = curr.parent
        div(
          cls := "tree-path",
          goToDirButton(treeNodeManager.root).amend(OMTags.glyph_house, padding := "5"),
          Seq(parent.parent, parent, curr).filterNot { sp ⇒
            sp.isEmpty || sp == treeNodeManager.root
          }.map { sp ⇒
            goToDirButton(sp, s" ${sp.name} / ")
          },
          div(glyph_plus, cls <-- plusFile.signal.map { pf ⇒
            "plus-button" + {
              if (pf) " selected" else ""
            }
          }, onClick --> { _ ⇒ plusFile.update(!_) })
        )
      },
      div(
        display.flex, justifyContent.flexEnd,
        div(OMTags.glyph_search,
          cls := "filtering-files-item-selected",
          onClick --> { _ ⇒ fileToolBar.filterToolOpen.update(!_) }),
        div(glyph_refresh, cls := "treePathItems file-refresh", onClick --> { _ ⇒ treeNodeManager.invalidCurrentCache }),
        div(cls := "bi-three-dots-vertical treePathItems", fontSize := "20px", onClick --> { _ ⇒
          multiTool.update { mcot ⇒
            mcot match {
              case Off ⇒ CopyOrTrash
              case _ ⇒
                confirmationDiv.set(None)
                treeNodeManager.clearSelection
                Off
            }
          }
          multiTool.now() match {
            case Off ⇒ treeNodeManager.invalidCurrentCache
            case _ ⇒
          }
        })
      )
      ,
      plusFile.signal.expand(createFileTool)
      ,
      multiTool.signal.map { m ⇒ m != Off }.expand(copyOrTrashTool)
      ,
      treeNodeManager.error --> treeNodeManager.errorObserver
      ,
      treeNodeManager.comment --> treeNodeManager.commentObserver
    )

  //def filter: FileFilter = treeNodeManager.fileFilter.now

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
        treeNodeManager.switch(safePath)
      },
      dropPairs,
      onDrop --> { e ⇒
        e.dataTransfer
        e.preventDefault()
        dropAction(safePath, true)
      }
    )

  def invalidCacheAnd(todo: () ⇒ Unit) = {
    treeNodeManager.invalidCurrentCache
    todo()
  }

  //  def computePluggables = fileToolBar.selectedTool.now match {
  //    case Some(PluginTool) ⇒ treeNodeManager.computePluggables(() ⇒ if (!treeNodeManager.pluggables.now.isEmpty) turnSelectionTo(true))
  //    case _                ⇒
  //  }

  val treeView: Div = {
    div(cls := "file-scrollable-content",
      children <-- treeNodeManager.sons.signal.combineWith(treeNodeManager.dirNodeLine.signal).combineWith(treeNodeManager.findFilesContaining.signal).combineWith(multiTool.signal).map {
        case (sons, currentDir, findString, foundFiles, multiTool) ⇒
          if (!foundFiles.isEmpty) {
            foundFiles.map { case (sp, isDir) => div(s"${sp.normalizedPathString}", cls := "findFile", onClick --> { _ =>
              fileToolBar.filterToolOpen.set(false)
              treeNodeManager.resetFileFinder
              fileToolBar.findInput.ref.value = ""
              val switchTarget = if (isDir) sp else sp.parent
              treeNodeManager.switch(switchTarget)
              treeNodeManager.computeCurrentSons()
              displayNode(sp)
            })
            }
          } else {
            if (currentDir == treeNodeManager.root && sons.isEmpty) {
              Seq(div("Create a first OpenMOLE script (.oms)", cls := "message"))
            }
            else {
              if (sons.contains(currentDir)) {
                (if (multiTool == CopyOrTrash) {
                  lazy val allCheck: Input = checkbox(false).amend(cls := "file0", marginBottom := "3px", onClick --> { _ ⇒
                    treeNodeManager.switchAllSelection(sons(currentDir).list.map { tn => currentDir ++ tn.name }, allCheck.ref.checked)
                  })
                  allCheck
                } else emptyNode) +:
                  sons(currentDir).list.zipWithIndex.flatMap { case (tn, id) =>
                    Seq(drawNode(tn, id).render)
                  }
              }
              else Seq(div())
            }
          }
      }
    )
  }

  def displayNode(safePath: SafePath): Unit = {
    if (safePath.extension.displayable) {
      downloadFile(
        safePath,
        saveFile = false,
        hash = true,
        onLoaded = (content: String, hash: Option[String]) ⇒ {
          fileDisplayer.display(safePath, content, hash.get, safePath.extension, services)
          treeNodeManager.invalidCurrentCache
        }
      )
    }
  }

  def displayNode(tn: TreeNode): Unit = tn match {
    case fn: FileNode ⇒
      val tnSafePath = treeNodeManager.dirNodeLine.now() ++ tn.name
      displayNode(tnSafePath)
    case _ ⇒
  }

  def stringAlert(message: String, okaction: () ⇒ Unit) =
    panels.alertPanel.string(message, okaction, transform = RelativeCenterPosition, zone = FileZone)

  def stringAlertWithDetails(message: String, detail: String) =
    panels.alertPanel.detail(message, detail, transform = RelativeCenterPosition, zone = FileZone)

  val currentSafePath: Var[Option[SafePath]] = Var(None)
  val currentLine = Var(-1)

  def timeOrSize(tn: TreeNode): String = treeNodeManager.fileFilter.now().fileSorting match {
    case TimeSorting() ⇒ CoreUtils.longTimeToString(tn.time)
    case _ ⇒ CoreUtils.readableByteCountAsString(tn.size)
  }

  def fileClick(todo: () ⇒ Unit) =
    onClick --> { _ ⇒
      plusFile.set(false)
      val currentMultiTool = multiTool.signal.now()
      if (currentMultiTool == Off || currentMultiTool == Paste) todo()
      fileToolBar.filterToolOpen.set(false)
      treeNodeManager.computeCurrentSons()
    }

  case class ReactiveLine(id: Int, tn: TreeNode, treeNodeType: TreeNodeType, todo: () ⇒ Unit) {

    val tnSafePath = treeNodeManager.dirNodeLine.now() ++ tn.name

    // val selected = Var(false)
    def isSelected(selection: Seq[SafePath]) = selection.contains(tnSafePath)

    def dirBox(tn: TreeNode) =
      div(
        child <-- multiTool.signal.combineWith(treeNodeManager.selected.signal).map { case (mcot, selected) ⇒
          if (mcot == CopyOrTrash) checkbox(isSelected(selected)).amend(onClick --> { _ ⇒
              treeNodeManager.switchSelection(tnSafePath)
          })
          else {
            tn match {
              case _: DirNode ⇒ div(cls := "dir plus bi-plus", cursor.pointer)
              case f: FileNode ⇒
                if (f.pluginState.isPlugin) {
                  div("P", cls := "plugin-file" + {
                    if (f.pluginState.isPlugged) " plugged"
                    else " unplugged"
                  })
                }
                else emptyNode
            }
          }
        }
      )

    val toolBox = new FileToolBox(tnSafePath, showExecution, treeNodeTabs, tn match {
      case f: FileNode ⇒ PluginState(f.pluginState.isPlugin, f.pluginState.isPlugged)
      case _ ⇒ PluginState(false, false)
    })

    val render: HtmlElement = {
      div(display.flex, flexDirection.column,
        div(display.flex, alignItems.center, lineHeight := "27px",
          backgroundColor <-- treeNodeManager.selected.signal.map { s ⇒ if (isSelected(s)) toolBoxColor else "" },
          dropPairs,
          onDragStart --> { e ⇒
            e.dataTransfer.setData("text/plain", "nothing") //  FIREFOX TRICK
            draggedNode.set(Some(tnSafePath))
          },
          onDrop --> { e ⇒
            e.dataTransfer
            e.preventDefault()
            dropAction(treeNodeManager.dirNodeLine.now() ++ tn.name, tn match {
              case _: DirNode ⇒ true
              case _ ⇒ false
            })
          },
          dirBox(tn).amend(cls := "file0", fileClick(todo), draggable := true),
          div(tn.name,
            cls.toggle("cursor-pointer") <-- multiTool.signal.map { mt ⇒
              mt == Off || mt == Paste
            },
            cls := "file1", fileClick(todo), draggable := true),
          i(timeOrSize(tn), cls := "file2"),
          button(cls := "bi-three-dots transparent-button", cursor.pointer, opacity := "0.5", onClick --> { _ ⇒
            currentSafePath.set(Some(tnSafePath))
            currentLine.set(
              if (id == currentLine.now()) -1
              else id
            )
          })
        ),
        currentLine.signal.map { i ⇒ i == id }.expand(toolBox.contentRoot)
      )
    }
  }

  def dropPairs = Seq(
    draggable := true,
    onDragEnter --> { e ⇒
      val el = e.target.asInstanceOf[HTMLElement]
      val style = new CSSStyleDeclaration()
      style.backgroundColor = "red"
      el.style = style
    },
    onDragLeave --> { e ⇒
      val style = new CSSStyleDeclaration
      style.backgroundColor = "transparent"
      e.target.asInstanceOf[HTMLElement].style = style
    },
    onDragOver --> { e ⇒
      e.dataTransfer.dropEffect = "move"
      e.preventDefault()
    }
  )

  def dropAction(to: SafePath, isDir: Boolean) = {
    draggedNode.now().map {
      dragged ⇒
        if (isDir) {
          if (dragged != to) {
            //treeNodeTabs.saveAllTabs(() ⇒ {
            Fetch.future(_.move(dragged, to).future).foreach {
              b ⇒
                treeNodeManager.invalidCache(to)
                treeNodeManager.invalidCache(dragged)
                treeNodeManager.invalidCurrentCache
                TabContent.checkTabs
            }
            //})
          }
        }
    }
    draggedNode.set(None)
  }

  def drawNode(node: TreeNode, i: Int) = {
    node match {
      case fn: FileNode ⇒
        ReactiveLine(i, fn, TreeNodeType.file, () ⇒ {
          displayNode(fn)
        })
      case dn: DirNode ⇒ ReactiveLine(i, dn, TreeNodeType.folder, () ⇒ {
        treeNodeManager switch (dn.name)
        treeWarning.set(true)
      })
    }
  }

}
