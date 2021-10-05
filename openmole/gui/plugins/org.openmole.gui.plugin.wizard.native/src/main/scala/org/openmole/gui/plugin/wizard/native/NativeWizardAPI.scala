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
//package org.openmole.gui.plugin.wizard.native
//
//import org.openmole.gui.ext.data._
//
//trait NativeWizardAPI {
//  def toTask(
//    target:         SafePath,
//    executableName: String,
//    command:        String,
//    inputs:         Seq[ProtoTypePair],
//    outputs:        Seq[ProtoTypePair],
//    libraries:      Option[String],
//    resources:      Resources,
//    data:           NativeWizardData): WizardToTask
//
//  def parse(safePath: SafePath): Option[LaunchingCommand]
//}