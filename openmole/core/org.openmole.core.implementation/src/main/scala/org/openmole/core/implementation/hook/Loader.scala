/*
 * Copyright (C) 2010 reuillon
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
 */

package org.openmole.core.implementation.hook

import java.io.File
import org.openmole.core.model.data.IContext
import org.openmole.core.model.persistence.PersistentContext

class Loader(dir: File) extends Iterable[IContext] {
  
  def iterator(): Iterator[IContext] = new Iterator[IContext] {
    val contextLinks = new File(dir, PersistentContext.CONTEXT_LINK).listFiles.iterator
    
    override def hasNext = contextLinks.hasNext
    override def next = {
      val contextLink = contextLinks.next
      val contextHash = contextLink.getName.substring(0, contextLink.getName.indexOf(PersistentContext.SEPARATOR))
      val contextFile = new File(new File(dir, PersistentContext.CONTEXT), contextHash)
      null
    }
      
  }
  
  
}
