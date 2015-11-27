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

import org.openmole.gui.client.core.files._
import org.openmole.gui.ext.data._
import org.openmole.gui.misc.js.{ Select, OMTags }
import autowire._
import org.scalajs.dom.html.TextArea
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow
import org.openmole.gui.client.core.files.treenodemanager.{ instance ⇒ manager }
import org.scalajs.dom.raw.{ HTMLDivElement, HTMLInputElement }
import org.openmole.gui.misc.js.JsRxTags._
import rx._
import org.openmole.gui.shared.Api
import scalatags.JsDom.{ TypedTag, tags ⇒ tags }

import scalatags.JsDom.all._
import fr.iscpif.scaladget.api.{ BootstrapTags ⇒ bs }
import bs._

class ModelWizardPanel extends ModalPanel {
  lazy val modalID = "modelWizardPanelID"

  def onOpen() = {}

  def onClose() = {}

  sealed trait Role[T] {
    def content: T

    def clone(t: T): Role[T]

    def switch: Role[T]
  }

  case class Input[T](content: T) extends Role[T] {
    def clone(otherT: T) = Input(otherT)

    def switch = Output(content)
  }

  case class Output[T](content: T) extends Role[T] {
    def clone(otherT: T): Role[T] = Output(otherT)

    def switch = Input(content)
  }

  case class CommandInput[T](content: T) extends Role[T] {
    def clone(otherT: T) = CommandInput(otherT)

    def switch = CommandOutput(content)
  }

  case class CommandOutput[T](content: T) extends Role[T] {
    def clone(otherT: T): Role[T] = CommandOutput(otherT)

    def switch = CommandInput(content)
  }

  implicit def pairToLine(variableElement: Role[VariableElement]): Reactive = buildReactive(variableElement)

  implicit def stringToOptionString(s: String): Option[String] = if (s.isEmpty) None else Some(s)

  val transferring: Var[FileTransferState] = Var(Standby())
  val labelName: Var[Option[String]] = Var(None)
  val launchingCommand: Var[Option[LaunchingCommand]] = Var(None)
  val currentReactives: Var[Seq[Reactive]] = Var(Seq())
  val updatableTable: Var[Boolean] = Var(true)
  val hasStep2: Var[Boolean] = Var(false)
  val bodyContent: Var[Option[TypedTag[HTMLDivElement]]] = Var(None)

  val commandArea: TextArea = bs.textArea(5)("").render

  Obs(launchingCommand, skipInitial = true) {
    commandArea.value = launchingCommand().map {
      _.fullCommand
    }.getOrElse("")
  }

  Obs(currentReactives, skipInitial = true) {
    if (updatableTable()) setBodyContent
  }

  def inputs(reactives: Seq[Reactive]): Seq[Role[VariableElement]] = {
    reactives.map {
      _.role
    }.collect {
      case x: Input[VariableElement]        ⇒ x
      case x: CommandInput[VariableElement] ⇒ x
    }
  }

  def outputs(reactives: Seq[Reactive]): Seq[Role[VariableElement]] =
    reactives.map {
      _.role
    }.collect {
      case x: Output[VariableElement]        ⇒ x
      case x: CommandOutput[VariableElement] ⇒ x
    }

  def getReactive(index: Int): Option[Reactive] = currentReactives().filter { _.index == index }.headOption

  def upButton = bs.div("centerWidth250")(
    tags.label(`class` := "inputFileStyle spacer5 certificate")(
      bs.fileInput((fInput: HTMLInputElement) ⇒ {
        FileManager.upload(fInput,
          manager.current.safePath(),
          (p: FileTransferState) ⇒ {
            transferring() = p
          },
          UploadProject(),
          () ⇒ {
            if (fInput.files.length > 0) {
              val fileName = fInput.files.item(0).name
              OMPost[Api].getCareBinInfos(manager.current.safePath() ++ fileName).call().foreach { b ⇒
                panels.treeNodePanel.refreshCurrentDirectory
                launchingCommand() = b
                hasStep2() = true
                labelName() = Some(fileName)
                launchingCommand().foreach { lc ⇒
                  currentReactives() = lc.arguments.flatMap { pp ⇒
                    pp match {
                      case ve: VariableElement ⇒ Some(buildReactive(CommandInput(ve)))
                      case _                   ⇒ None
                    }
                  }
                }
              }
            }
          }
        )
      }), Rx {
        labelName() match {
          case Some(s: String) ⇒ s
          case _               ⇒ "Your Model"
        }
      }
    )
  )

