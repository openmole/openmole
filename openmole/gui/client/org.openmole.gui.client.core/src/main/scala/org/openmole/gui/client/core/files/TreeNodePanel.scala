package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.CoreUtils
import org.openmole.gui.client.core.Waiter.*
import org.openmole.gui.shared.data.*
import org.openmole.gui.client.ext.*
import scaladget.bootstrapnative.bsn.*
import org.scalajs.dom.raw.*
import org.openmole.gui.client.core.*
import org.openmole.gui.client.ext.*

import scala.concurrent.ExecutionContext.Implicits.global
import TreeNode.*
import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveElement
import org.openmole.gui.client.core.files.FileToolBox.{glyphItemize, iconAction}
import org.openmole.gui.client.ext.FileManager
import org.openmole.gui.client.tool.OMTags
import org.openmole.gui.shared.api.*
import org.openmole.gui.shared.data.GitStatus.{Conflicting, Modified, Untracked}

import scala.collection.immutable.ArraySeq
import org.openmole.gui.client.core.files.TabContent.TabData

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

object TreeNodePanel:
  enum MultiTool:
    case On, PendingOperation, Off, Git

  extension (p: TreeNodePanel)
    def refresh = p.refresh

class TreeNodePanel {
  panel =>

  import TreeNodePanel.MultiTool
  import TreeNodePanel.MultiTool.*

  val treeNodeManager: TreeNodeManager = new TreeNodeManager
  val treeWarning = Var(true)
  val draggedNode: Var[Option[SafePath]] = Var(None)
  val update: Var[Long] = Var(0)
  val commitable: Var[Seq[String]] = Var(Seq())
  val addable: Var[Seq[String]] = Var(Seq())
  val gitFolder: Var[Boolean] = Var(false)

  def refresh = update.update(_ + 1)

  val fileToolBar = new FileToolBar(this, treeNodeManager)

  val editNodeInput = inputTag("").amend(
    placeholder := "Name",
    width := "240px",
    height := "24px",
    onMountFocus
  )

  val scriptErrors: Var[Seq[EditorPanelUI]] = Var(Seq())
  val plusFile = Var(false)
  
  // New file tool
  val newNodeInput =
    inputTag().amend(
      placeholder <-- directoryToggle.toggled.signal.map { d => if d then "New directory" else "New file" },
      width := "270px",
      marginLeft := "12px",
      inContext { thisNode ⇒ 
          plusFile.signal.toObservable --> Observer[Boolean] { e => if e then thisNode.ref.focus()}
     }
    )

  lazy val directoryToggle =
    object FileType
    val folder = ToggleState(FileType, "Folder", "btn purple-button", _ ⇒ {})
    val file = ToggleState(FileType, "File", "btn purple-button", _ ⇒ {})
    toggle(folder, false, file, () ⇒ {})

  def createNewNode(newFile: String)(using api: ServerAPI, basePath: BasePath, panels: Panels) =
    val currentDirNode = treeNodeManager.directory
    directoryToggle.toggled.now() match
      case true ⇒ CoreUtils.createFile(currentDirNode.now(), newFile, directory = true).map(_ => refresh)
      case false ⇒ CoreUtils.createFile(currentDirNode.now(), newFile).map(_ => refresh)

  //Upload tool
  val transferring: Var[ProcessState] = Var(Processed())

  def fInputMultiple(todo: Input ⇒ Unit) =
    inputTag().amend(cls := "upload", `type` := "file", multiple := true, OMTags.webkitdirectory <-- directoryToggle.toggled.signal, inContext { thisNode ⇒ onChange --> { _ ⇒ todo(thisNode) } })

  def upldoadButton(todo: Input ⇒ Unit): HtmlElement =
    span(aria.hidden := true, cls <-- directoryToggle.toggled.signal.map { d => if !d then "bi-cloud-upload" else "bi-cloud-upload-fill" }, cls := "fileUpload glyphmenu", margin := "10 0 10 12", fInputMultiple(todo)).
      tooltip("eranu").amend(dataAttr("original-title") <-- directoryToggle.toggled.signal.map { d => if !d then "Upload files" else "Upload directories" })

