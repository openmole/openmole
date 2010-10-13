/*
 *
 *  Copyright (c) 2007, 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation; either version 3 of
 *  the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public
 *  License along with this program; if not, write to the Free
 *  Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston,
 *  MA  02110-1301  USA
 */
package org.openmole.core.implementation.job;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.core.implementation.data.Variable;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


import org.openmole.core.model.job.IContext;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.pattern.IVisitable;
import org.openmole.commons.tools.pattern.IVisitor;
import org.openmole.core.model.data.IVariable;
import org.openmole.core.model.data.IPrototype;

public class Context implements IVisitable<IVariable>, IContext {

    final Map<String, IVariable> variables;
 
    public Context() {
        variables = new TreeMap<String, IVariable>();
    }

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#setVariables(java.util.ArrayList)
     */
    @Override
    public void putVariables(Iterable<? extends IVariable> vars) {
        for (IVariable<?> variable : vars) {
            putVariable(variable);
        }
    }

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#getVariables()
     */
    @Override
    public Map<String, IVariable> getVariables() {
        return variables;
    }

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#getVariable(org.openmole.core.data.structure.Prototype)
     */
    @Override
    public <T> IVariable<? extends T> getVariable(IPrototype<T> proto) {
        return (IVariable<T>) getVariables().get(proto.getName());
    }

    /* @Override*/
    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#putVariable(org.openmole.core.data.Variable)
     */
    @Override
    public void putVariable(IVariable<?> variable) {
        getVariables().put(variable.getPrototype().getName(), variable);
    }

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#putVariable(java.lang.String, java.lang.Object)
     */
    @Override
    public void putVariable(String name, Object value) {
        putVariable(new Variable<Object>(name, value));
    }

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#putVariable(java.lang.String, java.lang.Class, java.lang.Object)
     */
    @Override
    public <T> void putVariable(String name, Class<? super T> type, T value) {
        putVariable(new Variable<Object>(name, type, value));
    }
    

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#putVariable(org.openmole.core.data.structure.Prototype, java.lang.Object)
     */
    @Override
    public <T> void putVariable(IPrototype<? super T> proto, T value) {
        putVariable(new Variable<T>(proto, value));
    }

    //	@Override
	/* (non-Javadoc)
     * @see org.openmole.core.task.IContext#getLocalValue(java.lang.String)
     */
    @Override
    public <IT> IT getValue(String name) {
        synchronized (getVariables()) {
            Variable<IT> elt = getVariable(name);

            if (elt != null) {
                return elt.getValue();
            } else {
                return null;
            }
        }
    }

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#getLocalValue(org.openmole.core.data.structure.Prototype)
     */
    @Override
    public <IT> IT getValue(IPrototype<IT> proto) {
        return this.<IT>getValue(proto.getName());
    }

    //	@Override
	/* (non-Javadoc)
     * @see org.openmole.core.task.IContext#getLocalVariable(java.lang.String)
     */
    @Override
    public <IT> Variable<IT> getVariable(String name) {
        return (Variable<IT>) getVariables().get(name);
    }

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#removeVariable(java.lang.String)
     */
    @Override
    public void removeVariable(String name) {
        getVariables().remove(name);
    }

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#exist(java.lang.String)
     */
    @Override
    public boolean containsVariableWithName(String name) {
        return getVariables().containsKey(name);
    }

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#exist(org.openmole.core.data.structure.Prototype)
     */
    @Override
    public boolean containsVariableWithName(IPrototype<?> proto) {
        return getVariables().containsKey(proto.getName());
    }


    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#exist(org.openmole.core.data.structure.Prototype)
     */
    @Override
    public boolean contains(IPrototype<?> proto) {
        Variable<?> v = getVariable(proto.getName());
        if (v == null) {
            return false;
        }
        return proto.getType().isAssignableFrom(v.getType());
    }
    
    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#setValue(java.lang.String, java.lang.Object)
     */
    @Override
    public void setValue(String name, Object value) {
        synchronized (getVariables()) {
            Variable var = getVariable(name);

            if (var != null) {
                var.setValue(value);
            } else {
                putVariable(name, value);
            }
        }
    }


    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#setValue(org.openmole.core.data.structure.Prototype, java.lang.Object)
     */
    @Override
    public <T> void setValue(IPrototype<? super T> proto, T value) {
        synchronized (getVariables()) {
            IVariable var = getVariable(proto);

            if (var != null) {
                var.setValue(value);
            } else {
                putVariable(proto, value);
            }

        }
    }

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#clean()
     */
    @Override
    public void clean() {
        synchronized (getVariables()) {
            variables.clear();
        }
    }

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#visit(org.openmole.commons.tools.IVisitor)
     */
    @Override
    public void visit(IVisitor<IVariable> visitor) throws InternalProcessingError, UserBadDataError {
        for (IVariable<?> v : getVariables().values()) {
            visitor.action(v);
        }
    }


    /*@Override
    public Collection<IChildContext> getChlids() {
    return childs.values();
    }*/
    @Override
    public Iterator<IVariable> iterator() {
        return variables.values().iterator();
    }

}