  val step1 = tags.div(
    tags.h4("Step 1: Code import"),
    tags.div("Pick your code up among jar archive, netlogo scripts, or any code packaged on linux with Care ( like Python, C, C++ " +
      "R, etc). In the case of a Care archive, the packaging has to be done with the",
      tags.b(" -o yourmodel.tar.gz.bin."),
      " option."
    )
  )

  val step2 = tags.div(
    tags.h4("Step 2: Code I/O settings"),
    tags.div("The systems detects automatically the launching command and propose you the creation of some OpenMOLE Variables so that" +
      " your model will be able to be feeded with variable values coming from the workflow you will build afterwards. In the case of Java, Scala, Netlogo" +
      "(ie codes working on the JVM) the OpenMOLE variables can be set directly in the command line. Otherwise, they have to be set inside ${} statements." +
      " By default he systems detects automatically your Variable changes and update the launching command. However, this option can be desactivated."
    )
  )
  val buildModelTaskButton = bs.button(
    "Build",
    btn_primary)(onclick := { () ⇒
      save
      launchingCommand().foreach { lc ⇒
        OMPost[Api].buildModelTask(
          labelName().getOrElse(""),
          commandArea.value,
          RLanguage(),
          inputs(currentReactives()).map {
            _.content.prototype
          },
          outputs(currentReactives()).map {
            _.content.prototype
          },
          manager.current.safePath()).call().foreach { b ⇒
            close
          }
      }
    })

  def save = {
    currentReactives().map {
      _.save
    }
  }

  def buildReactive(role: Role[VariableElement], index: Int): Reactive = Reactive(role, index)

  def buildReactive(role: Role[VariableElement]): Reactive = currentReactives().filter { _.role == role }.headOption.getOrElse(buildReactive(role, currentReactives().size))

  def addVariableElement(p: Role[VariableElement]) = {
    save
    currentReactives() = currentReactives() :+ buildReactive(p)
  }

  def applyOnPrototypePair(p: Role[VariableElement], todo: (Role[VariableElement], Int) ⇒ Unit) =
    currentReactives().map {
      _.role
    }.zipWithIndex.filter { case (ptp, index) ⇒ ptp == p }.foreach {
      case (role, index) ⇒ todo(role, index)
    }

  def updatePrototypePair(p: Role[VariableElement], variableElement: VariableElement) =
    applyOnPrototypePair(p, (role: Role[VariableElement], index: Int) ⇒ currentReactives() = currentReactives().updated(index, buildReactive(role.clone(variableElement), index)))

  def switchPrototypePair(p: Role[VariableElement]) = {
    save
    applyOnPrototypePair(p, (role: Role[VariableElement], index: Int) ⇒ currentReactives() = currentReactives().updated(index, buildReactive(role.switch, index)))
  }

  def addSwitchedPrototypePair(p: Role[VariableElement]) = {
    save
    currentReactives() = (currentReactives() :+ buildReactive(p.switch)) distinct
  }

