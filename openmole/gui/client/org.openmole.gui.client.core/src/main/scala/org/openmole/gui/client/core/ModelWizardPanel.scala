package org.openmole.gui.client.core

/*
 * Copyright (C) 07/10/15 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.client.core.files.*
import org.openmole.gui.shared.data.*
import org.openmole.gui.shared.data.FileType.*
import org.scalajs.dom.html.TextArea

import scala.concurrent.ExecutionContext.Implicits.global
import org.openmole.gui.client.ext.*
import com.raquo.laminar.api.L.*
import Waiter.*
import org.openmole.gui.client.ext.FileManager
import org.openmole.gui.shared.data.DataUtils.*
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*
import org.openmole.gui.client.tool.{Component, OMTags, OptionsDiv, TagBadge}
import org.openmole.gui.shared.api.*
import scaladget.bootstrapnative.Selector.Options

import scala.concurrent.Future

object ModelWizardPanel:
  
  implicit def stringToOptionString(s: String): Option[String] = if (s.isEmpty) None else Some(s)

  val filePath: Var[Option[SafePath]] = Var(None)
  val transferring: Var[ProcessState] = Var(Processed())
  val labelName: Var[Option[String]] = Var(None)
  val modelMetadata: Var[Option[ModelMetadata]] = Var(None)
  val resources: Var[Resources] = Var(Resources.empty)
  
  def successDiv = div(cls := "bi bi-patch-check-fill successBadge")

  object exclusiveMenu {

    val entrySet: Var[Seq[Int]] = Var(Seq())
    val onoff: Var[Option[Int]] = Var(None)

    def onoffUpdate(currentId: Option[Int], id: Int) = {
      if (Some(id) == currentId) None
      else Some(id)
    }

    def expandAction(i: Option[Int], butId: Int) = {
      i match {
        case Some(ii: Int) =>
          if (ii == butId) true
          else false
        case None => false
      }
    }

    def entry(name: String, id: Int, panel: HtmlElement) = {
      div(flexRow,
        button(
          name,
          width := "150px", margin := "10 -5 10 25",
          btn_primary,
          onClick --> { _ => onoff.update(i => onoffUpdate(i, id)) }
        ),
        child <-- entrySet.signal.map { es =>
          if es.contains(id)
          then successDiv
          else emptyNode
        },
        onoff.signal.map(oo => expandAction(oo, id)).expand(panel)
      )
    }
  }

  def render(using api: ServerAPI, basePath: BasePath, panels: Panels, plugins: GUIPlugins) = {
    given NotificationService = NotificationManager.toService(panels.notifications)

    def factory(directory: SafePath, uploaded: Seq[RelativePath]): Option[WizardPluginFactory] =
      plugins.wizardFactories.filter { _.accept(directory, uploaded) }.headOption

    def buildScriptFrom(fInput: Input, targetPath: SafePath) =
      api.temporaryDirectory().flatMap { tempFile ⇒
        api.upload(
          fInput.ref.files,
          tempFile,
          (p: ProcessState) ⇒ transferring.set(p)
        ).map { uploaded ⇒
          factory(tempFile, uploaded).foreach(w => println(w.name))
//          val contentPath =
//            if uploaded.size == 1
//            then
//              val uploadedFile = tempFile ++ uploaded.head
//              val extracted =
//                FileType(uploadedFile) match
//                  case Archive ⇒
//                    val extractDirectory = tempFile ++ "__extract__"
//                    api.extractArchive(uploadedFile, extractDirector)
//                    extractDirectory
//                  case _ ⇒ uploadedFile
//
//              extracted.
//            else tempFile
//
//          def copyTo(targetPath: SafePath) =
//            // Not sure why...
//            val from =
//              CoreUtils.listFiles(tempFile).map { files =>
//                if files.size == 1 && files.head.directory.isDefined
//                then tempFile ++ files.head.name
//                else tempFile
//              }
//
//            for
//              f <- from
//              _ <- api.copyFiles(Seq(f -> targetPath), overwrite = true)
//            do
//              factory(targetPath).foreach { f =>
//                f.parse(targetPath).foreach { mmd =>
//                  modelMetadata.set(mmd)
//                  exclusiveMenu.onoff.set(Some(1))
//                }
//              }
//
        }.andThen(_ => api.deleteFiles(Seq(tempFile)))
      }

    val upButton =
      label(
        cls := "inputFileStyle",
        margin := "15px",
        transferring.withTransferWaiter {
          _ ⇒
            div(
              OMTags.omFileInput(fInput ⇒
                if fInput.ref.files.length > 0
                then
                  val fileName = fInput.ref.files.item(0).name
                  labelName.set(Some(fileName))
                  val targetPath = Some(panels.treeNodePanel.treeNodeManager.dirNodeLine.now() ++ fileName)
                  filePath.set(targetPath)
                  buildScriptFrom(fInput, panels.treeNodePanel.treeNodeManager.dirNodeLine.now()).andThen { _ => fInput.ref.value = "" }
              ),
              div(
                child <-- labelName.signal.map {
                  _ match {
                    case Some(s: String) ⇒
                      div(
                        flexRow,
                        div(s, btn_primary, cls := " badgeUploadModel"),
                        successDiv
                      )
                    case x: Any =>
                      div("Upload", btn_primary, cls := "badgeUploadModel")
                  }
                }
              )
            )
        }
      )

    val inputTags = new TagBadge()
    val outputTags = new TagBadge()

    val commandeInput = inputTag("").amend(placeholder := "Launching command")

    def ioTagBuilder(initialI: Seq[String], initialO: Seq[String]) = div(
      div(cls := "verticalFormItem", div("Inputs", width := "100px", margin := "15px"), inputTags.render(initialI)),
      div(cls := "verticalFormItem", div("Outputs", width := "100px", margin := "15px"), outputTags.render(initialO))
    )

    def inferProtoTyePair(param: String) = {
      val defaultPrototype = PrototypePair(param.clean, PrototypeData.Double, "0.0", Some(param))

      modelMetadata.now() match {
        case Some(mmd: ModelMetadata) => (mmd.inputs ++ mmd.outputs).find(p => p.name == param).getOrElse(defaultPrototype)
        case _ => defaultPrototype
      }
    }

    def browseToPath(safePath: SafePath)(using panels: Panels) =
      a(safePath.path.mkString, onClick --> { _ ⇒ panels.treeNodePanel.treeNodeManager.switch(safePath.parent)})

    def buildTask(safePath: SafePath)(using panels: Panels) = ???
//      factory(safePath).foreach { f =>
//        modelMetadata.now().foreach { mmd =>
//          val modifiedMMD = mmd.copy(
//            inputs = inputTags.tags.now().map { t => inferProtoTyePair(t.ref.innerText) },
//            outputs = outputTags.tags.now().map { t => inferProtoTyePair(t.ref.innerText) },
//            command = commandeInput.ref.value
//          )
//          f.toTask(safePath.parent, modifiedMMD).foreach {_ =>
//            panels.treeNodePanel.treeNodeManager.invalidCache(safePath.parent)
//          }
//        }
//      }


    val overwriteSwitch = Component.Switch("Overwrite exitsting files", true, "autoCleanExecSwitch")

    val buildButton = button("Build", width := "150px", margin := "40 25 10 25", OMTags.btn_purple,
      onClick --> {
        _ ⇒
          filePath.now().foreach { fp =>
            buildTask(fp)
            Panels.closeExpandable
          }
      })

    val IOObserver = Observer[Seq[Span]](tb =>
      if (tb.isEmpty) exclusiveMenu.entrySet.update(_.filterNot(_ == 1))
      else exclusiveMenu.entrySet.update(es => (es :+ 1).distinct)
    )

    div(flexColumn, marginTop := "20",
      div(flexRow, width := "100%",
        upButton,
        span(display.flex, alignItems.center, color.black, marginLeft := "10px",
          child <-- panels.treeNodePanel.treeNodeManager.dirNodeLine.signal.combineWith(filePath.signal).map { case (sp, uploadedPath) =>
            uploadedPath match {
              case Some(p: SafePath) => span("Uploaded in ", browseToPath(p))
              case _ => span("Your model will be uploaded in ", browseToPath(sp))
            }
          })),
      exclusiveMenu.entry("Inputs / Ouputs", 1,
        div(height := "200px",
          child <-- modelMetadata.signal.map {
            _ match {
              case Some(mmd: ModelMetadata) =>
                val text = mmd.language.map {
                  _.name
                }.getOrElse("Unknown language")
                ioTagBuilder(mmd.inputs.flatMap(_.mapping), mmd.outputs.flatMap(_.mapping))
              case _ => emptyNode
            }
          })),
      exclusiveMenu.entry("Command", 2, div(display.flex, commandeInput, height := "50px", margin := "10 40" +
        "")),
      div(
        buildButton,
        overwriteSwitch.element
      ),
      inputTags.tags --> IOObserver
    )
  }
