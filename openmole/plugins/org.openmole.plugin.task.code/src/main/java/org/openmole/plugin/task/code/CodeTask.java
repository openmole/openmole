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
package org.openmole.plugin.task.code;

import org.openmole.plugin.tools.code.StringSourceCode;
import org.openmole.plugin.tools.code.ISourceCode;
import org.openmole.plugin.tools.code.IContextToCode;
import org.openmole.plugin.tools.code.FileSourceCode;
import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.model.execution.IProgress;
import org.openmole.core.model.job.IContext;
import org.openmole.commons.aspect.caching.ChangeState;
import org.openmole.plugin.task.code.internal.Activator;
import org.openmole.plugin.task.external.ExternalSystemTask;

public abstract class CodeTask<T extends IContextToCode> extends ExternalSystemTask {

    private T contextToCode;
    
    private Set<File> libs = new TreeSet<File>();

    public CodeTask(String name, T contextToCode) throws UserBadDataError, InternalProcessingError {
        super(name);
        this.contextToCode = contextToCode;
    }

    @Override
    protected void process(IContext global, IContext context, IProgress progress) throws UserBadDataError, InternalProcessingError {
        try {
            File pwd = Activator.getWorkspace().newDir();
            prepareInputFiles(global, context, progress, pwd.getCanonicalFile());

            contextToCode.execute(global, context, progress, getOutput(), libs);
            
            fetchOutputFiles(global, context, progress, pwd.getCanonicalFile());
        } catch (IOException e) {
            throw new InternalProcessingError(e);
        }
    }

    protected T getContextToCode() {
        return contextToCode;
    }

    public void setCode(ISourceCode code) throws UserBadDataError, InternalProcessingError {
        contextToCode.setCode(code);
    }

    public void setCode(String code) throws UserBadDataError, InternalProcessingError {
        setCode(new StringSourceCode(code));
    }

    public void setCodeFile(String url) throws UserBadDataError, InternalProcessingError {
        setCode(new FileSourceCode(url));

    }

    public String getCode() throws UserBadDataError, InternalProcessingError {
        return contextToCode.getCode();
    }

    @ChangeState
    public synchronized void addLib(File lib) {
        libs.add(lib);
    }

    public void addLib(String lib) {
        addLib(new File(lib));
    }

}
