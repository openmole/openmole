package org.openmole.gui.ext

/*
 * Copyright (C) 16/12/14 // mathieu.leclaire@openmole.org
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

import org.openmole.gui.ext.data.PrototypeData
import rx._
import org.scalajs.dom
import scalatags.JsDom.TypedTag

package object dataui {
  type InputsUI = Var[Seq[Var[(PrototypeDataUI, Option[String])]]]
  type OutputsUI = Var[Seq[Var[PrototypeDataUI]]]

  type Inputs = Seq[(PrototypeData, Option[String])]
  type Outputs = Seq[PrototypeData]

  type Inputable = { def inputs: InputsUI }
  type Outputable = { def outputs: OutputsUI }

  type IODataUI = DataUI with Inputable with Outputable
}