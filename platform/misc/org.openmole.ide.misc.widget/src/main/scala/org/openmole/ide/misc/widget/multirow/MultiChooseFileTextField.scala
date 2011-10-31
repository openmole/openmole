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

import scala.swing.FileChooser.SelectionMode._

class MultiChooseFileTextField(rowName: String, 
                               initValues: List[String], 
                               chooserTitle: String="", 
                               chooserDescription: Option[String]=None, 
                               selectionMode: Value= FilesOnly,
                               extensions: Option[String]= None) extends MultiWidget(rowName,
            if (initValues.isEmpty) List(new ChooseTextFieldRowWidget("",chooserTitle,chooserDescription,selectionMode,extensions)) 
            else initValues.map(iv=>new ChooseTextFieldRowWidget(iv,chooserTitle,chooserDescription,selectionMode,extensions)),1)
