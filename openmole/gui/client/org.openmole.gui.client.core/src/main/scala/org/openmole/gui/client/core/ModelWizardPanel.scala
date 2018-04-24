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

import org.openmole.gui.client.core.alert.AlertPanel
import org.openmole.gui.client.core.files._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.data.FileType._
import org.openmole.gui.client.core.panels._
import autowire._
import org.scalajs.dom.html.TextArea

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
import org.scalajs.dom.raw.{ HTMLDivElement, HTMLInputElement }
import org.openmole.gui.ext.tool.client._
import rx._
import scalatags.JsDom.{ TypedTag, tags }
import scalatags.JsDom.all._
import Waiter._
import org.openmole.gui.ext.data.DataUtils._
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import org.openmole.gui.client.tool.{ OMTags, OptionsDiv }
import org.openmole.gui.ext.api.Api
import scaladget.bootstrapnative.Selector.Options
import org.openmole.gui.ext.tool.client.FileManager

import scala.concurrent.Future

class ModelWizardPanel {
  implicit val ctx: Ctx.Owner = Ctx.Owner.safe()

  println("Wizards " + Plugins.wizardFactories.now.map {
    _.name
  })

  sealed trait VariableRole[T] {
    def content: T

    def clone(t: T): VariableRole[T]

    def switch: VariableRole[T]
  }

  case class Input[T](content: T) extends VariableRole[T] {
    def clone(otherT: T) = Input(otherT)

    def switch = Output(content)
  }

  case class Output[T](content: T) extends VariableRole[T] {
    def clone(otherT: T): VariableRole[T] = Output(otherT)

    def switch = Input(content)
  }

  case class CommandInput[T](content: T) extends VariableRole[T] {
    def clone(otherT: T) = CommandInput(otherT)

    def switch = CommandOutput(content)
  }

  case class CommandOutput[T](content: T) extends VariableRole[T] {
    def clone(otherT: T): VariableRole[T] = CommandOutput(otherT)

    def switch = CommandInput(content)
  }

  implicit def pairToLine(variableElement: VariableRole[VariableElement]): Reactive = buildReactive(variableElement)

  implicit def stringToOptionString(s: String): Option[String] = if (s.isEmpty) None else Some(s)

  val filePath: Var[Option[SafePath]] = Var(None)
  val transferring: Var[ProcessState] = Var(Processed())
  val labelName: Var[Option[String]] = Var(None)
  val launchingCommand: Var[Option[LaunchingCommand]] = Var(None)
  val currentReactives: Var[Seq[Reactive]] = Var(Seq())
  val updatableTable: Var[Boolean] = Var(true)
  val bodyContent: Var[Option[TypedTag[HTMLDivElement]]] = Var(None)
  val resources: Var[Resources] = Var(Resources.empty)
  val currentTab: Var[Int] = Var(0)
  val autoMode = Var(true)
  //val upButton: HTMLDivElement = Var(tags.div().render)
  val fileToUploadPath: Var[Option[SafePath]] = Var(None)
  val targetPath: Var[Option[SafePath]] = Var(None)

  fileToUploadPath.trigger {
    fileToUploadPath.now.map {
      buildForm
    }
  }

  val modelSelector: Options[SafePath] = Seq[SafePath]().options(
    0,
    btn_default,
    SafePath.naming,
    onclose = () ⇒ {
      fileToUploadPath() = modelSelector.get
    }
  )

  def setScritpName = scriptNameInput.value = filePath.now.map {
    _.name.split('.').head
  }.getOrElse("model")

  val commandArea: TextArea = textArea(3).render
  val autoModeCheckBox = checkbox(autoMode.now)(onchange := {
    () ⇒
      autoMode() = !autoMode.now
  })

  val scriptNameInput = inputTag()(modelNameInput, placeholder := "Script name").render
  val factories = Plugins.wizardFactories.now

  launchingCommand.triggerLater {
    if (autoMode.now) {
      commandArea.value = launchingCommand.now.map {
        _.fullCommand
      }.getOrElse("")
    }
  }

  currentReactives.triggerLater {
    if (updatableTable.now) setBodyContent
  }

  def buttonStyle(i: Int): ModifierSeq = {
    if (i == currentTab.now) btn_primary
    else btn_default
  } +++ (marginRight := 20)

  def nbInputs = inputs(currentReactives.now).size

  def nbOutputs = currentReactives.now.size - nbInputs

  def inputs(reactives: Seq[Reactive]): Seq[VariableRole[VariableElement]] = {
    reactives.map {
      _.role
    }.collect {
      case x: Input[VariableElement]        ⇒ x
      case x: CommandInput[VariableElement] ⇒ x
    }
  }

