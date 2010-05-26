/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.plugin.task.filemanagement;

import java.io.File;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.commons.aspect.caching.ChangeState;
import org.openmole.core.workflow.implementation.resource.FileResource;
import org.openmole.core.workflow.model.job.IContext;
import org.openmole.core.workflow.model.task.annotations.Resource;

/**
 *
 * @author reuillon
 */
public class TemplateFileGeneratorFromLocalFileTask extends TemplateFileGeneratorTask {

    @Resource
    FileResource fileResource;


    public TemplateFileGeneratorFromLocalFileTask(String name) throws UserBadDataError, InternalProcessingError {
        super(name);
    }

    @ChangeState
    public synchronized void setFile(File template) {
        fileResource = new FileResource(template);
    }

    @Override
    File getFile(IContext context) {
        return fileResource.getDeployedFile();
    }

}
