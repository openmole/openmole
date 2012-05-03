package org.openmole.ide.misc.tools.check

/*
 * Copyright (C) 19/03/12 <mathieu.leclaire at openmole.org>
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

import org.openmole.core.model.data.IPrototype
import org.openmole.misc.tools.script.GroovyProxy

object TypeCheck {

  def apply(code: String,
            prototype: IPrototype[_]): (Boolean, String) = {
    try {
      apply(new GroovyProxy(code).execute(), prototype)
    } catch {
      case e ⇒ (false, e.getMessage)
    }
  }

  def apply(groovyObject: Object,
            prototype: IPrototype[_]): (Boolean, String) =
    prototype.accepts(groovyObject) match {
      case true ⇒ (true, groovyObject.toString)
      case false ⇒ (false, "The default value for the prototype " + prototype.name + " is not valid ( " + prototype.`type` + " is required )")
    }
}