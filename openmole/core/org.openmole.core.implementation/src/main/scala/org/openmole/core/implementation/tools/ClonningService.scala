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

package org.openmole.core.implementation.tools

import com.rits.cloning.Cloner
import com.rits.cloning.IFastCloner
import java.io.File
import java.math.BigInteger
import java.util.logging.Level
import java.util.logging.Logger

import org.openmole.commons.tools.io.FileUtil
import org.openmole.core.implementation.data.Variable
import org.openmole.core.implementation.internal.Activator
import org.openmole.core.model.data.IVariable

object ClonningService {
    
    def clone[T](variable: IVariable[T]): IVariable[T] = {
      Logger.getLogger(ClonningService.getClass.getName).log(Level.FINE, "Clonning {0}", variable.prototype)
        
        if (variable.value == null || 
            variable.value.asInstanceOf[AnyRef].getClass.isPrimitive ||
                variable.value.asInstanceOf[AnyRef].getClass == classOf[BigDecimal] ||
                variable.value.asInstanceOf[AnyRef].getClass == classOf[BigInteger]) {
            return variable
        }
        
        val cloner = new Cloner
       // val exceptions = Collections.synchronizedList(new LinkedList<Throwable>());

        cloner.registerFastCloner(classOf[File], new IFastCloner {

            override def clone(o: Object, cloner: Cloner, map: java.util.Map[Object, Object]): Object = {
                val toClone = o.asInstanceOf[File]
                val cloned = if (toClone.isDirectory) Activator.getWorkspace.newDir else Activator.getWorkspace.newFile
                    
                FileUtil.copy(toClone, cloned)
                cloned
            }
        })

        cloner.registerImmutable(classOf[BigDecimal])
        cloner.registerImmutable(classOf[BigInteger])
        
        //val cloned = cloner.deepClone(variable.value)

       /* if(!exceptions.isEmpty()) {
            throw new InternalProcessingError(new MultipleException(exceptions));
        }*/

        new Variable(variable.prototype, cloner.deepClone(variable.value))
    }
}
