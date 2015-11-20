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
import org.scalajs.dom.raw.HTMLInputElement
import org.openmole.gui.misc.js.JsRxTags._
import rx._
import org.openmole.gui.shared.Api
import scalatags.JsDom.{ tags ⇒ tags }

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

  implicit def pairToLine(prototypePair: Role[ProtoTypePair]): Reactive = Reactive(prototypePair)

  implicit def stringToOptionString(s: String): Option[String] = if (s.isEmpty) None else Some(s)

  val transferring: Var[FileTransferState] = Var(Standby())
  val labelName: Var[Option[String]] = Var(None)
  val launchingCommand: Var[Option[LaunchingCommand]] = Var(None)
  val currentReactives: Var[Seq[Reactive]] = Var(Seq())
  val commandArea: Var[Option[TextArea]] = Var(None)

  Obs(launchingCommand) {
    launchingCommand() match {
      case Some(lc: LaunchingCommand) ⇒
        commandArea() = Some(bs.textArea(5)(lc.fullCommand).render)
        commandArea().get
      case _ ⇒ tags.div()
    }
  }

  def inputs: Seq[Input[ProtoTypePair]] =
    currentReactives().map {
      _.role
    }.collect { case x: Input[ProtoTypePair] ⇒ x }

  def outputs: Seq[Output[ProtoTypePair]] =
    currentReactives().map {
      _.role
    }.collect { case x: Output[ProtoTypePair] ⇒ x }

  lazy val upButton = tags.label(`class` := "inputFileStyle spacer5 certificate marginLeft100")(
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
              launchingCommand() = b
              labelName() = Some(fileName)
              launchingCommand().foreach { lc ⇒ currentReactives() = lc.arguments.map { pp ⇒ Reactive(Input(pp)) } }
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

  val buildModelTaskButton = bs.button(
    "Build",
    btn_primary)(onclick := { () ⇒
      save
      launchingCommand().foreach { lc ⇒
        OMPost[Api].buildModelTask(
          labelName().getOrElse(""),
          lc.fullCommand,
          RLanguage(),
          inputs.map {
            _.content
          },
          outputs.map {
            _.content
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

  def addPrototypePair(p: Role[ProtoTypePair]) = {
    save
    currentReactives() = currentReactives() :+ Reactive(p)
  }

  def applyOnPrototypePair(p: Role[ProtoTypePair], todo: (Role[ProtoTypePair], Int) ⇒ Unit) =
    currentReactives().map {
      _.role
    }.zipWithIndex.filter { case (ptp, index) ⇒ ptp.content.name == p.content.name }.foreach {
      case (role, index) ⇒ todo(role, index)
    }

  def updatePrototypePair(p: Role[ProtoTypePair], newPrototypePair: ProtoTypePair) =
    applyOnPrototypePair(p, (role: Role[ProtoTypePair], index: Int) ⇒ currentReactives() = currentReactives().updated(index, Reactive(role.clone(newPrototypePair))))

  def switchPrototypePair(p: Role[ProtoTypePair]) = {
    save
    applyOnPrototypePair(p, (role: Role[ProtoTypePair], index: Int) ⇒ currentReactives() = currentReactives().updated(index, Reactive(role.switch)))
  }

  def addSwitchedPrototypePair(p: Role[ProtoTypePair]) = {
    save
    currentReactives() = (currentReactives() :+ Reactive(p.switch)) distinct
  }

  case class Reactive(role: Role[ProtoTypePair]) {
    val lineHovered: Var[Boolean] = Var(false)

    val switchGlyph = role match {
      case x: Input[_] ⇒ OMTags.glyph_arrow_right
      case _           ⇒ OMTags.glyph_arrow_left
    }

    def save = updatePrototypePair(role, role.content.copy(`type` = typeSelector.content().get, mapping = mappingInput.value))

    def removePrototypePair = {
      ModelWizardPanel.this.save
      currentReactives() = currentReactives().filterNot(_.role == role)
    }

    lazy val typeSelector: Select[ProtoTYPE.ProtoTYPE] = Select("modelProtos",
      ProtoTYPE.ALL.map {
        (_, emptyCK)
      }, Some(role.content.`type`),
      btn_primary, onclickExtra = () ⇒ {
        save
      }
    )

    val mappingInput: HTMLInputElement = bs.input(role.content.mapping.getOrElse(""))(onblur := { () ⇒
      save
    }).render

    lazy val line = {
      typeSelector.content() = Some(role.content.`type`)
      tags.tr(
        onmouseover := { () ⇒
          lineHovered() = true
        },
        onmouseout := { () ⇒
          lineHovered() = false
        },
        bs.td(bs.col_md_3 + "spacer7 greyBold")(role.content.name),
        bs.td(bs.col_md_2)(typeSelector.selector),
        bs.td(bs.col_md_3)(if (role.content.`type` == ProtoTYPE.FILE) mappingInput else tags.div()),
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

  lazy val iinput: HTMLInputElement = bs.input("")(placeholder := "Add Input").render

  lazy val oinput: HTMLInputElement = bs.input("")(placeholder := "Add Output").render

  lazy val prototypeTable = bs.div("spacer7")(
    Rx {
      tags.div(
        tags.div(`class` := "twocolumns right10")(
          tags.form(iinput, onsubmit := {
            () ⇒
              addPrototypePair(Input(ProtoTypePair(iinput.value, ProtoTYPE.DOUBLE)))
              iinput.value = ""
              false
          }),
          bs.table(striped)(
            tbody(
              for (ip ← inputs) yield {
                ip.line
              }))),
        tags.div(`class` := "twocolumns")(
          tags.form(oinput, onsubmit := {
            () ⇒
              addPrototypePair(Output(ProtoTypePair(oinput.value, ProtoTYPE.DOUBLE)))
              oinput.value = ""
              false
          }),
          bs.table(striped)(
            tbody(
              for (op ← outputs) yield {
                op.line
              }
            )
          )
        )
      )
    }
  )

  val dialog = bs.modalDialog(modalID,
    headerDialog(
      tags.span(tags.b("Model import"))
    ),
    bodyDialog(Rx {
      tags.div(
        transferring() match {
          case _: Transfering ⇒ OMTags.waitingSpan(" Uploading ...", btn_danger + "certificate")
          case _: Transfered ⇒
            panels.treeNodePanel.refreshCurrentDirectory
            upButton
          case _ ⇒ upButton
        }, prototypeTable,
        commandArea() match {
          case Some(t: TextArea) ⇒ t
          case _                 ⇒ tags.div()
        },
        buildModelTaskButton
      )
    }
    ),
    footerDialog(closeButton)
  )

}