  private def upButton(using api: ServerAPI, basePath: BasePath) = upldoadButton((fileInput: Input) ⇒
    val current = treeNodeManager.directory.now()
    api.upload(
      fileInput.ref.files.toSeq.map(f => f -> current / f.path),
      (p: ProcessState) ⇒ transferring.set(p)
    ).map { _ =>
      fileInput.ref.value = ""
      refresh
    }
  )

  def createFileTool(using api: ServerAPI, basePath: BasePath, panels: Panels) =
    form(flexRow, alignItems.center, height := "70px", color.white, margin := "0 10 0 10",
      directoryToggle.element,
      newNodeInput.amend(marginLeft := "10px"),
      upButton.amend(justifyContent.flexEnd),
      transferring.withTransferWaiter {
        _ ⇒
          div()
      }.amend(marginLeft := "10px"),
      onSubmit.preventDefault --> { _ ⇒
        createNewNode(newNodeInput.ref.value)
        newNodeInput.ref.value = ""
        plusFile.set(false)
      })

  val confirmationDiv: Var[Option[Div]] = Var(None)

  def info(text: String): Div =
    div(
      fileActions,
      text,
      button(cls := "btn white-button", "OK", marginLeft := "10", onClick --> {
        _ ⇒
          closeMultiTool
      })
    )

  def confirmation(text: String, okText: String, todo: () ⇒ Unit): Div =
    confirmationWithMessage(div(text, width := "50%", margin := "10px"), okText, todo)

  def confirmationWithMessage(message: ReactiveElement[_], okText: String, todo: () => Unit): Div =
    div(
      fileActions,
      message,
      div(fileItemCancel, "Cancel", onClick --> { _ ⇒ closeMultiTool }),
      div(fileItemWarning, okText, onClick --> { _ ⇒ todo() })
    )

  def archiveFiles(sp: Seq[SafePath]) =
    multiTool.set(Off)
    if (!sp.isEmpty)
      org.scalajs.dom.window.open(
        url = downloadFiles(sp, name = Some(s"${sp.head.nameWithoutExtension}")),
        target = "_blank"
      )

  def copyFiles(selectedFiles: Seq[SafePath], target: SafePath)(using api: ServerAPI, basePath: BasePath) =
    def toTarget(target: SafePath)(p: SafePath) = p -> (target ++ p.name)

    api.copyFiles(selectedFiles.map(toTarget(target)), overwrite = false).foreach: existing ⇒
      if existing.isEmpty
      then
        refresh
        closeMultiTool
      else
        confirmationDiv.set:
          Some:
            confirmation(s"${existing.size} files have already the same name. Overwrite them?", "Overwrite", () ⇒
              api.copyFiles(selectedFiles.map(toTarget(target)), overwrite = true).foreach: b ⇒
                refresh
                closeMultiTool
            )


  def moveFiles(selectedFiles: Seq[SafePath], target: SafePath)(using api: ServerAPI, basePath: BasePath, panels: Panels) =
    def toTarget(target: SafePath)(p: SafePath) = p -> (target ++ p.name)

    def afterMove =
      refresh
      closeMultiTool
      panels.tabContent.closeNonExstingFiles

    api.move(selectedFiles.map(toTarget(target)), overwrite = false).foreach: existing ⇒
      if existing.isEmpty
      then afterMove
      else
        confirmationDiv.set:
          Some:
            confirmation(s"${existing.size} files have already the same name. Overwrite them?", "Overwrite", () ⇒
              api.move(selectedFiles.map(toTarget(target)), overwrite = true).foreach: _ =>
                afterMove
            )


  def copy(move: Boolean)(using api: ServerAPI, basePath: BasePath, panels: Panels) =
    multiTool.set(MultiTool.PendingOperation)

