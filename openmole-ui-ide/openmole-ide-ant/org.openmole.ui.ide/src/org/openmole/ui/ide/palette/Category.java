/*
 *  Copyright (C) 2010 Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ui.ide.palette;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@openmole.fr>
 */


public class Category{

    public enum CategoryName {
	TASK,
        TASK_INSTANCE,
	TASK_CAPSULE,
	TASK_CAPSULE_INSTANCE,
	PROTOTYPE,
	PROTOTYPE_INSTANCE,
    }

    public static String toString(CategoryName cat){
        
        if (cat == CategoryName.TASK) return "task";
        else if(cat == CategoryName.TASK_CAPSULE) return "taskcapsule";
        else if(cat == CategoryName.PROTOTYPE) return "prototype";
        return "unknown string";
        }
    
}