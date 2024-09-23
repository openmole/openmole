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
import org.openmole.gui.client.ext.*
import org.openmole.gui.client.ext.wizard.*
import scaladget.bootstrapnative.bsn.*
import scaladget.tools.*
import org.openmole.gui.client.tool.{Component, OMTags, OptionsDiv, TagBadge}
import org.openmole.gui.shared.api.*
import scaladget.bootstrapnative.Selector.Options

import scala.concurrent.Future

object ModelWizardPanel:
  
  implicit def stringToOptionString(s: String): Option[String] = if (s.isEmpty) None else Some(s)

  case class ParsedModelMetadata(acceptedModel: AcceptedModel, data: ModelMetadata, files: Seq[(RelativePath, SafePath)], factory: WizardPluginFactory)

  def successDiv = div(cls := "bi bi-patch-check-fill successBadge")

  class ExclusiveMenu:
    val entrySet: Var[Seq[Int]] = Var(Seq())
    val onoff: Var[Option[Int]] = Var(None)

    def onoffUpdate(currentId: Option[Int], id: Int) =
      if currentId.contains(id)
      then None
      else Some(id)

    def expandAction(i: Option[Int], butId: Int) =
      i match
        case Some(ii: Int) =>
          if (ii == butId) true
          else false
        case None => false

    def entry(name: String, id: Int, panel: HtmlElement) =
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

  def render(using api: ServerAPI, basePath: BasePath, panels: Panels, plugins: GUIPlugins) =
    given NotificationService = NotificationManager.toService(panels.notifications)
    val exclusiveMenu = new ExclusiveMenu

    val transferring: Var[ProcessState] = Var(Processed())
    val modelMetadata: Var[Option[ParsedModelMetadata]] = Var(None)
    val currentDirectory: Var[SafePath] = Var(panels.directory.now())

    def factory(uploaded: Seq[(RelativePath, SafePath)]): Future[Option[(AcceptedModel, WizardPluginFactory)]] =
      val future =
        Future.sequence(
          plugins.wizardFactories.map { f => f.accept(uploaded).map(s => s.map(s => (s, f))) }
        )

      future.map(s => s.flatten.headOption)

    def uploadAndParse(tmpDirectory: SafePath, fInput: Input): Future[Unit] =
      val ret =
        for
          list <- api.listFiles(tmpDirectory, withHidden = true)
          _ <- api.deleteFiles(list.data.map(f => tmpDirectory / f.name))
          uploaded <- api.upload(fInput.ref.files.toSeq.map(f => f -> tmpDirectory / f.path), p ⇒ transferring.set(p))
          f <- factory(uploaded)
          _ <-
            f match
              case Some((accepted, factory)) =>
                factory.parse(uploaded, accepted).map: md =>
                  modelMetadata.set(Some(ParsedModelMetadata(accepted, md, uploaded, factory)))
              case None =>
                panels.notifications.addAndShowNotificaton(NotificationLevel.Info, "No wizard available for your model", div(s"No wizard found for: ${uploaded.map(_._1.mkString).mkString(", ")}"))
                Future.successful(())
        yield ()

      ret.andThen:
        case util.Failure(exception) => NotificationManager.toService(panels.notifications).notifyError(s"Error while parsing", exception)
        case util.Success(_) =>

    val uploadDirectorySwitch = Component.Switch("Upload a directory", false, "wizardControls")
    val uploadDirectory = Var(false)

    def upButton(tmpDirectory: SafePath) =
      label(
        cls := "inputFileStyle",
        margin := "15px",
        transferring.withTransferWaiter:
          _ ⇒
            div(
              child <--
                uploadDirectory.signal.map: directory =>
                  OMTags.omFileInput(fInput ⇒
                    if fInput.ref.files.length > 0
                    then uploadAndParse(tmpDirectory, fInput).andThen { _ => fInput.ref.value = "" },
                    directory = directory
                  ),
              div(
                child <-- modelMetadata.signal.map:
                  case Some(mmd) ⇒
                    div(
                      flexRow,
                      div(mmd.files.map(p => p._1.mkString).mkString(", "), btn_primary, cls := " badgeUploadModel"),
                      successDiv
                    )
                  case x: Any => div("Upload", btn_primary, cls := "badgeUploadModel")
              )
            )
      )

    val inputTags = new TagBadge()
    val outputTags = new TagBadge()

    val commandInput = textArea(placeholder := "Launching command", height := "100", width := "400", overflow.scroll, value <-- modelMetadata.signal.map(m => m.flatMap(_.data.command).getOrElse("")))

    def ioTagBuilder(initialI: Seq[String], initialO: Seq[String]) = div(
      div(cls := "verticalFormItem", div("Inputs", width := "100px", margin := "15px"), inputTags.render(initialI)),
      div(cls := "verticalFormItem", div("Outputs", width := "100px", margin := "15px"), outputTags.render(initialO))
    )

    def inferProtoTyePair(param: String, mmd: ParsedModelMetadata) =
      val variableName = WizardUtils.toVariableName(param)
      val defaultPrototype = PrototypeData(variableName, PrototypeData.Double, "0.0", Some(param))
      (mmd.data.inputs ++ mmd.data.outputs).find(p => p.name == variableName).getOrElse(defaultPrototype)

    def browseToPath(safePath: SafePath)(using panels: Panels) =
      a(safePath.path.mkString, onClick --> { _ ⇒ panels.treeNodePanel.treeNodeManager.switch(safePath.parent)})

    def buildTask(directory: SafePath, tmpDirectory: SafePath, mmd: Option[ParsedModelMetadata])(using panels: Panels): Future[Unit] =
      mmd match
        case Some(md) =>
          val modifiedMMD =
            md.data.copy(
              inputs = inputTags.tags.now().map { t => inferProtoTyePair(t.ref.innerText, md) },
              outputs = outputTags.tags.now().map { t => inferProtoTyePair(t.ref.innerText, md) },
              command = commandInput.ref.value
            )

          for
            content <- md.factory.content(md.files, md.acceptedModel, modifiedMMD)
            listed <- api.listFiles(tmpDirectory, withHidden = true)
            destination = content.directory match
              case None => directory
              case Some(d) => directory / d
            _ <- api.move(listed.data.map(f => (tmpDirectory / f.name) -> (destination / f.name)))
            modelName = content.name.getOrElse("Model.oms")
            _ <- api.saveFile(directory ++ modelName, content.content, overwrite = true)
          yield
            panels.treeNodePanel.refresh
            panels.treeNodePanel.displayNode(directory / modelName, refresh = true)
        case None => Future.successful(())


    def buildButton(tmpDirectory: SafePath) =
      button("Build", width := "150px", margin := "0 25 10 25", OMTags.btn_purple,
        onClick --> {
          _ ⇒
            buildTask(currentDirectory.now(), tmpDirectory, modelMetadata.now()).andThen {
              case util.Failure(exception) => NotificationManager.toService(panels.notifications).notifyError(s"Error while generating code", exception)
              case util.Success(_) =>
            }
            modelMetadata.set(None)
            inputTags.clear
            outputTags.clear
            panels.closeExpandable
        }
      )

    val IOObserver = Observer[Seq[Span]](tb =>
      if (tb.isEmpty) exclusiveMenu.entrySet.update(_.filterNot(_ == 1))
      else exclusiveMenu.entrySet.update(es => (es :+ 1).distinct)
    )

    div(flexColumn, marginTop := "20",
      children <-- Signal.fromFuture(api.temporaryDirectory()).map:
        case Some(tmpDirectory) =>
          def upload =
            div(flexRow, width := "100%",
              upButton(tmpDirectory),
              span(display.flex, alignItems.center, color.black, marginLeft := "10px",
                child <-- (modelMetadata.signal combineWith currentDirectory).map {
                  case (Some(md), _) => span(s"""Use wizard "${md.factory.name}" for: ${md.files.map(_._1.mkString).mkString(", ")}""")
                  case (_, d) => span(s"Task will be built in ⌂/${d.path.mkString}")
                })
            )

          def io =
            div (
              child <-- modelMetadata.signal.map {
                case Some(mmd) =>
                  if mmd.data.inputs.nonEmpty || mmd.data.outputs.nonEmpty
                  then
                    exclusiveMenu.entry("Inputs / Ouputs", 1,
                      div(height := "200px", ioTagBuilder(mmd.data.inputs.flatMap(_.name), mmd.data.outputs.flatMap(_.name)))
                    )
                  else div()
                case None => div()
              }
            )

          def cmd =
            div(
              child <-- modelMetadata.signal.map {
                case Some(_) => exclusiveMenu.entry("Command", 2, div(display.flex, commandInput, height := "120px", margin := "10 40"))
                case None => div()
              }
            )

          def build =
            div(
              child <-- modelMetadata.signal.map {
                case Some(mmd) =>
                  div(flexRow, width := "100%", marginTop := "40",
                    buildButton(tmpDirectory),
                    span(display.flex, alignItems.center, color.black, marginLeft := "10px", marginBottom := "10px",
                      child <-- currentDirectory.signal.map { d => span(s"${mmd.factory.name} task will be built in ⌂/${d.path.mkString}") }
                    ),
                  )
                case None => div()
              }
            )

          Seq(upload, io, cmd, build)
        case _ => Seq()
      ,
      uploadDirectorySwitch.element.amend(onClick --> { t => uploadDirectory.set(uploadDirectorySwitch.isChecked) }),
      div(onMountCallback(_ => currentDirectory.set(panels.directory.now()))),
      //            div(
      //              onUnmountCallback { _ => api.deleteFiles(Seq(tmpDirectory)) }
      //            )
      inputTags.tags --> IOObserver
    )