  case class Reactive(role: Role[VariableElement], index: Int) {
    val lineHovered: Var[Boolean] = Var(false)

    val switchGlyph = role match {
      case x: Input[_] ⇒ OMTags.glyph_arrow_right
      case _           ⇒ OMTags.glyph_arrow_left
    }

    def updateLaunchingCommand =
      role match {
        case CommandInput(_) | CommandOutput(_) ⇒
          launchingCommand() = launchingCommand().map { lc ⇒
            val statics = lc.statics
            lc.copy(arguments = statics ++
              currentReactives().map { aaa ⇒
                aaa.role
              }.collect {
                case x: CommandInput[_]  ⇒ x
                case y: CommandOutput[_] ⇒ y
              }.map {
                _.content
              }
            )
          }
        case _ ⇒
      }

    def save = getReactive(index).map { reactive ⇒ updatePrototypePair(reactive.role, reactive.role.content.clone(nameInput.value, typeSelector.content().get, mappingInput.value)) }

    def removePrototypePair = {
      currentReactives() = currentReactives().filterNot(_.role == role)
      ModelWizardPanel.this.save
    }

    lazy val typeSelector: Select[ProtoTYPE.ProtoTYPE] = Select("modelProtos",
      ProtoTYPE.ALL.map {
        (_, emptyCK)
      }, Some(role.content.prototype.`type`),
      btn_primary, onclickExtra = () ⇒ {
        save
      }
    )

    def saveWithoutTableUpdate = {
      updatableTable() = false
      save
      updatableTable() = true
    }

    val mappingInput: HTMLInputElement = bs.input(role.content.prototype.mapping.getOrElse(""))(oninput := { () ⇒
      saveWithoutTableUpdate
    }).render

    val nameInput: HTMLInputElement = bs.input(role.content.prototype.name)(oninput := { () ⇒
      saveWithoutTableUpdate
      updateLaunchingCommand
    }
    ).render

    val line = {
      typeSelector.content() = Some(role.content.prototype.`type`)
      tags.tr(
        onmouseover := { () ⇒
          lineHovered() = true
        },
        onmouseout := { () ⇒
          lineHovered() = false
        },
        bs.td(bs.col_md_3 + "spacer7")(nameInput),
        bs.td(bs.col_md_2)(typeSelector.selector),
        bs.td(bs.col_md_3)(if (role.content.prototype.`type` == ProtoTYPE.FILE) mappingInput else tags.div()),
        bs.td(bs.col_md_1 + "right")(
          id := Rx {
            "treeline" + {
              if (lineHovered()) "-hover" else ""
            }
          }, glyphSpan(switchGlyph, () ⇒ switchPrototypePair(role))(id := "glyphtrash", `class` := "glyphitem grey spacer2"),
          glyphSpan(OMTags.glyph_arrow_right_and_left, () ⇒ addSwitchedPrototypePair(role))(id := "glyphtrash", `class` := "glyphitem grey spacer2")
        ), bs.td(bs.col_md_1 + "right")(
          id := Rx {
            "treeline" + {
              if (lineHovered()) "-hover" else ""
            }
          },
          glyphSpan(glyph_trash, () ⇒ removePrototypePair)(id := "glyphtrash", `class` := "glyphitem grey spacer2")
        )
      )
    }
  }

  def setBodyContent = bodyContent() = Some({
    val reactives = currentReactives()
    tags.div(
      hasStep2() match {
        case true ⇒ tags.div()
        case _    ⇒ step1
      },
      transferring() match {
        case _: Transfering ⇒ OMTags.waitingSpan(" Uploading ...", btn_danger + "certificate")
        case _: Transfered  ⇒ upButton
        case _              ⇒ upButton
      },
      hasStep2() match {
        case true ⇒
          tags.div(step2, bs.div("spacer7")({

            val iinput: HTMLInputElement = bs.input("")(placeholder := "Add Input").render

            val oinput: HTMLInputElement = bs.input("")(placeholder := "Add Output").render

            val head = thead(tags.tr(
              for (h ← Seq("Name", "Type", "File mapping", "", "")) yield {
                tags.th(h)
              }))

            tags.div(
              tags.div(`class` := "twocolumns right10")(
                bs.form("paddingLeftRight50")(iinput,
                  onsubmit := {
                    () ⇒
                      addVariableElement(Input(VariableElement(-1, ProtoTypePair(iinput.value, ProtoTYPE.DOUBLE), CareTaskType())))
                      iinput.value = ""
                      false
                  }),
                bs.table(striped)(
                  head,
                  tbody(
                    for (ip ← inputs(reactives)) yield {
                      ip.line
                    }))),
              tags.div(`class` := "twocolumns")(
                bs.form("paddingLeftRight50")(oinput, onsubmit := {
                  () ⇒
                    addVariableElement(Output(VariableElement(-1, ProtoTypePair(oinput.value, ProtoTYPE.DOUBLE), CareTaskType())))
                    oinput.value = ""
                    false
                }),
                bs.table(striped)(
                  head,
                  tbody(
                    for (op ← outputs(reactives)) yield {
                      op.line
                    }
                  )
                )
              )
            )
          }
          ), commandArea)
        case _ ⇒ tags.div()
      }
    )
  })

  lazy val dialog = {
    setBodyContent
    bs.modalDialog(modalID,
      headerDialog(
        tags.span(tags.b("Model import"))
      ),
      bodyDialog(Rx {
        bodyContent().getOrElse(tags.div())
      }),
      footerDialog(bs.buttonGroup()(
        closeButton,
        buildModelTaskButton
      )
      )
    )
  }

}