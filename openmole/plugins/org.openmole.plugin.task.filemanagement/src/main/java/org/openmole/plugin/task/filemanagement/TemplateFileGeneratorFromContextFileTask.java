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
import org.openmole.core.model.data.IContext;


/**
 *
 * @author reuillon
 */
public class TemplateFileGeneratorFromContextFileTask extends TemplateFileGeneratorTask {

    final IData<File> templateFile;

    public TemplateFileGeneratorFromContextFileTask(String name, IPrototype<File> templateFile, IPrototype<File> outputPrototype) throws UserBadDataError, InternalProcessingError {
        super(name,outputPrototype);
        this.templateFile = new Data<File>(templateFile);
        addInput(templateFile);
    }

    @Override
    File getFile(IContext context) {
        return context.value(templateFile.prototype()).get();
    }

}