    confirmationDiv.set:
      Some:
        val selectedFiles = treeNodeManager.selected.now()
        def toTarget(target: SafePath)(p: SafePath) = p -> (target ++ p.name)

        confirmation(s"${selectedFiles.size} files copied. Browse to the target folder and press Paste", "Paste", () ⇒
          val target = treeNodeManager.directory.now()
          if move
          then moveFiles(selectedFiles, target)
          else copyFiles(selectedFiles, target)
        )


  def copyOrTrashTool(using api: ServerAPI, basePath: BasePath, panels: Panels) =
    div(
      height := "70px", flexRow, alignItems.center, color.white, justifyContent.spaceBetween,
      children <-- (commitable.signal combineWith addable.signal combineWith gitFolder.signal combineWith multiTool.signal).map: (co, ad, gf, mt) ⇒
        val selected = treeNodeManager.selected
        val isSelectionEmpty = selected.signal.map { _.isEmpty }

        def disableIfEmptyCls = cls <-- isSelectionEmpty.map: se =>
          if se then "disable" else ""

        def commit =
          val messageInput = inputTag().amend(
            placeholder := "Commit message", marginRight := "10",
             inContext{ctx =>
              if mt == MultiTool.Git then ctx.ref.focus()
              emptyMod
              }   
            )
          confirmationDiv.set(
            Some(
              confirmationWithMessage(messageInput, "Commit",
                () =>
                  val target = treeNodeManager.directory.now()
                  val commitMsg = messageInput.ref.value
                  if !commitMsg.isEmpty
                  then
                    api.commitFiles(selected.now(), commitMsg).foreach: _ =>
                      refresh
                      closeMultiTool
              )
            )
          )

        if (mt == MultiTool.On)
          Seq(
            iconAction(glyphItemize(OMTags.glyph_copy), "copy", () ⇒ copy(false))
              .amend(verticalLine, disableIfEmptyCls),
            iconAction(glyphItemize(OMTags.glyph_move), "move", () ⇒ copy(true))
              .amend(verticalLine, disableIfEmptyCls),
            iconAction(glyphItemize(glyph_download), "download", () ⇒ archiveFiles(selected.now()))
              .amend(verticalLine, disableIfEmptyCls),
            iconAction(glyphItemize(glyph_trash), "delete", () ⇒
              confirmationDiv.set(
                Some(confirmation(s"Delete ${treeNodeManager.selected.now().size} files ?", "OK", () ⇒
                  CoreUtils.trashNodes(this, treeNodeManager.selected.now()).andThen { _ ⇒ closeMultiTool }
                ))
              )
            ).amend(disableIfEmptyCls)
          )
        else if (mt == MultiTool.Git)
          Seq(
            if !ad.isEmpty
            then iconAction(glyphItemize(OMTags.glyph_addFile), "add", () ⇒
              confirmationDiv.set(
                Some(confirmation(s"Add ? ${treeNodeManager.selected.now().size} files ?", "OK", () ⇒
                  api.addFiles(treeNodeManager.selected.now()).andThen { _ ⇒ closeMultiTool }
                ))
              )
            ).amend(verticalLine)
            else emptyNode
            ,
            if !co.isEmpty
            then div(OMTags.glyph_commit, "commit", fileActionItems, verticalLine, cls := "glyphitem popover-item", onClick --> { _ ⇒ commit })
            else emptyNode
            ,
            div(OMTags.glyph_pull, "pull", paddingBottom := "20", fileActionItems, verticalLine, cls := "glyphitem popover-item",
              onClick --> { _ ⇒
                api.pull(treeNodeManager.directory.now()).foreach:
                  case MergeStatus.ChangeToBeResolved =>
                    confirmationDiv.set(Some(info("Merge impossible, first revert or commit your changes.")))
                  case _ => closeMultiTool
              }),
            div(OMTags.glyph_push, "push", fileActionItems, verticalLine, cls := "glyphitem popover-item", onClick --> { _ ⇒
              api.push(treeNodeManager.directory.now()).foreach:
                case PushStatus.Ok => confirmationDiv.set(Some(info("Push successful")))
                case PushStatus.AuthenticationRequired => confirmationDiv.set(Some(info("Push failed, an authentication is required")))
                case PushStatus.Failed => confirmationDiv.set(Some(info("Push failed")))
            }),
            if !co.isEmpty
            then div(OMTags.glyph_stash, fileActionItems, "stash", cls := "glyphitem popover-item", onClick --> { _ ⇒
              confirmationDiv.set(
                Some(confirmation("Stash changes ?", "OK", () ⇒
                  api.stash(treeNodeManager.directory.now()).andThen { _ ⇒ closeMultiTool }
                ))
              )
            }).amend(verticalLine)
            else emptyNode,
            if gf
            then div(OMTags.glyph_stash_pop, fileActionItems, "pop", cls := "glyphitem popover-item", onClick --> { _ ⇒
              confirmationDiv.set(
                Some(confirmation("Pop stashed changes ?", "OK", () ⇒
                  api.stashPop(treeNodeManager.directory.now()).foreach:
                    case MergeStatus.ChangeToBeResolved =>
                      confirmationDiv.set(Some(info("Merge impossible, first revert or commit your changes.")))
                    case _ => closeMultiTool
                ))
              )
            }).amend(if co.isEmpty then emptyMod else verticalLine)
            else emptyNode
            ,
            if !co.isEmpty
            then iconAction(glyphItemize(OMTags.glyph_rollback), "revert", () ⇒
              confirmationDiv.set(
                Some(confirmation(s"Revert changes ? ${treeNodeManager.selected.now().size} files ?", "OK", () ⇒
                  api.revertFiles(treeNodeManager.selected.now()).andThen { _ ⇒ closeMultiTool }
                ))
              )
            )
            else emptyNode
          )
        else Seq(emptyNode)
    )

