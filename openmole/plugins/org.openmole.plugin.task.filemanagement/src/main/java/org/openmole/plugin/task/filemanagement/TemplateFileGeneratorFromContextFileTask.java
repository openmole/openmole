/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.plugin.task.filemanagement;

import java.io.File;
import org.openmole.core.model.data.IData;
import org.openmole.core.model.data.IPrototype;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.data.Data;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.task.annotations.Input;

/**
 *
 * @author reuillon
 */
public class TemplateFileGeneratorFromContextFileTask extends TemplateFileGeneratorTask {

    @Input
    final IData<File> templateFile;

    public TemplateFileGeneratorFromContextFileTask(String name, IPrototype<File> templateFile, IPrototype<File> outputPrototype) throws UserBadDataError, InternalProcessingError {
        super(name,outputPrototype);
        this.templateFile = new Data<File>(templateFile);
    }

    @Override
    File getFile(IContext context) {
        return context.getValue(templateFile.getPrototype());
    }

}
