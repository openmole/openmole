/*
 *  Copyright (C) 2010 reuillon
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

package org.openmole.core.implementation.job;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.pattern.IVisitor;
import org.openmole.core.model.data.IPrototype;
import org.openmole.core.model.data.IVariable;
import org.openmole.core.model.job.IContext;

/**
 *
 * @author reuillon
 */
public class SynchronizedContext implements IContext {

    final IContext context;
    
    public SynchronizedContext() {
        this(new Context());
    }
    
    public SynchronizedContext(IContext context) {
        this.context = context;
    }

    @Override
    public synchronized void putVariables(List<IVariable<?>> variables) {
        context.putVariables(variables);
    }

    @Override
    public synchronized <T> void setValue(IPrototype<? super T> proto, T value) {
        context.<T>setValue(proto, value);
    }

    @Override
    public synchronized void setValue(String name, Object value) {
        context.setValue(name, value);
    }

    @Override
    public synchronized void removeVariable(String name) {
        context.removeVariable(name);
    }

    @Override
    public synchronized <T> void putVariable(IPrototype<? super T> proto, T value) {
        context.<T>putVariable(proto, value);
    }

    @Override
    public synchronized <T> void putVariable(String name, Class<? super T> type, T value) {
        context.<T>putVariable(name, type, value);
    }

    @Override
    public synchronized void putVariable(String name, Object value) {
        context.putVariable(name, value);
    }

    @Override
    public synchronized void putVariable(IVariable<?> variable) {
        context.putVariable(variable);
    }

    @Override
    public synchronized Map<String, IVariable> getVariables() {
        return context.getVariables();
    }

    @Override
    public synchronized <T> IVariable<? extends T> getVariable(IPrototype<T> proto) {
        return context.<T>getVariable(proto);
    }

    @Override
    public synchronized <T> IVariable<T> getVariable(String name) {
        return context.<T>getVariable(name);
    }

    @Override
    public synchronized <IT> IT getValue(IPrototype<IT> proto) {
        return context.<IT>getValue(proto);
    }

    @Override
    public synchronized <IT> IT getValue(String name) {
        return context.<IT>getValue(name);
    }

    @Override
    public synchronized boolean containsVariableWithName(String name) {
        return context.containsVariableWithName(name);
    }

    @Override
    public synchronized boolean containsVariableWithName(IPrototype<?> proto) {
        return context.containsVariableWithName(proto);
    }

    @Override
    public synchronized boolean contains(IPrototype<?> proto) {
        return context.contains(proto);
    }
    
    @Override
    public synchronized void clean() {
        context.clean();
    }

    @Override
    public void visit(IVisitor<IVariable> visitor) throws InternalProcessingError, UserBadDataError {
        context.visit(visitor);
    }

    @Override
    public Iterator<IVariable> iterator() {
        return context.iterator();
    }
    
    
    
}
