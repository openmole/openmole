/*
 *  Copyright (C) 2010 Romain Reuillon
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

package org.openmole.core.workflow.model.job;

import java.util.List;
import java.util.Map;
import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.commons.tools.pattern.IVisitable;

import org.openmole.core.workflow.model.data.IVariable;

public interface IContext extends IVisitable<IVariable>, Iterable<IVariable> {

    void chRoot();

    abstract IContext getRoot();

    void setRoot(IContext root);

    boolean isRoot();

    void setVariables(List<IVariable<?>> variables);

    Map<String, IVariable> getVariables();

    <T> IVariable<T> getVariable(String name);

    <T> IVariable<? extends T> getVariable(IPrototype<T> proto);

    void putVariable(IVariable<?> variable);

    void putVariable(String name, Object value);

    <T> void putVariable(String name, Class<? super T> type, T value);

    void putGlobalVariable(String name, Object value);

    <T> void putGlobalVariable(IPrototype<? super T> proto, T value);

    <T> void putVariable(IPrototype<? super T> proto, T value);

    <IT> IT getLocalValue(String name);

    <IT> IT getLocalValue(IPrototype<IT> proto);

    //	@Override
    <IT> IVariable<IT> getLocalVariable(String name);

    <IT> IVariable<IT> getLocalVariable(IPrototype<IT> proto);

    void removeVariable(String name);

    boolean containsVariableWithName(IPrototype<?> proto);

    boolean containsVariableWithName(String name);

    boolean contains(IPrototype<?> proto);

    boolean existGlobal(IPrototype<?> proto);

    IVariable<?> getGlobalVariable(String name);

    <IT> IT getGlobalValue(String name);

    <IT> IT getGlobalValue(IPrototype<? extends IT> proto);

    void setValue(String name, Object value);

    <T> void setValue(IPrototype<? super T> proto, T value);

    /**
     * Reset the content of the context. After this operation, the context doesn't
     * contain any variable, and is ready to use.
     */
    void clean();

    void removeGlobalVariable(String name);
}
