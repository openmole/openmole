///**
// * Created by Mathieu Leclaire on 23/04/18.
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// *
// */
//package org.openmole.gui.plugin.wizard.jar
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import boopickle.Default._
//import org.openmole.gui.ext.data._
//import org.openmole.gui.ext.client.{InputFilter, OMPost}
//import scaladget.bootstrapnative.bsn._
//import scaladget.tools._
//import autowire._
//import org.scalajs.dom.raw.HTMLElement
//
//import scala.concurrent.Future
//import scala.scalajs.js.annotation._
//import com.raquo.laminar.api.L._
//import org.openmole.gui.ext.api.Api
//import org.openmole.gui.ext.data.DataUtils._
//import org.openmole.gui.ext.client
//import scalajs.js
//
//object TopLevelExports {
//  @JSExportTopLevel("jar")
//  val jar = js.Object {
//    new org.openmole.gui.plugin.wizard.jar.JarWizardFactory
//  }
//}
//
//class JarWizardFactory extends WizardPluginFactory {
//  type WizardType = JarWizardData
//
//  val fileType = JarArchive
//
//  def build(safePath: SafePath, onPanelFilled: (LaunchingCommand) ⇒ Unit = (LaunchingCommand) ⇒ {}): WizardGUIPlugin = new JarWizardGUI(safePath, onPanelFilled)
//
//  def parse(safePath: SafePath): Future[Option[LaunchingCommand]] = OMPost()[JarWizardAPI].parse(safePath).call()
//
//  def name: String = "Jar"
//}
//
//class JarWizardGUI(safePath: SafePath, onMethodSelected: (LaunchingCommand) ⇒ Unit) extends WizardGUIPlugin {
//  type WizardType = JarWizardData
//
//  def factory = new JarWizardFactory
//
//  val jarClasses: Var[Seq[FullClass]] = Var(Seq())
//  val isOSGI: Var[Boolean] = Var(false)
//
//  OMPost()[Api].isOSGI(safePath).call().foreach { o =>
//    isOSGI.set(o)
//  }
//
//  val yes = ToggleState("Yes", btn_primary_string)
//  val no = ToggleState("No", btn_secondary_string)
//
//  lazy val embedAsPluginElement = toggle(yes, true, no, ()=> {})
//
//  val classTable: Var[Option[scaladget.bootstrapnative.DataTable]] = Var(None)
//  val methodTable: Var[Option[scaladget.bootstrapnative.DataTable]] = Var(None)
//
//  searchClassInput.nameFilter.trigger {
//    classTable.now.foreach { t ⇒
//      t.filter(searchClassInput.nameFilter.now)
//    }
//  }
//
//  OMPost()[JarWizardAPI].jarClasses(safePath).call().foreach { jc ⇒
//    val table = scaladget.bootstrapnative.DataTable(
//      jc.map { c ⇒
//        scaladget.bootstrapnative.Table.DataRow(Seq(c.name))
//      }.toSeq,
//      bsTableStyle = scaladget.bootstrapnative.Table.BSTableStyle(bordered_table, hover_table))
//
//    classTable.set(Some(table))
//
//    classTable.now.get.selected.trigger {
//      classTable.now.get.selected.now.foreach { s ⇒
//        OMPost()[JarWizardAPI].jarMethods(safePath, s.values.head).call().foreach { jm ⇒
//          val methodMap = jm.map { m ⇒ m.expand -> m }.toMap
//          methodTable.set(Some(
//            scaladget.bootstrapnative.DataTable(
//              rows = jm.map { m ⇒
//                scaladget.bootstrapnative.Table.DataRow(Seq(m.expand))
//              }.toSeq,
//              bsTableStyle = scaladget.bootstrapnative.Table.BSTableStyle(bordered_table, hover_table))
//          ))
//
//          methodTable.now.get.selected.trigger {
//            methodTable.now.get.selected.now.map(r ⇒ methodMap(r.values.head)).foreach { selectedMethod ⇒
//              onMethodSelected(JavaLaunchingCommand(
//                selectedMethod,
//                selectedMethod.args, selectedMethod.ret.map {
//                  Seq(_)
//                }.getOrElse(Seq()))
//              )
//            }
//          }
//        }
//      }
//
//    }
//  }
//
//  lazy val searchClassInput = InputFilter("", "Ex: mynamespace.MyClass")
//
//  val tableCSS: HESetters = Seq(
//    overflow := "auto",
//    height := "300",
//  )
//
//  lazy val panel: HtmlElement =
//    div(client.columnCSS,
//      child <-- isOSGI.signal.map { osgi =>
//        if (osgi) hForm(
//          embedAsPluginElement.element.withLabel("Embed jar as plugin?"),
//          span(client.modelHelp, client.columnCSS, "Your jar is an OSGI bundle. The best way to use it is to embed it as a plugin.")
//        ) else div(client.modelHelp,
//          div("Your jar is not an OSGI bundle. As an OSGI bundle is safer and more robust, we recommend you convert your jar to an OSGI bundle."),
//          a(href := "http://www.openmole.org/Plugin+Development.html", target := "_blank", "How to create an OSGI bundle?")
//        )
//      },
//      h3("Classes"),
//      searchClassInput.tag,
//      div(tableCSS, paddingTop := "10",
//        child <-- classTable.signal.map {
//          _.map{_.render}.getOrElse(div())
//        }
//      ),
//      div(client.columnCSS,
//        h3("Methods"),
//        div(tableCSS,
//          child <-- methodTable.signal.map{
//            _.map{_.render}.getOrElse(div())
//          }
//        )
//      )
//    )
//
//  def save(
//            target: SafePath,
//            executableName: String,
//            command: String,
//            inputs: Seq[ProtoTypePair],
//            outputs: Seq[ProtoTypePair],
//            libraries: Option[String],
//            resources: Resources) = {
//    val embedAsPlugin = {
//      if (isOSGI.now) embedAsPluginElement.toggled.now
//      else false
//    }
//
//    val plugin: Option[String] = {
//      if (embedAsPlugin) classTable.now.map {
//        _.selected.now.map {
//          _.values.headOption
//        }.flatten
//      }.flatten
//      else None
//    }
//
//    OMPost()[JarWizardAPI].toTask(
//      target,
//      executableName,
//      command,
//      inputs,
//      outputs,
//      libraries,
//      resources,
//      JarWizardData(embedAsPlugin, plugin, safePath)).call()
//  }
//}