  def closeMultiTool =
    multiTool.set(MultiTool.Off)
    confirmationDiv.set(None)
    treeNodeManager.clearSelection

  val multiTool: Var[MultiTool] = Var(MultiTool.Off)

  def initMultiTool(multiToolMode: MultiTool) =
    multiTool.update:
      case MultiTool.Off ⇒ multiToolMode
      case _ ⇒
        confirmationDiv.set(None)
        treeNodeManager.clearSelection
        MultiTool.Off

    multiTool.now() match
      case MultiTool.Off ⇒ refresh
      case _ ⇒

  def fileControler(using panels: Panels, api: ServerAPI, basePath: BasePath) =
    div(
      cls := "file-content",
      child <-- treeNodeManager.directory.signal.map { curr ⇒
        val parent = curr.parent
        div(
          cls := "tree-path",
          goToDirButton(treeNodeManager.root).amend(OMTags.glyph_house, padding := "0 10 5 0"),
          Seq(parent.parent, parent, curr).filterNot { sp ⇒
            sp.isEmpty || sp == treeNodeManager.root
          }.map { sp ⇒
            goToDirButton(sp, s"${sp.name} / ")
          },
          div(glyph_plus, cls <-- plusFile.signal.map { pf ⇒
            "plus-button" + {
              if (pf) " selected" else ""
            }
          }, onClick --> { _ ⇒ plusFile.update(!_) }),
        )
      },
      div(
        display.flex, justifyContent.flexEnd, marginTop := "10",
        div(flexRow, justifyContent.right,
          div(
            cls <-- fileToolBar.filterToolOpen.signal.map { o =>
              if (o) "open-transition" else "close-transition"
            },
            fileToolBar.filterTool
          )),
        div(cls := "toolBlock",
          child <--
            gitFolder.signal.map: gf =>
              if gf
              then
                div(cls := "specific-file git gitInfo", color := "white", cursor.pointer, marginRight := "18px",
                  onClick --> { _ ⇒
                    initMultiTool(MultiTool.Git)
                  })
              else emptyNode
          ,
          div(OMTags.glyph_search,
            cls := "filtering-files-item-selected",
            onClick --> { _ ⇒ fileToolBar.filterToolOpen.update(!_) }),
          div(glyph_refresh, cls := "treePathItems file-refresh", onClick --> { _ ⇒ refresh }),
          div(cls := "bi-three-dots-vertical treePathItems", fontSize := "20px", paddingRight := "15px", onClick --> { _ ⇒
            initMultiTool(MultiTool.On)
          })
        )
      ),
      plusFile.signal.expand(createFileTool),
      child <--
        confirmationDiv.signal.map:
          case Some(d) => div(height := "70px", flexRow, alignItems.center, color.white, justifyContent.spaceBetween, d)
          case None => multiTool.signal.map(_ != MultiTool.Off).expand(copyOrTrashTool),
      plusFile.toObservable --> Observer[Boolean]: v =>
        if !v then directoryToggle.toggled.set(false)
    )

