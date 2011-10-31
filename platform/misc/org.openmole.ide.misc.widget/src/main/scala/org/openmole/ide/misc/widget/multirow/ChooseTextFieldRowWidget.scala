/*
 * Copyright (C) 2011 <mathieu.leclaire at openmole.org>
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.openmole.ide.misc.widget.multirow

import org.openmole.ide.misc.widget.ChooseFileTextField
import scala.swing.Component

class ChooseTextFieldRowWidget(initValue: String) extends IRowWidget{
  override val components: List[ChooseFileTextField] = List(new ChooseFileTextField(initValue))
  
  override def buildEmptyRow: IRowWidget = new ChooseTextFieldRowWidget("")
  
  override def content: List[(Component,String)] = components.map(c=> (c,c.text)).filterNot(_._2.isEmpty).toList
}
