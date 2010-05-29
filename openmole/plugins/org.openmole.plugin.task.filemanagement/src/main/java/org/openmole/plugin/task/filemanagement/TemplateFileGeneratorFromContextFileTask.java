/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.plugin.task.filemanagement;

import java.io.File;
import org.openmole.core.implementation.data.Data;
import org.openmole.core.model.data.IData;
import org.openmole.core.model.data.IPrototype;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.aspect.caching.ChangeState;
import org.openmole.core.model.job.IContext;
import org.openmole.core.model.task.annotations.Input;

/**
 *
 * @author reuillon
 */
public class TemplateFileGeneratorFromContextFileTask extends TemplateFileGeneratorTask {

    @Input
    IData<File> templateFile;

    public TemplateFileGeneratorFromContextFileTask(String name) throws UserBadDataError, InternalProcessingError {
        super(name);
    }

    @ChangeState
    public void setTemplateFile(IPrototype<File> templateFile) {
        this.templateFile = new Data<File>(templateFile);
    }

    @Override
    File getFile(IContext context) {
        return context.getLocalValue(templateFile.getPrototype());
    }

}
