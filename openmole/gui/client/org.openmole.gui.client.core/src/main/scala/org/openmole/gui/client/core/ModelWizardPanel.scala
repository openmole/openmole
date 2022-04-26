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

import org.openmole.gui.client.core.alert.{AlertPanel, BannerAlert}
import org.openmole.gui.client.core.files._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data.FileType._
import org.openmole.gui.client.core.panels._
import autowire._
import org.scalajs.dom.html.TextArea

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import org.scalajs.dom.raw.{HTMLDivElement, HTMLElement, HTMLInputElement}
import org.openmole.gui.ext.client._
import com.raquo.laminar.api.L._
import Waiter._
import org.openmole.gui.ext.data.DataUtils._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.client.tool.{OMTags, OptionsDiv}
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.client.FileManager
import scaladget.bootstrapnative.Selector.Options

import scala.concurrent.Future

class ModelWizardPanel(treeNodeManager: TreeNodeManager, treeNodeTabs: TreeNodeTabs, bannerAlert: BannerAlert, wizards: Seq[WizardPluginFactory]) {

  sealed trait VariableRole[T] {
    def content: T

    def clone(t: T): VariableRole[T]

    def switch: VariableRole[T]
  }
//
//  case class ModelInput[T](content: T) extends VariableRole[T] {
//    def clone(otherT: T) = ModelInput(otherT)
//
//    def switch = ModelOutput(content)
//  }
//
//  case class ModelOutput[T](content: T) extends VariableRole[T] {
//    def clone(otherT: T): VariableRole[T] = ModelOutput(otherT)
//
//    def switch = ModelInput(content)
//  }
//
//  case class CommandInput[T](content: T) extends VariableRole[T] {
//    def clone(otherT: T) = CommandInput(otherT)
//
//    def switch = CommandOutput(content)
//  }
//
//  case class CommandOutput[T](content: T) extends VariableRole[T] {
//    def clone(otherT: T): VariableRole[T] = CommandOutput(otherT)
//
//    def switch = CommandInput(content)
//  }
//
//  implicit def pairToLine(variableElement: VariableRole[VariableElement]): Reactive = buildReactive(variableElement)

  implicit def stringToOptionString(s: String): Option[String] = if (s.isEmpty) None else Some(s)

