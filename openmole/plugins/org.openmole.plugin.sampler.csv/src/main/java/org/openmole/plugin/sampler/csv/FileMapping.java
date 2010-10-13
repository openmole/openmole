/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.plugin.sampler.csv;

import java.io.File;
import org.openmole.commons.exception.UserBadDataError;

/**
 *
 * @author Mathieu Leclaire <mathieu.leclaire@iscpif.fr>
 */
public class FileMapping implements IStringMapping<File>{
    private File basePath;

    public FileMapping(File basePath) {
        this.basePath = basePath;
    }

    @Override
    public File convert(String stringToBeConverted) throws UserBadDataError {    
        File fi = new File(basePath, stringToBeConverted);
       /* if (!fi.exists()){
               throw new UserBadDataError("The path " + fi.getAbsolutePath() + " does not exist.");
        }*/
        return fi;
    }

}
