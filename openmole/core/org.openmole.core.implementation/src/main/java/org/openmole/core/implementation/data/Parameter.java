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
package org.openmole.core.implementation.data;

import org.openmole.core.workflow.model.data.IParameter;
import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.core.workflow.model.data.IVariable;

/**
 *
 * @author reuillon
 */
public class Parameter<T> implements IParameter<T> {

    final IVariable<T> variable;
    boolean override;

    public Parameter(IPrototype<? super T> prototype, T value) {
        this(prototype, value, false);
    }

    public Parameter(IPrototype<? super T> prototype, T value, boolean override) {
        this(new Variable<T>(prototype, value), override);
    }

    public Parameter(IVariable<T> variable) {
        this(variable, false);
    }

    public Parameter(IVariable<T> variable, boolean override) {
        this.variable = variable;
        this.override = override;
    }

    @Override
    public IVariable<T> getVariable() {
        return variable;
    }

    @Override
    public boolean getOverride() {
        return override;
    }
}