  def outputs(reactives: Seq[Reactive]): Seq[VariableRole[VariableElement]] =
    reactives.map {
      _.role
    }.collect {
      case x: Output[VariableElement]        ⇒ x
      case x: CommandOutput[VariableElement] ⇒ x
    }

  def getReactive(index: Int): Option[Reactive] = currentReactives.now.filter {
    _.index == index
  }.headOption

  val upButton =
    div(ms("modelWizardDivs"))(
      div(maxWidth := 250)(
        label(
          Seq(display := "table") +++ certificate +++ "inputFileStyle",
          transferring.withTransferWaiter {
            _ ⇒
              div(
                fileInput((fInput: HTMLInputElement) ⇒ {
                  if (fInput.files.length > 0) {
                    fileToUploadPath() = None
                    resources() = Resources.empty
                    val fileName = fInput.files.item(0).name
                    labelName() = Some(fileName)
                    filePath() = Some(manager.current.now ++ fileName)
                    filePath.now.map {
                      fp ⇒
                        moveFilesAndBuildForm(fInput, fileName, fp)
                    }
                  }
                }), labelName.now match {
                  case Some(s: String) ⇒ s
                  case _               ⇒ "Your Model"
                }
              )
          }
        ),
        Rx {
          span(grey)(
            filePath() match {
              case Some(sp: SafePath) ⇒
                val fileType: FileType = sp
                if (fileType == Archive) modelSelector.selector else div.render
              case _ ⇒ div.render
            },
            div(
              labelName.now.flatMap {
                factory
              } match {
                case f: WizardPluginFactory ⇒ f.help
                case _                      ⇒ ""
              }
            )
          )
        }
      )
    ).render

  def setTargetPath(fileType: FileType, safePath: SafePath) =
    targetPath() = Some(safePath)

  //      Some(fileType match {
  //      case codeFile: CodeFile ⇒ safePath
  //      case _                  ⇒ safePath
  //    }
  //)

  def moveFilesAndBuildForm(fInput: HTMLInputElement, fileName: String, uploadPath: SafePath) =
    CoreUtils.withTmpFile {
      tempFile ⇒
        FileManager.upload(
          fInput,
          tempFile,
          (p: ProcessState) ⇒ {
            transferring() = p
          },
          UploadAbsolute(),
          () ⇒ {
            post()[Api].extractAndTestExistence(tempFile ++ fileName, uploadPath.parent).call().foreach {
              existing ⇒
                val fileType: FileType = uploadPath
                setTargetPath(fileType, uploadPath)

                // Move files from tmp to target path
                if (existing.isEmpty) {
                  targetPath.now.map {
                    tp ⇒
                      post()[Api].copyAllTmpTo(tempFile, tp).call().foreach {
                        b ⇒
                          fileToUploadPath() = Some(uploadPath)
                          //buildForm(uploadPath)
                          post()[Api].deleteFile(tempFile, ServerFileSystemContext.absolute).call()
                      }
                  }
                }
                else {
                  val optionsDiv = OptionsDiv(existing, SafePath.naming)
                  AlertPanel.div(
                    tags.div(
                      "Some files already exist, overwrite ?",
                      optionsDiv.div
                    ),
                    () ⇒ {
                      post()[Api].copyFromTmp(tempFile, optionsDiv.result /*, fp ++ fileName*/ ).call().foreach {
                        b ⇒
                          //buildForm(uploadPath)
                          fileToUploadPath() = Some(uploadPath)
                          post()[Api].deleteFile(tempFile, ServerFileSystemContext.absolute).call()
                      }
                    }, () ⇒ {
                    }, buttonGroupClass = "right"
                  )
                }
            }
          }
        )
    }

  def fromSafePath(safePath: SafePath) = {
    labelName() = Some(safePath.name)
    fileToUploadPath() = Some(safePath)
    setTargetPath(safePath, safePath)
    // buildForm(safePath)
  }

  def factory(safePath: SafePath): Option[WizardPluginFactory] = factory(safePath.name)

  def factory(fileName: String): Option[WizardPluginFactory] = {
    val fileType: FileType = fileName

    factories.filter {
      _.fileType == fileType
    }.headOption
  }

