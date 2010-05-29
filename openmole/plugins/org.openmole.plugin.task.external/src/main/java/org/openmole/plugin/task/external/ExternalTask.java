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
package org.openmole.plugin.task.external;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.workflow.implementation.resource.FileSetResource;
import org.openmole.core.workflow.implementation.task.Task;
import org.openmole.core.workflow.model.data.IPrototype;
import org.openmole.core.workflow.model.execution.IProgress;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.commons.tools.io.FastCopy;
import org.openmole.commons.tools.structure.Duo;
import org.openmole.core.workflow.model.task.annotations.Resource;
import org.openmole.commons.tools.io.IFileOperation;

import static org.openmole.core.workflow.implementation.tools.VariableExpansion.*;

public abstract class ExternalTask extends Task {

    @Resource
    FileSetResource inFiles = new FileSetResource();

    List<Duo<IPrototype<File>, String>> inContextFiles = new LinkedList<Duo<IPrototype<File>, String>>();
    List<Duo<IPrototype<List<File>>, IPrototype<List<String>>>> inContextFileList = new LinkedList<Duo<IPrototype<List<File>>, IPrototype<List<String>>>>();
    Map<File, String> inFileNames = new TreeMap<File, String>();

    List<Duo<IPrototype<File>, String>> outFileNames = new LinkedList<Duo<IPrototype<File>, String>>();
    List<Duo<IPrototype<File>, IPrototype<String>>> outFileNamesFromVar = new LinkedList<Duo<IPrototype<File>, IPrototype<String>>>();
    Map<IPrototype<File>, IPrototype<String>> outFileNamesVar = new HashMap<IPrototype<File>, IPrototype<String>>();

    public ExternalTask(String name) throws UserBadDataError,
            InternalProcessingError {
        super(name);
    }

    protected void prepareInputFiles(IContext context, IProgress progress, File tmpDir) throws InternalProcessingError, UserBadDataError {
        try {
            for (Map.Entry<File, String> entry : inFileNames.entrySet()) {
                File localFile = inFiles.getDeployed(entry.getKey());
                File correctName = new File(tmpDir, expandData(context, entry.getValue()));
                copy(localFile, correctName);
            }

            for (Duo<IPrototype<File>, String> p : inContextFiles) {
                File f = context.getLocalValue(p.getLeft());

                if (f == null) {
                    throw new UserBadDataError("File supposed to be present in variable \"" + p.getLeft().getName() + "\" at the beging of the task \"" + getName() + "\" and is not.");
                }

                File correctName = new File(tmpDir, expandData(context, p.getRight()));
                copy(f, correctName);
            }

            for (Duo<IPrototype<List<File>>, IPrototype<List<String>>> p : inContextFileList) {
                List<File> lstFile = context.getLocalValue(p.getLeft());
                List<String> lstName = context.getLocalValue(p.getRight());

                if (lstFile != null && lstName != null) {
                    Iterator<File> fIt = lstFile.iterator();
                    Iterator<String> sIt = lstName.iterator();

                    while (fIt.hasNext() && sIt.hasNext()) {
                        File f = fIt.next();
                        String name = sIt.next();

                        File fo = new File(tmpDir, expandData(context, name));
                        fo.deleteOnExit();

                        copy(f, fo);
                    }
                }
            }
        } catch (IOException e) {
            throw new InternalProcessingError(e);
        }
    }

    private void copy(File from, File to) throws IOException {
        FastCopy.copy(from, to);

        FastCopy.applyRecursive(to, new IFileOperation() {

            @Override
            public void execute(File file)  {
                if (file.isFile()) {
                    file.setExecutable(true);
                }
                file.deleteOnExit();
            }
        });
    }

    private void setDeleteOnExit(File file) {
        FastCopy.applyRecursive(file, new IFileOperation() {

            @Override
            public void execute(File file) {
                file.deleteOnExit();
            }
        });
    }

    protected void fetchOutputFiles(IContext context, IProgress progress, File tmpDir) throws InternalProcessingError, UserBadDataError {
        for (Duo<IPrototype<File>, String> p : outFileNames) {
            String filename = expandData(context, p.getRight());
            File fo = new File(tmpDir, filename);

            if (!fo.exists()) {
                throw new UserBadDataError("Output file " + fo.getAbsolutePath() + " for task " + getName() + " doesn't exist");
            }

            setDeleteOnExit(fo);
            context.putVariable(p.getLeft(), fo);
            if (outFileNamesVar.containsKey(p.getLeft())) {
                context.putVariable(outFileNamesVar.get(p.getLeft()), filename);
            }
        }

        for (Duo<IPrototype<File>, IPrototype<String>> p : outFileNamesFromVar) {
            if (!context.contains(p.getRight())) {
                throw new UserBadDataError("Variable containing the output file name should exist in the context at the end of the task" + getName());
            }

            File fo = new File(tmpDir, context.getLocalValue(p.getRight()));
            if (!fo.exists()) {
                throw new UserBadDataError("Output file " + fo.getAbsolutePath() + " for task " + getName() + " doesn't exist");
            }

            setDeleteOnExit(fo);
            context.putVariable(p.getLeft(), fo);
        }

    }

    public void exportFilesFromContextAs(IPrototype<List<File>> fileList, IPrototype<List<String>> names) {
        inContextFileList.add(new Duo<IPrototype<List<File>>, IPrototype<List<String>>>(fileList, names));
        super.addInput(fileList);
        super.addInput(names);
    }

    public void exportFileFromContextAs(IPrototype<File> fileProt, String name) {
        inContextFiles.add(new Duo<IPrototype<File>, String>(fileProt, name));
        super.addInput(fileProt);
    }

    public void importFileInContext(IPrototype<File> var, String fileName) {
        outFileNames.add(new Duo<IPrototype<File>, String>(var, fileName));
        addOutput(var);
    }

    public void importFileAndFileNameInContext(IPrototype<File> var, IPrototype<String> varFileName, String fileName) {
        importFileInContext(var, fileName);
        addOutput(varFileName);
        outFileNamesVar.put(var, varFileName);
    }

    public void importFileInContext(IPrototype<File> var, IPrototype<String> varFileName) {
        addOutput(var);
        outFileNamesFromVar.add(new Duo<IPrototype<File>, IPrototype<String>>(var, varFileName));
    }

    public void addInFile(File file, String name) {
        inFiles.addFile(file);
        inFileNames.put(file, name);
    }

    public void addInFile(File file) {
        addInFile(file, file.getName());
    }
    public void addInFile(String location) {
        addInFile(new File(location));
    }
}