  def downloadFile(safePath: SafePath, hash: Boolean)(using api: ServerAPI, basePath: BasePath) =
    api.download(
      safePath,
      (p: ProcessState) ⇒ {
        transferring.set(p)
      },
      hash = hash
    )


  def goToDirButton(safePath: SafePath, name: String = "")(using panels: Panels, api: ServerAPI, basePath: BasePath): HtmlElement =
    div(cls := "treePathItems", paddingLeft := "4px", name,
      onClick --> { _ ⇒
        clearErrorView(Some(safePath))
        treeNodeManager.switch(safePath)
      },
      dropPairs,
      onDrop --> { e ⇒
        e.dataTransfer
        e.preventDefault()
        dropAction(safePath, true)
      }
    )

  private def treeView(using panels: Panels, pluginServices: PluginServices, api: ServerAPI, basePath: BasePath, plugins: GUIPlugins): Div =
    val size = Var(100)
    div(
      cls := "file-scrollable-content",
      children <--
        (treeNodeManager.directory.signal combineWith treeNodeManager.findFilesContaining.signal combineWith multiTool.signal combineWith treeNodeManager.fileSorting.signal combineWith update.signal combineWith size.signal).flatMap { (currentDir, findString, foundFiles, multiTool, fileFilter, _, sizeValue) ⇒
          EventStream.fromFuture(CoreUtils.listFiles(currentDir, fileFilter.copy(size = Some(sizeValue)), withHidden = false).map(Some(_)), true).toSignal(None).map {
            case None =>
              Seq(
                i(cls := "bi bi-hourglass-split", marginLeft := "250", textAlign := "center")
              )
            case Some(nodes) =>
              val content =
                if !foundFiles.isEmpty
                then
                  foundFiles.map { (sp, isDir) =>
                    div(s"${sp.normalizedPathString}", cls := "findFile",
                      onClick --> { _ =>
                        fileToolBar.filterToolOpen.set(false)
                        treeNodeManager.resetFileFinder
                        fileToolBar.findInput.ref.value = ""
                        val switchTarget = if isDir then sp else sp.parent
                        treeNodeManager.switch(switchTarget)
                        //treeNodeManager.computeCurrentSons
                        displayNode(sp)
                      }
                    )
                  }
                else if currentDir == treeNodeManager.root && nodes.data.isEmpty
                then Seq(div("Create a first OpenMOLE script (.oms)", cls := "message"))
                else
                  val checked =
                    if multiTool == MultiTool.On || multiTool == MultiTool.Git
                    then
                      val allCheck: Input = checkbox(false)
                      allCheck.amend(
                        cls := "file0", marginBottom := "3px", onClick --> { _ ⇒
                          treeNodeManager.switchAllSelection(nodes.data.map { tn => currentDir ++ tn.name }, allCheck.ref.checked)
                        }
                      )
                    else emptyNode
                  fileToolBar.gitBranchList.set(nodes.branchData)
                  commitable.set(nodes.data.map(d => d.name -> d.gitStatus).filter(x => x._2 == Some(Untracked) || x._2 == Some(Conflicting) || x._2 == Some(Modified)).map(_._1))
                  addable.set(nodes.data.map(d => d.name -> d.gitStatus).filter(x => x._2 == Some(Untracked)).map(_._1))
                  gitFolder.set(nodes.data.headOption.map(tn => tn.gitStatus.isDefined && tn.gitStatus != Some(GitStatus.Root)).getOrElse(false))
                  checked +: nodes.data.zipWithIndex.flatMap { case (tn, id) => Seq(drawNode(tn, id).render) }

              def more =
                if nodes.listed < nodes.total
                then
                  Seq(
                    div(position := "absolute", bottom := "20", left := "250", cursor.pointer, textAlign := "center",
                      i(cls := "bi bi-plus"),
                      br(),
                      i(fontSize := "12", s"${nodes.listed}/${nodes.total}"),
                      onClick --> { _ => size.update(_ * 2) }
                    )
                  )
                else Seq()

              content ++ more
          }
        },
      treeNodeManager.directory.toObservable --> Observer { _ => size.set(100) }
    )