  val filePath: Var[Option[SafePath]] = Var(None)
  val transferring: Var[ProcessState] = Var(Processed())
  val labelName: Var[Option[String]] = Var(None)
  val launchingCommand: Var[Option[LaunchingCommand]] = Var(None)
//  val currentReactives: Var[Seq[Reactive]] = Var(Seq())
  val updatableTable: Var[Boolean] = Var(true)
  val resources: Var[Resources] = Var(Resources.empty)
  val currentTab: Var[Int] = Var(0)
  val autoMode = Var(true)
  val fileToUploadPath: Var[Option[SafePath]] = Var(None)
  val currentPluginPanel: Var[Option[WizardGUIPlugin]] = Var(None)
//
//  fileToUploadPath.trigger {
//    fileToUploadPath.now.foreach {
//      buildForm
//    }
//  }
//
//  val modelSelector: Options[SafePath] = Seq[SafePath]().options(
//    0,
//    btn_secondary,
//    SafePath.naming,
//    onclose = () ⇒ {
//      fileToUploadPath.set(modelSelector.get)
//    }
//  )
//
//  //  val commandArea: TextArea = textArea(3)
//  //  val autoModeCheckBox = checkbox(autoMode.now)(onchange := {
//  //    () ⇒
//  //      autoMode() = !autoMode.now
//  //  })
//  //
//  val scriptNameInput = inputTag().amend(modelNameInput, placeholder := "Script name")
//  //
//  //  launchingCommand.triggerLater {
//  //    if (autoMode.now) {
//  //      commandArea.value = launchingCommand.now.map {
//  //        _.fullCommand
//  //      }.getOrElse("")
//  //    }
//  //  }
//
//  def buttonStyle(i: Int): HESetters =
//    Seq(
//      if (i == currentTab.now) btn_primary
//      else btn_secondary,
//      (marginRight := "20"),
//    )
//
//  def nbInputs = inputs(currentReactives.now).size
//
//  def nbOutputs = currentReactives.now.size - nbInputs
//
//  def inputs(reactives: Seq[Reactive]): Seq[VariableRole[VariableElement]] = {
//    reactives.map {
//      _.role
//    }.collect {
//      case x: ModelInput[VariableElement] ⇒ x
//      case x: CommandInput[VariableElement] ⇒ x
//    }
//  }
//
//  def outputs(reactives: Seq[Reactive]): Seq[VariableRole[VariableElement]] =
//    reactives.map {
//      _.role
//    }.collect {
//      case x: ModelOutput[VariableElement] ⇒ x
//      case x: CommandOutput[VariableElement] ⇒ x
//    }
//
//  def getReactive(index: Int): Option[Reactive] = currentReactives.now.filter {
//    _.index == index
//  }.headOption
//
//  lazy val upButton =
//    div(Seq(display := "table", width := 200))(
//      label(
//        cls := "inputFileStyle",
//        transferring.withTransferWaiter {
//          _ ⇒
//            div(
//              fileInput((fInput: HTMLInputElement) ⇒ {
//                if (fInput.files.length > 0) {
//                  fileToUploadPath.set(None)
//                  resources.set(Resources.empty)
//                  val fileName = fInput.files.item(0).name
//                  labelName.set(Some(fileName))
//                  filePath.set(Some(treeNodeManager.current.now ++ fileName))
//                  filePath.now.map {
//                    fp ⇒
//                      moveFilesAndBuildForm(fInput, fileName, fp)
//                  }
//                }
//              }),
//              child <-- labelName.signal.map {
//                _ match {
//                  case Some(s: String) ⇒ s
//                  case _ ⇒ "Your Model"
//                }
//              }
//            )
//        }
//      ),
//      span(grey,
//        child <-- filePath.signal.map {
//          _ match {
//            case Some(sp: SafePath) ⇒
//              val fileType: FileType = sp
//              if (fileType == Archive) modelSelector.selector else div()
//            case _ ⇒ div()
//          }
//        }
//      )
//    )
//
//  lazy val topConfiguration =
//    div(
//      upButton,
//      child <-- labelName.signal.combineWith(currentPluginPanel.signal).map { case (ln, cpp) =>
//        ln.flatMap(factory) match {
//          case Some(f: WizardPluginFactory) =>
//            div(marginTop := "20",
//              cpp.map {
//                _.panel
//              }.getOrElse(div())
//            )
//        }
//      }
//    )
//
//  def moveFilesAndBuildForm(fInput: ModelInput, fileName: String, uploadPath: SafePath) =
//    CoreUtils.withTmpFile {
//      tempFile ⇒
//        FileManager.upload(
//          fInput,
//          tempFile,
//          (p: ProcessState) ⇒ {
//            transferring() = p
//          },
//          UploadAbsolute(),
//          () ⇒ {
//            Post()[Api].extractAndTestExistence(tempFile ++ fileName, uploadPath.parent).call().foreach {
//              existing ⇒
//                val fileType: FileType = uploadPath
//                val targetPath = fileType match {
//                  case Archive ⇒ uploadPath.parent ++ uploadPath.nameWithNoExtension
//                  case _ ⇒ uploadPath
//                }
//
//                // Move files from tmp to target path
//                if (existing.isEmpty) {
//                  Post()[Api].copyAllTmpTo(tempFile, targetPath).call().foreach {
//                    b ⇒
//                      fileToUploadPath() = Some(uploadPath)
//                      Post()[Api].deleteFile(tempFile, ServerFileSystemContext.absolute).call()
//                  }
//                }
//                else {
//                  val optionsDiv = OptionsDiv(existing, SafePath.naming)
//                  panels.alertPanel.alertDiv(
//                    div(
//                      "Some files already exist, overwrite?",
//                      optionsDiv.render
//                    ),
//                    () ⇒ {
//                      Post()[Api].copyFromTmp(tempFile, optionsDiv.result /*, fp ++ fileName*/).call().foreach {
//                        b ⇒
//                          //buildForm(uploadPath)
//                          fileToUploadPath() = Some(uploadPath)
//                          Post()[Api].deleteFile(tempFile, ServerFileSystemContext.absolute).call()
//                      }
//                    }, () ⇒ {
//                    }, buttonGroupClass = "right"
//                  )
//                }
//            }
//          }
//        )
//    }
//
  def fromSafePath(safePath: SafePath) = {
    filePath.set(Some(safePath))
    fileToUploadPath.set(Some(safePath))
    labelName.set(Some(safePath.name))
  }
//
//  def factory(safePath: SafePath): Option[WizardPluginFactory] = factory(safePath.name)
//
//  def factory(fileName: String): Option[WizardPluginFactory] = {
//    val fileType: FileType = fileName
//
//    wizards.filter {
//      _.fileType == fileType
//    }.headOption
//  }
//
//  def buildForm(safePath: SafePath) = {
//    val pathFileType: FileType = safePath
//    pathFileType match {
//      case Archive ⇒
//        currentPluginPanel.set(None)
//        Post()[Api].models(safePath).call().foreach {
//          models ⇒
//            modelSelector.setContents(models, () ⇒ {
//              panels.treeNodePanel.refreshAnd(() ⇒
//                fileToUploadPath.set(modelSelector.get)
//              )
//            })
//        }
//      case _ ⇒
//        factory(safePath).foreach { factory ⇒
//          scriptNameInput.ref.value = safePath.nameWithNoExtension
//          fileToUploadPath.set(Some(safePath))
//          treeNodeManager.invalidCurrentCache
//          factory.parse(safePath).foreach { b ⇒
//            currentPluginPanel.set(Some(factory.build(safePath, (lc: LaunchingCommand) ⇒ {
//              setLaunchingComand(Some(lc), safePath)
//            })))
//            setLaunchingComand(b, safePath)
//          }
//        }
//    }
//
//  }
//
//  def setLaunchingComand(lc: Option[LaunchingCommand], safePath: SafePath) = {
//    launchingCommand.set(lc)
//    launchingCommand.now match {
//      case Some(lc: LaunchingCommand) ⇒ setReactives(lc)
//      case _ ⇒
//    }
//  }
//
//  def getResourceInfo = {
//    fileToUploadPath.now.foreach {
//      mp ⇒
//        val modelName = mp.name
//        val resourceDir = mp.parent
//        Post()[Api].listFiles(resourceDir).call().foreach {
//          b ⇒
//            val l = b.list.filterNot {
//              _.name == modelName
//            }.map {
//              tn ⇒
//                val sp = resourceDir ++ tn.name
//                Resource(sp, 0L)
//            }
//            resources.set(resources.now.copy(implicits = l, number = l.size))
//            Post()[Api].expandResources(resources.now).call().foreach {
//              lf ⇒
//                resources.set(lf)
//            }
//        }
//    }
//  }
//
//  def setReactives(lc: LaunchingCommand) = {
//    val nbArgs = lc.arguments.size
//    val iReactives = lc.arguments.zipWithIndex.collect {
//      case (ve: VariableElement, id: Int) ⇒ buildReactive(CommandInput(ve), id)
//    }
//    val oReactives = lc.outputs.zipWithIndex collect {
//      case (ve: VariableElement, id: Int) ⇒ buildReactive(CommandOutput(ve), id + nbArgs)
//    }
//    currentReactives.set(iReactives ++ oReactives)
//  }
//
//  val step1 = div(
//    h4("Step 1: Code import"),
//    div(grey, rightBlock,
//      "Browse for your source file. Your file can be a Java archive, a NetLogo script, or in any language you want (Python, C, C++," +
//        " R, etc)."
//    )
//  )
//
//  val step2 = div(grey,
//    "The system automatically detects the launching commands from your script, and offers to create the corresponding OpenMOLE variables." +
//      " This will allow you to feed values to these variables directly from the workflow you will build." +
//      " In the case of Java, Scala or NetLogo (i.e. programs working on the JVM), the OpenMOLE variables can be set directly in the command line." +
//      " Otherwise, they have to be set inside ${} statements." +
//      " By default, the system automatically detects variable changes, and updates the launching commands accordingly. However, this option can be deactivated."
//  )
//
//  //  val autoModeTag = div(onecolumn, paddingTop := "20",
//  //    b("Launching commands"),
//  //    div(float.right, paddingTop := "4",
//  //      "Automatic ",
//  //      autoModeCheckBox,
//  //      span(grey, " Check to automatically update the launching commands.")
//  //    )
//  //  )
//
//  lazy val buildScriptButton = {
//    button("Build", btn_primary, onClick --> {
//      _ ⇒
//        save
//        dialog.hide
//        fileToUploadPath.now.foreach(buildScript)
//    })
//  }
//
//  def buildScript(safePath: SafePath) = factory(safePath).foreach {
//    factory ⇒
//      currentPluginPanel.now.foreach {
//        _.save(
//          treeNodeManager.current.now ++ s"${
//            scriptNameInput.ref.value.clean
//          }.oms",
//          labelName.now.getOrElse("script"),
//          // commandArea.value,
//          inputs(currentReactives.now).map {
//            _.content.prototype
//          },
//          outputs(currentReactives.now).map {
//            _.content.prototype
//          },
//          fileToUploadPath.now.map {
//            _.name
//          },
//          resources.now
//        ).foreach {
//          wtt ⇒
//            if (wtt.errors.isEmpty) {
//              treeNodeTabs remove wtt.safePath
//              treeNodePanel.displayNode(FileNode(Var(wtt.safePath.name), 0L, 0L))
//              treeNodeManager.invalidCurrentCache
//            }
//            else {
//              dialog.hide
//              bannerAlert.registerWithDetails("Plugin import failed", wtt.errors.map {
//                ErrorData.stackTrace
//              }.mkString("\n"))
//            }
//        }
//      }
//  }
//
//  def save = {
//    currentReactives.now.map {
//      _.save
//    }
//  }
//
//  def buildReactive(role: VariableRole[VariableElement], index: Int): Reactive = Reactive(role, index)
//
//  def buildReactive(role: VariableRole[VariableElement]): Reactive =
//    currentReactives.now.filter {
//      _.role == role
//    }.headOption.getOrElse(buildReactive(role, role.content.index))
//
//  def addVariableElement(p: VariableRole[VariableElement]) = {
//    save
//    currentReactives.update(cr=> cr :+ buildReactive(p, -1))
//  }
//
//  def applyOnPrototypePair(p: VariableRole[VariableElement], todo: (VariableRole[VariableElement], Int) ⇒ Unit) =
//    currentReactives.now.map {
//      _.role
//    }.zipWithIndex.filter {
//      case (ptp, index) ⇒ ptp == p
//    }.foreach {
//      case (role, index) ⇒ todo(role, index)
//    }
//
//  def updatePrototypePair(p: VariableRole[VariableElement], variableElement: VariableElement) =
//    applyOnPrototypePair(p, (role: VariableRole[VariableElement], index: Int) ⇒ currentReactives() = currentReactives.now.updated(index, buildReactive(role.clone(variableElement), index)))
//
//  def switchPrototypePair(p: VariableRole[VariableElement]) = {
//    save
//    applyOnPrototypePair(p, (role: VariableRole[VariableElement], index: Int) ⇒ currentReactives() = currentReactives.now.updated(index, buildReactive(role.switch, index)))
//  }
//
//  def addSwitchedPrototypePair(p: VariableRole[VariableElement]) = {
//    save
//    currentReactives() = (currentReactives.now :+ buildReactive(p.switch, -1)) distinct
//  }
//
//  case class Reactive(role: VariableRole[VariableElement], index: Int) {
//    val lineHovered: Var[Boolean] = Var(false)
//
//    val switchGlyph = role match {
//      case i: ModelInput[_] ⇒ glyph_arrow_right
//      case ci: CommandInput[_] ⇒ glyph_arrow_right
//      case _ ⇒ glyph_arrow_left
//    }
//
//    def updateLaunchingCommand =
//      role match {
//        case CommandInput(_) | CommandOutput(_) ⇒
//          launchingCommand.set(launchingCommand.now.map { lc ⇒
//            lc.updateVariables(currentReactives.now.map {
//              _.role
//            }.collect {
//              case x: CommandInput[_] ⇒ x
//              case y: CommandOutput[_] ⇒ y
//            }.map {
//              _.content
//            })
//          })
//        case _ ⇒
//      }
//
//    def save = getReactive(index).map { reactive ⇒ updatePrototypePair(reactive.role, reactive.role.content.clone(nameInput.value, role.content.prototype.`type`, mappingInput.value)) }
//
//    def removePrototypePair = {
//      currentReactives.update(cr => cr.filterNot(_.role == role))
//      ModelWizardPanel.this.save
//    }
//
//    def saveWithoutTableUpdate = {
//      updatableTable.set(false)
//      save
//      updatableTable.set(true)
//    }
//
//    val mappingInput = inputTag(role.content.prototype.mapping.getOrElse("")).amend(onInput --> { _ ⇒
//      saveWithoutTableUpdate
//      updateLaunchingCommand
//    })
//
//    val nameInput = inputTag(role.content.prototype.name).amend(onInput --> { _ ⇒
//      saveWithoutTableUpdate
//      updateLaunchingCommand
//    })
//
//    val line = {
//      val glyphModifier = Seq(color := grey, paddingTop := "2", cursor.pointer, opacity := 0.5)
//      tr(
//        td(colBS(3), paddingTop := "7", nameInput),
//        td(colBS(2), label(role.content.prototype.`type`.name.split('.').last, badge_primary)),
//        td(colBS(1), grey, role.content.prototype.default),
//        td(colBS(3), if (role.content.prototype.mapping.isDefined) mappingInput else div()),
//        td(colBS(2), float.right,
//          span(onClick --> { _ ⇒ switchPrototypePair(role) }, glyphModifier, switchGlyph),
//          span(onClick --> { _ ⇒ addSwitchedPrototypePair(role) }, glyphModifier, glyph_arrow_right_and_left),
//          span(cursor.pointer, onClick --> { _ ⇒ removePrototypePair }, glyphModifier, glyph_trash)
//        )
//      )
//    }
//  }
//
//  val topButtons =
//    div(paddingTop := "20",
//      button(
//        "I/O",
//        buttonStyle(0), onClick --> {
//          _ ⇒ {
//            currentTab.set(0)
//          }
//        }, badge(s"$nbInputs/$nbOutputs", badge_primary)),
//      child <-- resources.signal.map { r =>
//        button("Resources", buttonStyle(1), onClick --> {
//          () ⇒ {
//            currentTab.set(1)
//          }
//        }, badge(s"${r.number}", badge_primary)
//        )
//      }
//    )
//
//
//  val bodyContent = div(
//    step1,
//    transferring.now match {
//      case _: Processing ⇒ OMTags.waitingSpan(" Uploading...", btn_danger)
//      case _: Processed ⇒ topConfiguration
//      case _ ⇒ topConfiguration
//    },
//    //  launchingCommand() match {
//    //  case Some(lc: LaunchingCommand) ⇒
//    div(paddingTop := 20,
//      h4("Step 2: Task configuration"), step2,
//      topButtons,
//      child <-- currentTab.signal.combineWith(currentReactives.signal, fileToUploadPath.signal, resources.signal).map { case (tab, reactives, filePath, resources) =>
//        if (tab == 0) {
//
//          val idiv = div(modelIO, h3("Inputs"))
//          val odiv = div(modelIO, h3("Outputs"))
//
//          val head = thead(tr(
//            for (h ← Seq("Name", "Type", "Default", "Mapped with", "", "")) yield {
//              th(textAlign := "center", h)
//            }
//          ))
//          div(
//            div(paddingTop := "30",
//              div(twocolumns, paddingRight := "10"),
//              idiv,
//              table(striped,
//                head,
//                tbody(
//                  for (ip ← inputs(reactives)) yield {
//                    ip.line
//                  }
//                )
//              )
//            ),
//            div(twocolumns,
//              odiv,
//              table(striped,
//                head,
//                tbody(
//                  for (op ← outputs(reactives)) yield {
//                    op.line
//                  }
//                )
//              )
//            )
//          )
//        }
//        else {
//          val body = tbody()
//          for {
//            i ← resources.implicits
//          } yield {
//            render(body.ref,
//              tr(
//                td(colBS(3), i.safePath.name),
//                td(colBS(2), CoreUtils.readableByteCountAsString(i.size))
//              )
//            )
//          }
//          table(striped, paddingTop := "20", body)
//        }
//      }
//    )
//  )


  val dialogHeader = span(b("Model import"))

  val dialogFooter = buttonGroup.amend(width := "200", right := "100",
    closeButton("Close", () => dialog.hide),
  //  scriptNameInput,
  //  buildScriptButton
  )

  lazy val dialog: ModalDialog = ModalDialog(
    dialogHeader,
   div("body"),
    // bodyContent,
    dialogFooter,
    omsheet.panelWidth(92),
    () => {},
    () => {})

}
