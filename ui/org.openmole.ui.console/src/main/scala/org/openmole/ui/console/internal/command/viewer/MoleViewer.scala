/*
 * Copyright (C) 2011 reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
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

package org.openmole.ui.console.internal.command.viewer

import org.openmole.core.model.mole.IMole
import org.openmole.core.model.transition.IAggregationTransition
import org.openmole.core.model.transition.IExplorationTransition

class MoleViewer extends IViewer {

  override def view(obj: Object, args: Array[String]) = {
    val mole = obj.asInstanceOf[IMole]
    
    mole.capsules.zipWithIndex.foreach { 
      case(c, i) => 
        println(i + " " + c + " (" + c.outputTransitions.map{
            t =>
              (t match {
                case _: IExplorationTransition => "< "
                case _: IAggregationTransition => "> "
                case _ => "- "
              }) + t.end.capsule.toString
          }.foldLeft("") {
            (acc, c) => if(acc.isEmpty) c else acc + ", " + c
          } + ")")
    }
    
  }
  
}