  def clearCurrentErrorView(using panels: Panels) =
    clearErrorView(panels.tabContent.current.now().map(_.safePath))
    
  def clearErrorView(safePath: Option[SafePath]) =
    scriptErrors.update: s=>
      val ePUI = s.find(pui=> Some(pui.safePath) == safePath)
      ePUI.map: pui=>
        pui.unsetErrors
        s.filterNot(_== pui)
      .getOrElse(s)

  def treeViewOrErrors(using panels: Panels, pluginServices: PluginServices, api: ServerAPI, basePath: BasePath, plugins: GUIPlugins): Div =
      div(
        child <-- scriptErrors.signal.combineWith(panels.tabContent.current.signal).map: (se,curTab)=> 
          curTab match
            case Some(td: TabData)=> 
              se.filter(_.safePath == td.safePath).headOption match
                case Some(e: EditorPanelUI)=> e.errorView
                case _=> treeView
            case _ => treeView
      )

  def displayNode(safePath: SafePath, refresh: Boolean = false)(using panels: Panels, api: ServerAPI, basePath: BasePath, plugins: GUIPlugins): Unit =
    if refresh then panels.tabContent.removeTab(safePath)
    files.FileDisplayer.display(safePath)

  def displayNode(tn: TreeNode)(using panels: Panels, api: ServerAPI, basePath: BasePath, plugins: GUIPlugins): Unit =
    tn match
      case tn: TreeNode.File ⇒
        val tnSafePath = treeNodeManager.directory.now() ++ tn.name
        displayNode(tnSafePath)
      case _ ⇒

  val currentSafePath: Var[Option[SafePath]] = Var(None)
  val currentLine = Var(-1)

  def timeOrSize(tn: TreeNode): String = treeNodeManager.fileSorting.now().fileSorting match {
    case ListSorting.TimeSorting ⇒ CoreUtils.longTimeToString(tn.time)
    case _ ⇒ CoreUtils.readableByteCountAsString(tn.size)
  }

  def fileClick(todo: () ⇒ Unit)(using api: ServerAPI, basePath: BasePath) =
    onClick --> { _ ⇒
      plusFile.set(false)
      val currentMultiTool = multiTool.signal.now()
      if (currentMultiTool == MultiTool.Off || currentMultiTool == MultiTool.PendingOperation) todo()
      fileToolBar.filterToolOpen.set(false)
      //treeNodeManager.computeCurrentSons
    }

