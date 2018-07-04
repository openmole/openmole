/**
  * Created by Mathieu Leclaire on 23/04/18.
  *
  * This program is free software: you can redistribute it and/or modify
  * it under the terms of the GNU Affero General Public License as published by
  * the Free Software Foundation, either version 3 of the License, or
  * (at your option) any later version.
  *
  * This program is distributed in the hope that it will be useful,
  * but WITHOUT ANY WARRANTY; without even the implied warranty of
  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  * GNU General Public License for more details.
  *
  * You should have received a copy of the GNU General Public License
  * along with this program.  If not, see <http://www.gnu.org/licenses/>.
  *
  */
package org.openmole.gui.plugin.wizard.jar

import scala.concurrent.ExecutionContext.Implicits.global
import boopickle.Default._
import org.openmole.gui.ext.data._
import org.openmole.gui.ext.tool.client.{InputFilter, OMPost}
import scaladget.bootstrapnative.bsn._
import scaladget.tools._
import autowire._
import org.scalajs.dom.raw.HTMLElement
import scaladget.bootstrapnative.SelectableButtons

import scala.concurrent.Future
import scala.scalajs.js.annotation._
import scalatags.JsDom.TypedTag
import scalatags.JsDom.all._
import org.openmole.gui.ext.api.Api
import org.openmole.gui.ext.data.DataUtils._
import org.openmole.gui.ext.tool.client
import rx._

@JSExportTopLevel("org.openmole.gui.plugin.wizard.jar.JarWizardFactory")
class JarWizardFactory extends WizardPluginFactory {
  type WizardType = JarWizardData

  val fileType = JarArchive

  def build(safePath: SafePath, onPanelFilled: (LaunchingCommand) ⇒ Unit = (LaunchingCommand) ⇒ {}): WizardGUIPlugin = new JarWizardGUI(safePath, onPanelFilled)

  def parse(safePath: SafePath): Future[Option[LaunchingCommand]] = OMPost()[JarWizardAPI].parse(safePath).call()

  def name: String = "Jar"
}

@JSExportTopLevel("org.openmole.gui.plugin.wizard.jar.JarWizardGUI")
class JarWizardGUI(safePath: SafePath, onMethodSelected: (LaunchingCommand) ⇒ Unit) extends WizardGUIPlugin {
  type WizardType = JarWizardData

  def factory = new JarWizardFactory

  val jarClasses: Var[Seq[FullClass]] = Var(Seq())
  val isOSGI: Var[Boolean] = Var(false)

  OMPost()[Api].isOSGI(safePath).call().foreach {o=>
    isOSGI() = o
  }

  lazy val embedAsPluginCheckBox: SelectableButtons = radios()(
    selectableButton("Yes", onclick = () ⇒ println("YES")),
    selectableButton("No", onclick = () ⇒ println("NO"))
  )

  val classTable: Var[Option[scaladget.bootstrapnative.DataTable]] = Var(None)
  val methodTable: Var[Option[scaladget.bootstrapnative.DataTable]] = Var(None)

  searchClassInput.nameFilter.trigger {
    classTable.now.foreach { t ⇒
      t.filter(searchClassInput.nameFilter.now)
    }
  }

  OMPost()[JarWizardAPI].jarClasses(safePath).call().foreach { jc ⇒
    val table = scaladget.bootstrapnative.DataTable(
      rows = jc.map { c ⇒
        scaladget.bootstrapnative.DataTable.DataRow(Seq(c.name))
      }.toSeq,
      bsTableStyle = scaladget.bootstrapnative.Table.BSTableStyle(bordered_table +++ hover_table, emptyMod))

    classTable() = Some(table)

    classTable.now.get.selected.trigger {
      classTable.now.get.selected.now.foreach { s ⇒
        OMPost()[JarWizardAPI].jarMethods(safePath, s.values.head).call().foreach { jm ⇒
          val methodMap = jm.map { m ⇒ m.expand -> m }.toMap
          methodTable() = Some(
            scaladget.bootstrapnative.DataTable(
              rows = jm.map { m ⇒
                scaladget.bootstrapnative.DataTable.DataRow(Seq(m.expand))
              }.toSeq,
              bsTableStyle = scaladget.bootstrapnative.Table.BSTableStyle(bordered_table +++ hover_table, emptyMod))
          )

          methodTable.now.get.selected.trigger {
            methodTable.now.get.selected.now.map(r ⇒ methodMap(r.values.head)).foreach { selectedMethod ⇒
              onMethodSelected(JavaLaunchingCommand(
                selectedMethod,
                selectedMethod.args, selectedMethod.ret.map {
                  Seq(_)
                }.getOrElse(Seq()))
              )
            }
          }
        }
      }

    }
  }

  lazy val searchClassInput = InputFilter("", "Ex: mynamespace.MyClass")

  val tableCSS: ModifierSeq = Seq(
    overflow := "auto",
    height := 300,
  )

  lazy val panel: TypedTag[HTMLElement] = div(
  div(client.columnCSS)(
      Rx{
      if(isOSGI()) hForm(
        div(embedAsPluginCheckBox.render)
          .render.withLabel("Embed jar as plugin ?"),
        span(client.modelHelp +++ client.columnCSS, "Your jar is an OSGI bundle. The best way to use it is to embed it as a plugin.").render
      ) else div(client.modelHelp,
        div("Your jar in not an OSGI bundle. An OSGI bundle is safer and more robust, so that we recomend you to render it as an OSGI bundle."),
        a(href := "http://www.openmole.org/Plugins.html", target := "_blank")("How to create an OSGI bundle ?"))},
      h3("Classes"),
      searchClassInput.tag,
      div(tableCSS, paddingTop := 10)(
        Rx {
          classTable().map {
            _.render
          }.getOrElse(div())
        }).render
    ),
    div(client.columnCSS)(
      h3("Methods"),
      div(tableCSS)(
        Rx {
          methodTable().map {
            _.render
          }.getOrElse(div())
        }).render
    )
  )

  def save(
            target: SafePath,
            executableName: String,
            command: String,
            inputs: Seq[ProtoTypePair],
            outputs: Seq[ProtoTypePair],
            libraries: Option[String],
            resources: Resources) = {
    val embedAsPlugin = if (embedAsPluginCheckBox.activeIndex == 0) true else false

    val plugin: Option[String] = {
      if(embedAsPlugin) classTable.now.map{_.selected.now.map{_.values.headOption}.flatten}.flatten
      else None
    }

    OMPost()[JarWizardAPI].toTask(
      target,
      executableName,
      command,
      inputs,
      outputs,
      libraries,
      resources,
      JarWizardData(embedAsPlugin, plugin, safePath)).call()
  }
}