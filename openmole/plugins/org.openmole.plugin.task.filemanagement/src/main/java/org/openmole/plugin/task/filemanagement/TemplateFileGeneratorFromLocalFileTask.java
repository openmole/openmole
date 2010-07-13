/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.plugin.task.filemanagement;

import java.io.File;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.aspect.caching.ChangeState;
import org.openmole.core.model.job.IContext;

/**
 *
 * @author reuillon
 */
public class TemplateFileGeneratorFromLocalFileTask extends TemplateFileGeneratorTask {

    final File template;

    public TemplateFileGeneratorFromLocalFileTask(String name, File template) throws UserBadDataError, InternalProcessingError {
        super(name);
        this.template = template;
    }

    @Override
    File getFile(IContext context) {
        return template;
    }

}
