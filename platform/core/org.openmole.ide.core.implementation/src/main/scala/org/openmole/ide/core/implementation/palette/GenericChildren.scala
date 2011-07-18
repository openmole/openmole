/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
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

package org.openmole.ide.core.implementation.palette

import org.openide.nodes.Index
import org.openide.nodes.Node
import org.openmole.ide.core.model.dataproxy.IDataProxyUI
import org.openmole.ide.core.model.palette.IGenericChildren
import java.awt.datatransfer.DataFlavor
import java.util.ArrayList

class GenericChildren(collection: Iterable[IDataProxyUI], dataFlavor: DataFlavor) extends Index.ArrayChildren with IGenericChildren{

  def refreshNodes= refresh
  
  override def initCollection: java.util.List[Node] = {
    val childrenNodes = new ArrayList[Node](collection.size)
    collection.foreach(dpu=>{childrenNodes.add(new GenericNode(dataFlavor,dpu))})
    childrenNodes
  }
}