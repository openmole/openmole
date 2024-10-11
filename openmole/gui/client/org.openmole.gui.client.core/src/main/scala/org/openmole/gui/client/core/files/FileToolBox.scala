package org.openmole.gui.client.core.files

import org.openmole.gui.client.core.*

import scala.concurrent.ExecutionContext.Implicits.global
import scaladget.bootstrapnative.bsn.*
import org.openmole.gui.client.tool.OMTags
import org.openmole.gui.shared.data.*
import org.openmole.gui.client.ext.*
import com.raquo.laminar.api.L.*
import org.openmole.gui.client.ext
import org.openmole.gui.client.ext.ClientUtil
import org.openmole.gui.shared.api.*

object FileToolBox:
  def iconAction(icon: HESetters, text: String, todo: () ⇒ Unit) =
    div(fileActionItems, icon, text, onClick --> { _ ⇒ todo() })

  def glyphItemize(icon: HESetter) = icon.appended(cls := "glyphitem popover-item")


class FileToolBox(initSafePath: SafePath, showExecution: () ⇒ Unit, pluginState: PluginState, isDirectory: Boolean):

  import FileToolBox.{iconAction, glyphItemize}

  def closeToolBox(using panels: Panels) = panels.treeNodePanel.currentLine.set(-1)

  def download(using panels: Panels) =
    withSafePath: sp ⇒
      closeToolBox
      org.scalajs.dom.window.open(
        url = downloadFile(sp),
        target = "_blank"
      )

  def omrToCSV(using panels: Panels) =
    withSafePath: sp ⇒
      closeToolBox
      org.scalajs.dom.window.open(
        url = convertOMR(sp, GUIOMRContent.ExportFormat.CSV),
        target = "_blank"
      )

  def omrToJSON(using panels: Panels) =
    withSafePath: sp ⇒
      closeToolBox
      org.scalajs.dom.window.open(
        url = convertOMR(sp, GUIOMRContent.ExportFormat.JSON),
        target = "_blank"
      )

  def omrFiles(sp: SafePath)(using panels: Panels) =
    withSafePath: path =>
      closeToolBox
      org.scalajs.dom.window.open(
        url = downloadFile(sp, includeTopDirectoryInArchive = Some(false), name = Some(s"${path.nameWithoutExtension}-files")),
        target = "_blank"
      )

  def trash(using panels: Panels, api: ServerAPI, basePath: BasePath) = withSafePath { safePath ⇒
    closeToolBox
    CoreUtils.trashNodes(panels.treeNodePanel, Seq(safePath)).andThen { _ ⇒
      if isDirectory
      then panels.tabContent.closeNonExstingFiles
      else panels.tabContent.removeTab(safePath)
      panels.pluginPanel.getPlugins
      panels.treeNodePanel.refresh
    }
  }

  def duplicate(using panels: Panels, api: ServerAPI, basePath: BasePath) = withSafePath { sp ⇒
    val newName =
      val prefix = sp.path.name
      if (prefix.contains(".")) prefix.replaceFirst("[.]", "_1.")
      else prefix + "_1"
    closeToolBox
    api.copyFiles(Seq(sp -> (sp.parent ++ newName)), false) andThen { _ =>
      panels.treeNodePanel.refresh
    }
  }

  def extract(using panels: Panels, api: ServerAPI, basePath: BasePath) = withSafePath { sp ⇒
    api.extractArchive(sp, sp.parent).foreach { _ ⇒ panels.treeNodePanel.refresh }
    closeToolBox
  }

  def execute(using panels: Panels, api: ServerAPI, path: BasePath) =
    import scala.concurrent.duration._
    withSafePath { sp ⇒
      api.launchScript(sp, true).foreach { _ ⇒ showExecution() }
      closeToolBox
    }

  def commit(message: String)(using panels: Panels, api: ServerAPI, basePath: BasePath) = withSafePath { sp ⇒
    api.commitFiles(Seq(sp), message).foreach { _ ⇒ panels.treeNodePanel.refresh }
    closeToolBox
  }

  def add(using panels: Panels, api: ServerAPI, basePath: BasePath) = withSafePath { sp ⇒
    api.addFiles(Seq(sp)).foreach { _ ⇒ panels.treeNodePanel.refresh }
    closeToolBox
  }

  def revert(using panels: Panels, api: ServerAPI, basePath: BasePath, plugins: GUIPlugins) =
    withSafePath: sp ⇒
      api.revertFiles(Seq(sp)).foreach: _ ⇒
        panels.treeNodePanel.refresh
        if panels.tabContent.alreadyDisplayed(sp).isDefined
        then
          panels.tabContent.removeTab(sp)
          FileDisplayer.display(sp)
        closeToolBox

  def rename(safePath: SafePath, to: String)(using panels: Panels, api: ServerAPI, basePath: BasePath, plugins: GUIPlugins) =
    val newNode = safePath.parent ++ to

    def afterRename =
      actionEdit.set(None)
      actionConfirmation.set(None)
      if !isDirectory
      then panels.tabContent.rename(safePath, newNode)
      else panels.tabContent.closeNonExstingFiles

      panels.treeNodePanel.refresh
      panels.treeNodePanel.currentSafePath.set(Some(newNode))

      closeToolBox

    api.move(Seq(safePath -> newNode), overwrite = false).foreach: existing =>
      if existing.nonEmpty
      then
        actionConfirmation.set:
          Some:
            confirmation(s"Overwrite ${safePath.name}?", () ⇒
              api.move(Seq(safePath -> newNode), overwrite = true).foreach:  _ =>
                afterRename
            )
      else afterRename


  def plugOrUnplug(safePath: SafePath, pluginState: PluginState)(using panels: Panels, api: ServerAPI, basePath: BasePath) =
    pluginState.isPlugged match
      case true ⇒
        CoreUtils.removePlugin(safePath).foreach { _ ⇒
          panels.pluginPanel.getPlugins
          panels.treeNodePanel.refresh
        }
      case false ⇒
        CoreUtils.addPlugin(safePath).foreach: errors ⇒
          for e <- errors
            do panels.notifications.showGetItNotification(NotificationLevel.Error, "An error occurred while adding plugin", ClientUtil.errorTextArea(ErrorData.stackTrace(e)))
          panels.pluginPanel.getPlugins
          panels.treeNodePanel.refresh


  def withSafePath(action: SafePath ⇒ Unit)(using panels: Panels) =
    panels.treeNodePanel.currentSafePath.now().foreach: sp ⇒
      action(sp)


  val actionConfirmation: Var[Option[Div]] = Var(None)
  val actionEdit: Var[Option[Div]] = Var(None)

  def editForm(sp: SafePath, initText: String, ph: String, todo: String => Unit)(using panels: Panels, api: ServerAPI, basePath: BasePath, plugins: GUIPlugins): Div =
    val renameInput = inputTag(initText).amend(
      margin := "auto 10 auto 25",
      placeholder := ph,
      onMountFocus
    )

    div(
      child <-- actionConfirmation.signal.map:
        case Some(c) ⇒ c
        case None ⇒
          form(
            renameInput,
            onSubmit.preventDefault --> { _ ⇒ todo(renameInput.ref.value) }
          )
    )

  def confirmation(text: String, todo: () ⇒ Unit) =
    div(
      fileActions,
      div(text, width := "50%", margin := "10px"),
      div(fileItemCancel, "Cancel", onClick --> {
        _ ⇒ actionConfirmation.set(None)
      }),
      div(fileItemWarning, "OK", onClick --> {
        _ ⇒
          todo()
          actionConfirmation.set(None)
      })
    )

  def contentRoot(using panels: Panels, api: ServerAPI, basePath: BasePath, plugins: GUIPlugins) =
    div(
      height := "80px",
      child <-- actionConfirmation.signal.combineWith(actionEdit.signal).map:
        case (Some(ac), _) ⇒ ac
        case (_, Some(ae)) ⇒ ae
        case (None, None) ⇒
          div(
            fileActions,
            iconAction(glyphItemize(OMTags.glyph_arrow_left_right), "duplicate", () ⇒ duplicate).amend(verticalLine),
            iconAction(glyphItemize(glyph_edit), "rename",
              () ⇒ actionEdit.set(Some(editForm(initSafePath, initSafePath.name, "File name",
                (inputValue: String) =>
                  withSafePath { sp ⇒ rename(sp, inputValue) }
              )))).amend(verticalLine),
            iconAction(glyphItemize(glyph_download), "download", () ⇒ download).amend(verticalLine),
            FileContentType(initSafePath) match
              case FileContentType.OpenMOLEResult ⇒
                iconAction(glyphItemize(glyph_download), "csv", () ⇒ omrToCSV).amend(verticalLine)
              case _ ⇒ emptyMod
            ,
            FileContentType(initSafePath) match
              case FileContentType.OpenMOLEResult ⇒
                iconAction(glyphItemize(glyph_download), "json", () ⇒ omrToJSON).amend(verticalLine)
              case _ ⇒ emptyMod
            ,
            child <-- {
              FileContentType(initSafePath) match
                case FileContentType.OpenMOLEResult =>
                  Signal.fromFuture(api.omrFiles(initSafePath), None).map:
                    case Some(p) => iconAction(glyphItemize(glyph_download), "files", () ⇒ omrFiles(p)).amend(verticalLine)
                    case None => emptyNode
                case _ => Signal.fromValue(emptyNode)
            },
            FileContentType(initSafePath) match
              case FileContentType.TarGz | FileContentType.Tar | FileContentType.Zip | FileContentType.TarXz ⇒
                iconAction(glyphItemize(OMTags.glyph_extract), "extract", () ⇒ extract).amend(verticalLine)
              case _ ⇒ emptyMod
            ,
            FileContentType(initSafePath) match
              case FileContentType.OpenMOLEScript ⇒
                iconAction(glyphItemize(OMTags.glyph_flash), "run", () ⇒ execute).amend(verticalLine)
              case _ ⇒ emptyMod
            ,
            child <-- panels.treeNodePanel.addable.signal.map: ad =>
              if ad.contains(initSafePath.name)
              then iconAction(glyphItemize(OMTags.glyph_addFile), "add", () ⇒
                actionConfirmation.set(Some(confirmation(s"Add ${initSafePath.name} ?", () ⇒ add)))).amend(verticalLine)
              else emptyNode
            ,
            children <-- {
              panels.treeNodePanel.commitable.signal.map: co =>
                if co.contains(initSafePath.name)
                then
                  Seq(
                    div(OMTags.glyph_commit, fileActionItems, verticalLine, cls := "glyphitem popover-item", "commit", verticalLine, onClick --> { _ ⇒
                      actionEdit.set(Some(editForm(initSafePath, "", "Commit message",
                        (inputValue: String) => commit(inputValue)
                      )))
                    }),
                    iconAction(glyphItemize(OMTags.glyph_rollback), "revert", () ⇒
                      actionConfirmation.set(Some(confirmation(s"Revert ${initSafePath.name} ?", () ⇒ revert)))).amend(verticalLine)
                  )
                else Seq()
            },
            iconAction(glyphItemize(glyph_trash), "delete", () ⇒
              actionConfirmation.set(Some(confirmation(s"Delete ${initSafePath.name} ?", () ⇒ trash)))),

            //                FileContentType(initSafePath) match {
            //                  //FIXME discover extensions from wizard plugins
            //                  case FileContentType.Jar | FileContentType.NetLogo | FileContentType.R | FileContentType.TarGz ⇒
            //                    iconAction(glyphItemize(OMTags.glyph_share), "to OMS", () ⇒ toScript)
            //                  case _ ⇒ emptyMod
            //                },
            if pluginState.isPlugin
            then
              val (icon, text) =
                if pluginState.isPlugged
                then (OMTags.glyph_unpuzzle, "unplug")
                else (OMTags.glyph_puzzle, "plug")
              iconAction(glyphItemize(icon), text, () ⇒ plugOrUnplug(initSafePath, pluginState))
            else emptyMod
          )
    )

