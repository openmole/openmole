/*
 *  Copyright (C) 2010 Romain Reuillon <romain.reuillon at openmole.org>
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

package org.openmole.plugin.domain.groovy;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.openmole.core.implementation.data.Variable;
import org.openmole.core.workflow.model.data.IVariable;
import org.openmole.core.implementation.data.Prototype;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.domain.IDomain;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.plugin.tools.code.FileSourceCode;
import org.openmole.plugin.tools.code.ISourceCode;
import org.openmole.plugin.tools.code.StringSourceCode;
import org.openmole.plugin.tools.groovy.ContextToGroovyCode;

/**
 *
 * @author Romain Reuillon <romain.reuillon at openmole.org>
 */
public class GroovyDomainModifier<T> implements IDomain<T> {

    final IDomain<T> domain;
    final private Prototype<T> prototype;
    final ContextToGroovyCode contextToGroovyCode = new ContextToGroovyCode();

    public GroovyDomainModifier(IDomain<T> domain, Prototype<T> prototype) {
        this.domain = domain;
        this.prototype = prototype;
    }

    public void setCodeFile(String file) throws UserBadDataError, InternalProcessingError {
        setCode(new FileSourceCode(file));
    }

    public void setCodeFile(File file) throws UserBadDataError, InternalProcessingError {
        setCode(new FileSourceCode(file));
    }

    public void setCode(String code) throws UserBadDataError, InternalProcessingError {
        setCode(new StringSourceCode(code));
    }

    public void setCode(ISourceCode codeSource) throws InternalProcessingError, UserBadDataError {
        contextToGroovyCode.setCode(codeSource);
    }

    public void addImport(String pack) {
        contextToGroovyCode.addImport(pack);
    }

    @Override
    public Iterator<? extends T> iterator(final IContext context) throws UserBadDataError, InternalProcessingError {
        final Iterator<? extends T> testIt = domain.iterator(context);
        if(testIt.hasNext()) {
            List<IVariable> vars = new ArrayList<IVariable>(1);
            vars.add(new Variable<T>(prototype, testIt.next()));
            T testRes = (T) contextToGroovyCode.execute(context, vars);
        }

        return new Iterator<T>() {

            final Iterator<? extends T> iterator = domain.iterator(context);

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                try {
                    T next = iterator.next();
                    List<IVariable> vars = new ArrayList<IVariable>(1);
                    vars.add(new Variable<T>(prototype, next));
                    return (T) contextToGroovyCode.execute(context, vars);
                } catch (UserBadDataError ex) {
                    throw new NoSuchElementException(ex.getLocalizedMessage());
                } catch (InternalProcessingError ex) {
                    throw new NoSuchElementException(ex.getLocalizedMessage());
                }
            }

            @Override
            public void remove() {
                iterator.remove();
            }

        };
    }

}
