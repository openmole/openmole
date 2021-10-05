///**
// * Created by Mathieu Leclaire on 19/04/18.
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
//package org.openmole.gui.plugin.wizard.native
//
//import scala.concurrent.ExecutionContext.Implicits.global
//import boopickle.Default._
//import org.openmole.gui.ext.data._
//import org.openmole.gui.ext.client.OMPost
//import autowire._
//import org.scalajs.dom.raw.HTMLElement
//
//import scala.concurrent.Future
//import scala.scalajs.js.annotation._
//import scalatags.JsDom.TypedTag
//import scalatags.JsDom.all._
//import rx._
//
//import scala.scalajs.js
//
//object TopLevelExports {
//  @JSExportTopLevel("native")
//  val native = js.Object {
//    new org.openmole.gui.plugin.wizard.native.NativeWizardFactory
//  }
//}
//
//class NativeWizardFactory extends WizardPluginFactory {
//  val fileType = CareArchive
//
//  def build(safePath: SafePath, onPanelFilled: (LaunchingCommand) ⇒ Unit = (LaunchingCommand) ⇒ {}): WizardGUIPlugin = new NativeWizardGUI
//
//  def parse(safePath: SafePath): Future[Option[LaunchingCommand]] = OMPost()[NativeWizardAPI].parse(safePath).call()
//
//  def name: String = "Care"
//}
//
//case class NativeWizardData() extends WizardData
//
//class NativeWizardGUI extends WizardGUIPlugin {
//
//  type WizardType = NativeWizardData
//
//  def factory = new NativeWizardFactory
//
//  lazy val panel: TypedTag[HTMLElement] = div()
//
//  def save(
//    target:         SafePath,
//    executableName: String,
//    command:        String,
//    inputs:         Seq[ProtoTypePair],
//    outputs:        Seq[ProtoTypePair],
//    libraries:      Option[String],
//    resources:      Resources) =
//    OMPost()[NativeWizardAPI].toTask(
//      target,
//      executableName,
//      command,
//      inputs,
//      outputs,
//      libraries,
//      resources,
//      NativeWizardData()
//    ).call()
//}