  def buildForm(safePath: SafePath) = {
    val pathFileType: FileType = safePath
    pathFileType match {
      case Archive ⇒
        post()[Api].models(safePath).call().foreach {
          models ⇒
            modelSelector.setContents(models, () ⇒ {
              TreeNodePanel.refreshAnd(() ⇒
                fileToUploadPath() = modelSelector.get
              )
            })
        }
      case _ ⇒
        factory(safePath).foreach { factory ⇒
          factory.parse(safePath).foreach {
            b ⇒
              TreeNodePanel.refreshAndDraw
              launchingCommand() = b
              fileToUploadPath() = Some(safePath)
              launchingCommand.now.foreach {
                lc ⇒
                  //            lc.language.map { l ⇒
                  //              factorySelector.contents.now.filter {
                  //                _.language == l
                  //              }.headOption.map {
                  //                factorySelector.set
                  //              }
                  //            }
                  setScritpName
                  setReactives(lc)
              }
          }
        }
    }

    //    fileType match {
    //      case archive: Archive ⇒
    //        fromArchive() = true
    //        archive.language match {
    //          //Java case
    //          case JavaLikeLanguage() ⇒
    //            modelSelector.emptyContents
    //            fileToUploadPath() = Some(uploadPath)
    //          // launchingCommand() = Some("Your Java/Scala method call here")
    //          // Other archive: tgz, tar.gz
    //          // case RLanguage() ⇒xxx
    //          case UndefinedLanguage() ⇒
    ////            post()[Api].models(uploadPath).call().foreach {
    ////              models ⇒
    ////                fileToUploadPath() = models.headOption
    ////                modelSelector.setContents(models, () ⇒ {
    ////                  TreeNodePanel.refreshAnd(() ⇒ onModelChange)
    ////                })
    //           //     getResourceInfo
    //            }
    //
    //          case _ ⇒
    //            fromArchive() = false
    //
    //
    //      case codeFile: CodeFile ⇒
    //        modelSelector.emptyContents
    //        resources() = resources.now.withNoImplicit
    //        setLaunchingCommand(uploadPath)
    //      case _ ⇒
    //    }
  }

  def getResourceInfo = {
    fileToUploadPath.now.foreach {
      mp ⇒
        val modelName = mp.name
        val resourceDir = mp.parent
        post()[Api].listFiles(resourceDir).call().foreach {
          b ⇒
            val l = b.list.filterNot {
              _.name == modelName
            }.map {
              tn ⇒
                val sp = resourceDir ++ tn.name
                Resource(sp, 0L)
            }
            resources() = resources.now.copy(implicits = l, number = l.size)
            post()[Api].expandResources(resources.now).call().foreach {
              lf ⇒
                resources() = lf
            }
        }
    }
  }

  //  def setLaunchingCommand(filePath: SafePath) =
  //    currentFactory.now.foreach { factory ⇒
  //      factory.parse(filePath).foreach { b ⇒
  //        TreeNodePanel.refreshAndDraw
  //        launchingCommand() = b
  //        fileToUploadPath() = Some(filePath)
  //        launchingCommand.now.foreach {
  //          lc ⇒
  //            currentFactory() = Some(factory)
  //            //            lc.language.map { l ⇒
  //            //              factorySelector.contents.now.filter {
  //            //                _.language == l
  //            //              }.headOption.map {
  //            //                factorySelector.set
  //            //              }
  //            //            }
  //            setScritpName
  //            setReactives(lc)
  //        }
  //      }
  //    }

  def setReactives(lc: LaunchingCommand) = {
    val nbArgs = lc.arguments.size
    val iReactives = lc.arguments.zipWithIndex.collect {
      case (ve: VariableElement, id: Int) ⇒ buildReactive(CommandInput(ve), id)
    }
    val oReactives = lc.outputs.zipWithIndex collect {
      case (ve: VariableElement, id: Int) ⇒ buildReactive(CommandOutput(ve), id + nbArgs)
    }
    currentReactives() = iReactives ++ oReactives
  }

  val step1 = tags.div(
    tags.h4("Step 1: Code import"),
    div(grey +++ rightBlock)(
      "Pick your code up among jar archive, netlogo scripts, or any code packaged on linux with Care ( like Python, C, C++ " +
        "R, etc). In the case of a Care archive, the packaging has to be done with the",
      tags.b(" -o yourmodel.tar.gz.bin"), " or ", tags.b(" -o yourmodel.tgz.bin"), " option."
    )
  )

  val step2 = div(
    div(grey)("The systems detects automatically the launching command and propose you the creation of some OpenMOLE Variables so that" +
      " your model will be able to be feeded with variable values coming from the workflow you will build afterwards. In the case of Java, Scala, Netlogo" +
      "(ie codes working on the JVM) the OpenMOLE variables can be set directly in the command line. Otherwise, they have to be set inside ${} statements." +
      " By default he systems detects automatically your Variable changes and update the launching command. However, this option can be desactivated.")
  )

