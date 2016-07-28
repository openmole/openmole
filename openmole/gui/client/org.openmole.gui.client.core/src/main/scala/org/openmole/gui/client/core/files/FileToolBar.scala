package org.openmole.gui.client.core.files

import fr.iscpif.scaladget.api.Select.SelectElement
import org.openmole.gui.client.core.{ OMPost, CoreUtils }
import org.openmole.gui.ext.data._
import org.openmole.gui.misc.js.OMTags
import org.openmole.gui.misc.utils.stylesheet
import org.openmole.gui.shared.Api
import org.scalajs.dom.html.Input
import scala.util.Try
import scalatags.JsDom.{ tags ⇒ tags }
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import org.openmole.gui.misc.utils.{ stylesheet ⇒ omsheet }
import fr.iscpif.scaladget.stylesheet.{ all ⇒ sheet }
import fr.iscpif.scaladget.api._
import omsheet._
import sheet._
import org.openmole.gui.misc.js.JsRxTags._
import org.openmole.gui.client.core.files.TreeNode._
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import autowire._
import bs._
import org.scalajs.dom.raw.{ HTMLButtonElement, HTMLSpanElement, HTMLInputElement }
import rx._
import org.openmole.gui.client.core.Waiter._

/*
 * Copyright (C) 20/01/16 // mathieu.leclaire@openmole.org
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

object FileToolBar {

  trait SelectedTool {
    def glyph: Glyphicon
  }

  object FilterTool extends SelectedTool {
    val glyph = glyph_filter
  }

  object TrashTool extends SelectedTool {
    val glyph = glyph_trash
  }

  object FileCreationTool extends SelectedTool {
    val glyph = glyph_plus
  }

  object PluginTool extends SelectedTool {
    val glyph = OMTags.glyph_plug
  }

  object CopyTool extends SelectedTool {
    val glyph = glyph_copy
  }

  object PasteTool extends SelectedTool {
    val glyph = glyph_paste
  }

  object RefreshTool extends SelectedTool {
    val glyph = glyph_refresh
  }

}

import FileToolBar._

class FileToolBar(treeNodePanel: TreeNodePanel) {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  val selectedTool: Var[Option[SelectedTool]] = Var(None)
  val transferring: Var[ProcessState] = Var(Processed())
  val fileFilter = Var(FileFilter.defaultFilter)
  val fileNumberThreshold = 100

  implicit def someIntToString(i: Option[Int]): String = i.map {
    _.toString
  }.getOrElse("")

  def resetFilter = {
    selectedTool() = None
    nameInput.value = ""
    thresholdInput.value = fileNumberThreshold.toString
    filterSubmit()
  }

  def buildSpan(tool: SelectedTool, todo: () ⇒ Unit, modifierSeq: ModifierSeq = emptyMod): Rx[TypedTag[HTMLSpanElement]] = Rx {
    span(
      tool.glyph +++ pointer +++ selectedTool().filter(_ == tool).map { _ ⇒ modifierSeq +++ stylesheet.selectedTool }.getOrElse(emptyMod) +++ "glyphmenu",
      onclick := todo
    )
  }

  def buildAndSelectSpan(tool: SelectedTool, todo: Boolean ⇒ Unit = (Boolean) ⇒ {}): Rx[TypedTag[HTMLSpanElement]] = buildSpan(tool, { () ⇒
    val isSelectedTool = selectedTool.now == Some(tool)
    if (isSelectedTool) unselectTool
    else {
      selectedTool() = Some(tool)
      selectedTool.now match {
        case Some(CopyTool | TrashTool) ⇒ treeNodePanel.turnSelectionTo(true)
        case Some(PluginTool) ⇒
          manager.computePluggables(() ⇒
            if (manager.pluggables.now.isEmpty)
              println("Empty")
            //AlertPanel.string("No plugin has been found in this folder", okaction = { () ⇒ unselectTool }, cancelaction = { () ⇒ unselectTool }, transform = RelativeCenterPosition, zone = FileZone)
            else
              treeNodePanel.turnSelectionTo(true))
        case _ ⇒
      }
    }
    // todo(isSelectedTool)
  })

  def fInputMultiple(todo: HTMLInputElement ⇒ Unit) = {
    lazy val input: HTMLInputElement = bs.input()(ms("upload"), `type` := "file", multiple := "")(onchange := { () ⇒
      todo(input)
    }).render
    input
  }

  //Upload tool
  def upbtn(todo: HTMLInputElement ⇒ Unit): TypedTag[HTMLSpanElement] =
    span(aria.hidden := "true", glyph_upload +++ "fileUpload glyphmenu")(
      fInputMultiple(todo)
    )

  private val upButton = upbtn((fileInput: HTMLInputElement) ⇒ {
    FileManager.upload(fileInput, manager.current.now.safePath.now, (p: ProcessState) ⇒ transferring() = p, UploadProject(), () ⇒ treeNodePanel.refreshAndDraw)
  })

  // New file tool
  val newNodeInput: Input = bs.input()(
    placeholder := "File name",
    width := "130px",
    left := "-2px",
    autofocus
  ).render

  lazy val addRootDirButton: Select[TreeNodeType] = {
    val contents: Seq[SelectElement[TreeNodeType]] = Seq(SelectElement(TreeNodeType.file, glyph_file +++ sheet.paddingRight(3)), SelectElement(TreeNodeType.folder, glyph_folder_close +++ sheet.paddingRight(3)))
    contents.select(Some(TreeNodeType.file), (tnt: TreeNodeType) ⇒ tnt.name, btn_default +++ borderRightFlat, onclickExtra = () ⇒ {
      addRootDirButton.content.now.foreach { c ⇒
        newNodeInput.placeholder = c.value.name + " name"
      }
    })
  }

  // Filter tool
  val thresholdTag = "threshold"
  val nameTag = "names"

  val thresholdInput = bs.input(fileNumberThreshold.toString)(
    id := thresholdTag,
    width := "50px",
    autofocus
  ).render

  val nameInput = bs.input("")(
    id := nameTag,
    width := "70px",
    autofocus
  ).render

  def filterSubmit = () ⇒ {
    updateFilter(fileFilter.now.copy(threshold = thresholdInput.value, nameFilter = nameInput.value))
    false
  }

  val filterTool = tags.div(centerElement)(
    tags.span(tdStyle)(label("# of entries ")(labelStyle)),
    tags.span(tdStyle)(form(thresholdInput, onsubmit := filterSubmit)),
    tags.span(tdStyle)(label("name ")(`for` := nameTag, labelStyle)),
    tags.span(tdStyle)(form(nameInput, onsubmit := filterSubmit))
  )

  def createNewNode = {
    val newFile = newNodeInput.value
    val currentDirNode = manager.current
    addRootDirButton.content.now.foreach {
      _.value match {
        case dt: DirNodeType  ⇒ CoreUtils.addDirectory(currentDirNode.now, newFile, () ⇒ unselectAndRefreshTree)
        case ft: FileNodeType ⇒ CoreUtils.addFile(currentDirNode.now, newFile, () ⇒ unselectAndRefreshTree)
      }
    }
  }

  val createFileTool = bs.inputGroup()(
    bs.inputGroupButton(addRootDirButton.selector),
    form(newNodeInput, onsubmit := { () ⇒
      createNewNode
      false
    })
  ).render

  def unselectAndRefreshTree: Unit = {
    unselectTool
    newNodeInput.value = ""
    treeNodePanel.refreshAndDraw
  }

  def unselectTool = {
    resetFilter
    treeNodePanel.turnSelectionTo(false)
    selectedTool() = None
  }

  val deleteButton = bs.button("Delete", btn_danger, () ⇒ {
    CoreUtils.trashNodes(manager.selected.now) { () ⇒
      unselectAndRefreshTree
    }
  })

  val copyButton = bs.button("Copy", btn_default, () ⇒ {
    manager.setSelectedAsCopied
    unselectTool
  })

  val pluginButton = bs.button("Plug", btn_default, () ⇒ {
    OMPost[Api].copyToPluginDir(manager.selected.now).call().foreach { c ⇒
      OMPost[Api].addPlugins(manager.selected.now.map {
        _.name.now
      }).call().foreach { x ⇒
        unselectAndRefreshTree
      }
    }
  })

  //Filter
  implicit def stringToIntOption(s: String): Option[Int] = Try(s.toInt).toOption

  def updateFilter(newFilter: FileFilter) = {
    fileFilter() = newFilter
    treeNodePanel.refreshAndDraw
  }

  def switchAlphaSorting = updateFilter(fileFilter.now.switchTo(AlphaSorting))

  def switchTimeSorting = updateFilter(fileFilter.now.switchTo(TimeSorting))

  def switchSizeSorting = updateFilter(fileFilter.now.switchTo(SizeSorting))

  val sortingGroup = {
    val topTriangle = glyph_triangle_top +++ (fontSize := 10)
    val bottomTriangle = glyph_triangle_bottom +++ (fontSize := 10)
    bs.exclusiveButtonGroup(stylesheet.sortingBar, ms("sortingTool"), ms("selectedSortingTool"))(
      ExclusiveButton.twoGlyphSpan(
        topTriangle,
        bottomTriangle,
        () ⇒ switchAlphaSorting,
        () ⇒ switchAlphaSorting,
        preString = "Aa",
        ms("sortingTool")
      ),
      ExclusiveButton.twoGlyphButtonStates(
        topTriangle,
        bottomTriangle,
        () ⇒ switchTimeSorting,
        () ⇒ switchTimeSorting,
        preGlyph = twoGlyphButton +++ glyph_time
      ),
      ExclusiveButton.twoGlyphButtonStates(
        topTriangle,
        bottomTriangle,
        () ⇒ switchSizeSorting,
        () ⇒ switchSizeSorting,
        preGlyph = twoGlyphButton +++ OMTags.glyph_data +++ sheet.paddingTop(10) +++ (fontSize := 12)
      )
    )
  }

  def getIfSelected(butt: TypedTag[HTMLButtonElement]) = manager.selected.map { m ⇒
    if (m.isEmpty) tags.div else butt
  }

  val fileToolDiv = Rx {
    tags.div(centerElement +++ sheet.marginBottom(10))(
      selectedTool() match {
        case Some(FilterTool)       ⇒ filterTool
        case Some(FileCreationTool) ⇒ createFileTool
        case Some(TrashTool)        ⇒ getIfSelected(deleteButton)
        case Some(PluginTool)       ⇒ getIfSelected(pluginButton)
        case Some(CopyTool) ⇒
          manager.emptyCopied
          getIfSelected(copyButton)
        case _ ⇒ tags.div()
      },
      transferring.withTransferWaiter { _ ⇒
        tags.div()
      }
    )
  }

  val div = tags.div(
    tags.div(centerElement)(
      buildSpan(RefreshTool, () ⇒ treeNodePanel.refreshAndDraw),
      upButton,
      buildAndSelectSpan(PluginTool),
      buildAndSelectSpan(TrashTool),
      buildAndSelectSpan(CopyTool),
      buildAndSelectSpan(FileCreationTool),
      buildAndSelectSpan(FilterTool)
    ),
    fileToolDiv
  )

}