  case class ReactiveLine(id: Int, tn: TreeNode, treeNodeType: TreeNodeType, todo: () ⇒ Unit) {
    val tnSafePath = treeNodeManager.directory.now() ++ tn.name

    def isSelected(selection: Seq[SafePath]) = selection.contains(tnSafePath)

    def dirBox(tn: TreeNode)(using plugins: GUIPlugins) =
      div(
        child <-- multiTool.signal.combineWith(treeNodeManager.selected.signal).map { case (mcot, selected) ⇒
          if (mcot == MultiTool.On || mcot == MultiTool.Git) checkbox(isSelected(selected)).amend(onClick --> { _ ⇒
            treeNodeManager.switchSelection(tnSafePath)
          })
          else {
            tn match
              case _: TreeNode.Directory ⇒
                tn.gitStatus match
                  case Some(GitStatus.Root) => div(cls := "specific-file git", cursor.pointer)
                  case _ => div(cls := "dir plus bi-plus", cursor.pointer)
              case f: TreeNode.File ⇒
                if (f.pluginState.isPlugin)
                then
                  div("P", cls := "specific-file" + {
                    if (f.pluginState.isPlugged) " plugged"
                    else " unplugged"
                  })
                else
                  FileContentType(tnSafePath) match
                    case FileContentType.OpenMOLEScript => div("S", cls := "specific-file oms")
                    case FileContentType.OpenMOLEResult => div("R", cls := "specific-file omr")
                    case _ => emptyNode
          }
        }
      )


    def toolBox(using api: ServerAPI, basePath: BasePath, panels: Panels, plugins: GUIPlugins) =
      val showExecution = () ⇒ ExecutionPanel.open
      FileToolBox(
        tnSafePath,
        showExecution,
        tn match
          case f: TreeNode.File ⇒ PluginState(f.pluginState.isPlugin, f.pluginState.isPlugged)
          case _ ⇒ PluginState(false, false)
        ,
        isDirectory = tn.directory.isDefined
      )

    def gitDivStatus(tn: TreeNodeData) =
      tn.gitStatus match
        case Some(GitStatus.Modified) => "git-status-modified"
        case Some(GitStatus.Untracked) => "git-status-untracked"
        case Some(GitStatus.Conflicting) => "git-status-conflicting"
        case _ => ""

    def render(using panels: Panels, api: ServerAPI, basePath: BasePath, plugins: GUIPlugins): HtmlElement =
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
            dropAction(treeNodeManager.directory.now() ++ tn.name, TreeNode.isDir(tn))
          },
          dirBox(tn).amend(cls := "file0", fileClick(todo), draggable := true),
          div(tn.name,
            cls.toggle("cursor-pointer") <-- multiTool.signal.map { mt ⇒
              mt == MultiTool.Off || mt == MultiTool.PendingOperation
            },
            cls := s"file1 ${gitDivStatus(tn)}", fileClick(todo), draggable := true
          ),
          i(timeOrSize(tn), cls := "file2"),
          child <--
            multiTool.signal.map:
              _ match
                case On | Git => emptyNode
                case _ =>
                  button(cls := "bi-three-dots transparent-button", cursor.pointer, opacity := "0.5", onClick --> { _ ⇒
                    currentSafePath.set(Some(tnSafePath))
                    currentLine.update(cl => if cl == id then -1 else id)
                  })
        ),
        currentLine.signal.map { i ⇒ i == id }.expand(toolBox.contentRoot),
        treeNodeManager.directory.toObservable --> Observer { _ => currentLine.set(-1) }
      )
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
      e.dataTransfer.dropEffect = org.scalajs.dom.DataTransferDropEffectKind.move
      e.preventDefault()
    }
  )

  def dropAction(to: SafePath, isDir: Boolean)(using panels: Panels, api: ServerAPI, basePath: BasePath) =
    draggedNode.now().foreach: dragged ⇒
      if isDir && dragged != to
      then moveFiles(Seq(dragged), to)
    draggedNode.set(None)

  def drawNode(node: TreeNode, i: Int)(using panels: Panels, plugins: GUIPlugins, api: ServerAPI, basePath: BasePath) =
    node match
      case fn: TreeNode.File ⇒
        ReactiveLine(i, fn, TreeNodeType.File, () ⇒ displayNode(fn))
      case dn: TreeNode.Directory ⇒
        ReactiveLine(
          i,
          dn,
          TreeNodeType.Folder,
          () ⇒
            treeNodeManager switch (dn.name)
            treeWarning.set(true)
        )
}
