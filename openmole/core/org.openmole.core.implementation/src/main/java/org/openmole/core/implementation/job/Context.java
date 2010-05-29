/*
 *
 *  Copyright (c) 2007, 2008, Cemagref
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU General Public License as
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
import java.util.logging.Logger;


import org.openmole.core.workflow.model.job.IContext;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.tools.pattern.IVisitable;
import org.openmole.commons.tools.pattern.IVisitor;
import org.openmole.core.workflow.model.data.IVariable;
import org.openmole.core.workflow.model.data.IPrototype;

public class Context implements IVisitable<IVariable>, IContext {

    Map<String, IVariable> variables;
    IContext root;

    public Context() {
        variables = Collections.synchronizedMap(new TreeMap<String, IVariable>());
        setRoot(this);
    }

    public Context(IContext root) {
        variables = Collections.synchronizedMap(new TreeMap<String, IVariable>());
        setRoot(root);
        //parent.addChild(name, this, ticket);
    }

    @Override
    protected void finalize() throws Throwable {
       super.finalize();
    }

    @Override
    public void chRoot() {
        setRoot(this);
    }

    @Override
    public void setRoot(IContext root) {
        this.root = root;
    }

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#getRoot()
     */
    @Override
    public IContext getRoot() {
        return root;
    }


    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#setVariables(java.util.ArrayList)
     */
    @Override
    public void setVariables(List<IVariable<?>> vars) {
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
        synchronized (getVariables()) {
            getVariables().put(variable.getPrototype().getName(), variable);
        }
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
     * @see org.openmole.core.task.IContext#putGlobalVariable(java.lang.String, java.lang.Object)
     */
    @Override
    public void putGlobalVariable(String name, Object value) {
        getRoot().putVariable(new Variable<Object>(name, value));
    }

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#putVariable(org.openmole.core.data.structure.Prototype, java.lang.Object)
     */
    @Override
    public <T> void putVariable(IPrototype<? super T> proto, T value) {
        putVariable(new Variable<T>(proto, value));
    }

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#putGlobalVariable(org.openmole.core.data.structure.Prototype, java.lang.Object)
     */
    @Override
    public <T> void putGlobalVariable(IPrototype<? super T> proto, T value) {
        getRoot().putVariable(new Variable<T>(proto, value));
    }

    //	@Override
	/* (non-Javadoc)
     * @see org.openmole.core.task.IContext#getLocalValue(java.lang.String)
     */
    @Override
    public <IT> IT getLocalValue(String name) {
        synchronized (getVariables()) {
            Variable<IT> elt = getLocalVariable(name);

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
    public <IT> IT getLocalValue(IPrototype<IT> proto) {
        return this.<IT>getLocalValue(proto.getName());
    }

    //	@Override
	/* (non-Javadoc)
     * @see org.openmole.core.task.IContext#getLocalVariable(java.lang.String)
     */
    @Override
    public <IT> Variable<IT> getLocalVariable(String name) {
        return (Variable<IT>) getVariables().get(name);
    }

    @Override
    public <IT> Variable<IT> getLocalVariable(IPrototype<IT> proto) {
        return (Variable<IT>) getVariables().get(proto.getName());
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
     * @see org.openmole.core.task.IContext#getGlobal(java.lang.String)
     */
    @Override
    public IVariable<?> getGlobalVariable(String name) {
        return getRoot().getVariable(name);
    }

    @Override
    public void removeGlobalVariable(String name) {
        getRoot().removeVariable(name);
    }

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#getGlobalValue(java.lang.String)
     */
    @Override
    public <IT> IT getGlobalValue(String name) {
        IVariable<IT> var = getRoot().getVariable(name);

        if (var == null) {
            return null;
        }

        return (IT) var.getValue();
    }

    @Override
    public <IT> IT getGlobalValue(IPrototype<? extends IT> proto) {
        return (IT) getRoot().getVariable(proto).getValue();
    }

    @Override
    public boolean existGlobal(IPrototype<?> proto) {
        return getRoot().contains(proto);
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
            variables = new TreeMap<String, IVariable>();
        }
        //	cleanCache();
    }

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#getParent()
     */
    /*public IContext getParent() {
    return parent;
    }*/

    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#visit(org.openmole.commons.tools.IVisitor)
     */
    @Override
    public void visit(IVisitor<IVariable> visitor) throws InternalProcessingError, UserBadDataError {
        for (IVariable<?> v : getVariables().values()) {
            visitor.action(v);
        }
    }

    /*public Set<String> getChildNames() {
    return childs.keySet();
    }*/
    /* (non-Javadoc)
     * @see org.openmole.core.task.IContext#getVariable(java.lang.String)
     */
    @Override
    public <T> Variable<T> getVariable(String name) {
        return (Variable<T>) getVariables().get(name);
    }

    /*@Override
    public Collection<IChildContext> getChlids() {
    return childs.values();
    }*/
    @Override
    public Iterator<IVariable> iterator() {
        return variables.values().iterator();
    }

    @Override
    public boolean isRoot() {
        return this.equals(getRoot());
    }


}
