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
package org.openmole.plugin.task.filemanagement;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import org.openmole.core.implementation.data.Data;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.aspect.caching.ChangeState;
import org.openmole.core.implementation.task.Task;

import org.openmole.core.model.data.IData;
import org.openmole.core.model.data.IPrototype;
import org.openmole.core.model.execution.IProgress;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.task.annotations.Output;
import org.openmole.plugin.task.filemanagement.internal.Activator;

import static org.openmole.core.implementation.tools.VariableExpansion.*;

public abstract class TemplateFileGeneratorTask extends Task {

    @Output
    final IData<File> output;
     
    public TemplateFileGeneratorTask(String name, IPrototype<File> outputPrototype) throws UserBadDataError, InternalProcessingError {
        super(name);
        this.output = new Data(outputPrototype);
    }

    @Override
    protected void process(IContext context, IProgress progress) throws InternalProcessingError, UserBadDataError {
        try {

            File templateFile = getFile(context);
            File outputFile = Activator.getWorkspace().newFile("output", "template");

            BufferedReader reader = new BufferedReader(new FileReader(templateFile));
            try {
                PrintWriter writer = new PrintWriter(outputFile);
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        line = expandData(context, line);
                        writer.println(line);
                    }
                } finally {
                    writer.close();
                }
            } finally {
                reader.close();
            }

            context.setValue(output.getPrototype(), outputFile);
        } catch (IOException ex) {
            throw new UserBadDataError(ex);
        }
    }

    abstract File getFile(IContext context);
}
