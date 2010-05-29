/*
 *
 *  Copyright (c) 2008, Cemagref
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
package org.openmole.core.implementation.data;


import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.core.workflow.model.data.IVariable;

/**
 * This class handle a multi-purpose variable. A variable has a name, a type and
 * a value.
 * @param T
 * @see org.openmole.core.workflow.model.data.DataContainer
 */
public class Variable<T> implements IVariable<T> {

	private IPrototype<? super T> prototype;
	protected T value;

	private Variable() {
	}

	public Variable(String name,  T value) {
		this(new Prototype<T>(name, (Class<T>)value.getClass()), value);
	}
	
	public Variable(String name,  Class<? extends T> type, T value) {
		this(new Prototype<T>(name, type), value);
	}

	public Variable(String name, Class<? extends T> type) {
		this(new Prototype<T>(name, type));
	}

	public Variable(IPrototype<T> prototype) {
		this.prototype = prototype;
	}


	public Variable(IPrototype<? super T> prototype, T value) {
		this.prototype = prototype;
		setValue(value);
	}



	public Object readResolve() {
		if (getValue() == null) {
			if (getType() == Integer.class) {
				value = (T) new Integer(0);
			} else {
				value = (T) new Double(0.0);
			}
		}
		return this;
	}

        @Override
	public void setValue(T value) {
		//if (getType().isAssignableFrom(value.getClass())) {
		this.value = value;
		/*} else {
			throw new AssigmentException("Cannot assign " + getType() + " from " + value.getClass());
		}*/
	}

        @Override
	public IPrototype<? super T> getPrototype() {
		return prototype;
	}

	/*public void setPrototype(Prototype<? super T> structure) {
		this.prototype = structure;
	}*/

	public String getName() {
	//	return /*namespace.child(getPrototype().getName()).toString()*/;
		return getPrototype().getName();
	}

	public Class getType() {
		return getPrototype().getType();
	}

	public T getValue() {
		return value;
	}

	@Override
	public String toString() {
		return prototype.getName() + '=' + value.toString();
	}

	
	
}
