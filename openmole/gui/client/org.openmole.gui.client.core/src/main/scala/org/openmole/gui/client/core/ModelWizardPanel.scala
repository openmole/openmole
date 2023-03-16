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

  case class ParsedModelMetadata(data: ModelMetadata, files: Seq[RelativePath], factory: WizardPluginFactory)

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

  def render(using api: ServerAPI, basePath: BasePath, panels: Panels, plugins: GUIPlugins) =
    given NotificationService = NotificationManager.toService(panels.notifications)

    //val filePath: Var[Option[SafePath]] = Var(None)
    val transferring: Var[ProcessState] = Var(Processed())
    val modelMetadata: Var[Option[ParsedModelMetadata]] = Var(None)
    //val resources: Var[Resources] = Var(Resources.empty)

    def factory(directory: SafePath, uploaded: Seq[RelativePath]): Option[WizardPluginFactory] =
      plugins.wizardFactories.filter { _.accept(directory, uploaded) }.headOption

    def uploadAndParse(tmpDirectory: SafePath, fInput: Input): Future[Unit] =
        api.deleteFiles(Seq(tmpDirectory)).andThen(_ =>
          api.upload(
            fInput.ref.files,
            tmpDirectory,
            (p: ProcessState) ⇒ transferring.set(p)
          ).flatMap { uploaded ⇒
            factory(tmpDirectory, uploaded).headOption match
              case Some(factory) =>
                factory.parse(tmpDirectory, uploaded).map {
                  md => modelMetadata.set(Some(ParsedModelMetadata(md, uploaded, factory)))
                }
              case None =>
                panels.notifications.addAndShowNotificaton(NotificationLevel.Info, "No wizard available for your model", div(s"No wizard found for: ${uploaded.map(_.mkString).mkString(", ")}"))
                Future.successful(())
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
          }//.andThen(_ => api.deleteFiles(Seq(tempFile)))
        )

    val uploadDirectorySwitch = Component.Switch("Upload a directory", false, "autoCleanExecSwitch")
    val uploadDirectory = Var(false)

    def upButton(tmpDirectory: SafePath) =
      label(
        cls := "inputFileStyle",
        margin := "15px",
        transferring.withTransferWaiter {
          _ ⇒
            div(
              child <--
                uploadDirectory.signal.map { directory =>
                  OMTags.omFileInput(fInput ⇒
                    if fInput.ref.files.length > 0
                    then uploadAndParse(tmpDirectory, fInput).andThen { _ => fInput.ref.value = "" },
                    directory = directory
                  )
                },
              div(
                child <-- modelMetadata.signal.map {
                  _ match {
                    case Some(mmd) ⇒
                      div(
                        flexRow,
                        div(mmd.files.map(p => p.mkString).mkString(", "), btn_primary, cls := " badgeUploadModel"),
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

    def inferProtoTyePair(param: String) =
      val defaultPrototype = PrototypePair(param.clean, PrototypeData.Double, "0.0", Some(param))

      modelMetadata.now() match
        case Some(mmd) => (mmd.data.inputs ++ mmd.data.outputs).find(p => p.name == param).getOrElse(defaultPrototype)
        case _ => defaultPrototype

    def browseToPath(safePath: SafePath)(using panels: Panels) =
      a(safePath.path.mkString, onClick --> { _ ⇒ panels.treeNodePanel.treeNodeManager.switch(safePath.parent)})

    def buildTask(safePath: SafePath, tmpDirectory: SafePath)(using panels: Panels) =
      modelMetadata.now().foreach { md =>
        val modifiedMMD =
          md.data.copy(
            inputs = inputTags.tags.now().map { t => inferProtoTyePair(t.ref.innerText) },
            outputs = outputTags.tags.now().map { t => inferProtoTyePair(t.ref.innerText) },
            command = commandeInput.ref.value
          )
        md.factory.content(tmpDirectory, md.files, modifiedMMD).foreach(println)
      }
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



    def buildButton(tmpDirectory: SafePath) =
      button("Build", width := "150px", margin := "0 25 10 25", OMTags.btn_purple,
        onClick --> {
          _ ⇒
            val targetPath = panels.directory.now()
            buildTask(targetPath, tmpDirectory)
            modelMetadata.set(None)
            panels.closeExpandable
        }
      )

    val IOObserver = Observer[Seq[Span]](tb =>
      if (tb.isEmpty) exclusiveMenu.entrySet.update(_.filterNot(_ == 1))
      else exclusiveMenu.entrySet.update(es => (es :+ 1).distinct)
    )

    div(flexColumn, marginTop := "20",
      children <-- Signal.fromFuture(api.temporaryDirectory()).map {
        case None => Seq()
        case Some(tmpDirectory) =>
          Seq(
            div(flexRow, width := "100%",
              upButton(tmpDirectory),
              span(display.flex, alignItems.center, color.black, marginLeft := "10px",
                child <-- (modelMetadata.signal combineWith panels.directory.signal).map {
                  case (Some(md), _) => span(s"""Use wizard "${md.factory.name}" for: ${md.files.map(_.mkString).mkString(", ")}""")
                  case (_, d) => span(s"Task will be built in ⌂/${d.path.mkString}")
                })
            ),
            exclusiveMenu.entry("Inputs / Ouputs", 1,
              div(height := "200px",
                child <--
                  modelMetadata.signal.map {
                    case Some(mmd) => ioTagBuilder(mmd.data.inputs.flatMap(_.mapping), mmd.data.outputs.flatMap(_.mapping))
                    case _ => emptyNode
                  }
              )
            ),
            exclusiveMenu.entry("Command", 2, div(display.flex, commandeInput, height := "50px", margin := "10 40")),
            div(flexRow, width := "100%", marginTop := "40",
              buildButton(tmpDirectory),
              span(display.flex, alignItems.center, color.black, marginLeft := "10px", marginBottom := "10px",
                child <-- (modelMetadata.signal combineWith panels.directory.signal).map {
                  case (Some(md), d) => span(s"${md.factory.name} task will be built in ⌂/${d.path.mkString}")
                  case _ => span()
                }),
            ),
            uploadDirectorySwitch.element.amend(onClick --> { t => uploadDirectory.set(uploadDirectorySwitch.isChecked) })
//            div(
//              onUnmountCallback { _ => api.deleteFiles(Seq(tmpDirectory)) }
//            )
          )
      },
      inputTags.tags --> IOObserver
    )