  val autoModeTag = div(onecolumn +++ (paddingTop := 20))(
    tags.b("Launching Command"),
    div(floatRight +++ (paddingTop := 4))(
      "Automatic ",
      autoModeCheckBox,
      span(grey)(" It is automatically updated (default), or it can be set manually")
    )
  )

  val buildScriptButton = {

    tags.button("Build", btn_primary, onclick := {
      () ⇒
        save
        dialog.hide

        //        val codeType = factorySelector.getOrElse(factories.head)
        fileToUploadPath.now.foreach {
          fp ⇒
            factory(fp).map {
              factory ⇒
                CoreUtils.buildModelScript(
                  factory,
                  labelName.now.getOrElse("script"),
                  commandArea.value,
                  manager.current.now ++ s"${
                    scriptNameInput.value.clean
                  }.oms",
                  resources.now,
                  inputs(currentReactives.now).map {
                    _.content.prototype
                  },
                  outputs(currentReactives.now).map {
                    _.content.prototype
                  },
                  fileToUploadPath.now.map {
                    _.name
                  })

            }
        }

      //        val targetSuffix = codeType match {
      //          case NetLogoLanguage() ⇒
      //            if (fromArchive.now) s"/${
      //              fileToUploadPath.now.map {
      //                _.name
      //              }.getOrElse("NetLogoMODEL")
      //            }"
      //            else ""
      //          case _ ⇒ ""
      //        }

      //        CoreUtils.buildModelScript(
      //          codeType,
      //          commandArea.value,
      //          scriptNameInput.value.clean,
      //          manager.current.now,
      //          resources.now,
      //          targetPath.now.map {
      //            _.name
      //          }.getOrElse("executable") + targetSuffix,
      //          inputs(currentReactives.now).map {
      //            _.content.prototype
      //          },
      //          outputs(currentReactives.now).map {
      //            _.content.prototype
      //          },
      //          fileToUploadPath.now.map {
      //            _.name
      //          })
    })
  }

  def save = {
    currentReactives.now.map {
      _.save
    }
  }

  def buildReactive(role: VariableRole[VariableElement], index: Int): Reactive = Reactive(role, index)

  def buildReactive(role: VariableRole[VariableElement]): Reactive =
    currentReactives.now.filter {
      _.role == role
    }.headOption.getOrElse(buildReactive(role, role.content.index))

  def addVariableElement(p: VariableRole[VariableElement]) = {
    save
    currentReactives() = currentReactives.now :+ buildReactive(p, -1)
  }

  def applyOnPrototypePair(p: VariableRole[VariableElement], todo: (VariableRole[VariableElement], Int) ⇒ Unit) =
    currentReactives.now.map {
      _.role
    }.zipWithIndex.filter {
      case (ptp, index) ⇒ ptp == p
    }.foreach {
      case (role, index) ⇒ todo(role, index)
    }

  def updatePrototypePair(p: VariableRole[VariableElement], variableElement: VariableElement) =
    applyOnPrototypePair(p, (role: VariableRole[VariableElement], index: Int) ⇒ currentReactives() = currentReactives.now.updated(index, buildReactive(role.clone(variableElement), index)))

  def switchPrototypePair(p: VariableRole[VariableElement]) = {
    save
    applyOnPrototypePair(p, (role: VariableRole[VariableElement], index: Int) ⇒ currentReactives() = currentReactives.now.updated(index, buildReactive(role.switch, index)))
  }

  def addSwitchedPrototypePair(p: VariableRole[VariableElement]) = {
    save
    currentReactives() = (currentReactives.now :+ buildReactive(p.switch, -1)) distinct
  }

  case class Reactive(role: VariableRole[VariableElement], index: Int) {
    val lineHovered: Var[Boolean] = Var(false)

    val switchGlyph = role match {
      case i: Input[_]         ⇒ glyph_arrow_right
      case ci: CommandInput[_] ⇒ glyph_arrow_right
      case _                   ⇒ glyph_arrow_left
    }

    def updateLaunchingCommand =
      role match {
        case CommandInput(_) | CommandOutput(_) ⇒
          launchingCommand() = launchingCommand.now.map { lc ⇒
            lc.updateVariables(currentReactives.now.map {
              _.role
            }.collect {
              case x: CommandInput[_]  ⇒ x
              case y: CommandOutput[_] ⇒ y
            }.map {
              _.content
            })
          }
        case _ ⇒
      }

    def save = getReactive(index).map { reactive ⇒ updatePrototypePair(reactive.role, reactive.role.content.clone(nameInput.value, role.content.prototype.`type`, mappingInput.value)) }

    def removePrototypePair = {
      currentReactives() = currentReactives.now.filterNot(_.role == role)
      ModelWizardPanel.this.save
    }

    def saveWithoutTableUpdate = {
      updatableTable() = false
      save
      updatableTable() = true
    }

    val mappingInput: HTMLInputElement = inputTag(role.content.prototype.mapping.getOrElse(""))(oninput := { () ⇒
      saveWithoutTableUpdate
      updateLaunchingCommand
    }).render

    val nameInput: HTMLInputElement = inputTag(role.content.prototype.name)(oninput := { () ⇒
      saveWithoutTableUpdate
      updateLaunchingCommand
    }).render

    val line = {
      val glyphModifier = grey +++ (paddingTop := 2) +++ pointer +++ (opacity := 0.5)
      tags.tr(
        td(colMD(3) +++ (paddingTop := 7))(nameInput),
        td(colMD(2))(label(role.content.prototype.`type`.name.split('.').last)(label_primary)),
        td(colMD(1) +++ grey)(role.content.prototype.default),
        td(colMD(3))(if (role.content.prototype.mapping.isDefined) mappingInput else tags.div()),
        td(colMD(2) +++ floatRight)(
          tags.span(onclick := { () ⇒ switchPrototypePair(role) })(glyphModifier +++ switchGlyph),
          tags.span(onclick := { () ⇒ addSwitchedPrototypePair(role) })(glyphModifier +++ glyph_arrow_right_and_left),
          tags.span(pointer, onclick := { () ⇒ removePrototypePair })(glyphModifier +++ glyph_trash)
        )
      )
    }
  }

  def setBodyContent: Unit = bodyContent() = Some({
    val reactives = currentReactives.now
    val topButtons = Rx {
      div(paddingTop := 20)(
        button(
          "I/O",
          buttonStyle(0), onclick := {
            () ⇒
              {
                currentTab() = 0
                setBodyContent
              }
          })(badge(s"$nbInputs/$nbOutputs")),
        button("Resources", buttonStyle(1), onclick := {
          () ⇒
            {
              currentTab() = 1
              setBodyContent
            }
        }
        )(badge(s"${
          resources().number
        }"))
      )
    }

    tags.div(
      fileToUploadPath.now.map {
        _ ⇒ tags.div()
      }.getOrElse(step1),
      transferring.now match {
        case _: Processing ⇒ OMTags.waitingSpan(" Uploading ...", btn_danger + "certificate")
        case _: Processed  ⇒ upButton
        case _             ⇒ upButton
      },
      fileToUploadPath.now.map {
        _ ⇒
          div(paddingTop := 20)(
            tags.h4("Step2: Task configuration"), step2,
            topButtons,
            if (currentTab.now == 0) {
              tags.div({

                val idiv = div(modelIO)(tags.h3("Inputs")).render

                val odiv = div(modelIO)(tags.h3("Outputs")).render

                val head = thead(tags.tr(
                  for (h ← Seq("Name", "Type", "Default", "Mapped with", "", "")) yield {
                    th(textAlign := "center", h)
                  }
                ))

                div(paddingTop := 30)(
                  div(twocolumns +++ (paddingRight := 10))(
                    idiv,
                    tags.table(striped)(
                      head,
                      tbody(
                        for (ip ← inputs(reactives)) yield {
                          ip.line
                        }
                      )
                    )
                  ),
                  div(twocolumns)(
                    odiv,
                    tags.table(striped)(
                      head,
                      tbody(
                        for (op ← outputs(reactives)) yield {
                          op.line
                        }
                      )
                    )
                  )
                )
              }, autoModeTag, commandArea)
            }
            else {
              val body = tbody.render
              for {
                i ← resources.now.implicits
              } yield {
                body.appendChild(tags.tr(
                  td(colMD(3))(i.safePath.name),
                  td(colMD(2))(CoreUtils.readableByteCountAsString(i.size))
                ).render)
              }
              tags.table(striped +++ (paddingTop := 20))(body)
            }
          )
      }.getOrElse(tags.div())
    )
  })

  lazy val dialog = {
    setBodyContent
    ModalDialog(omsheet.panelWidth(92))
  }

  dialog.header(
    tags.span(tags.b("Model import"))
  )

  dialog.body(
    tags.div(
      Rx {
        bodyContent().getOrElse(tags.div())
      }
    )
  )

  dialog.footer(buttonGroup(Seq(width := 200, right := 100))(
    inputGroupButton(ModalDialog.closeButton(dialog, btn_default, "Close")),
    inputGroupButton(scriptNameInput),
    inputGroupButton(buildScriptButton)
  ))

}