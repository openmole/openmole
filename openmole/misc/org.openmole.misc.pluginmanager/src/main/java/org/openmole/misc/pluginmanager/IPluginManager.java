/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.misc.pluginmanager;

import java.io.File;
import java.io.FileFilter;
import java.util.Collection;
import org.osgi.framework.Bundle;
import org.openmole.commons.exception.InternalProcessingError;

/**
 *
 * @author reuillon
 */
public interface IPluginManager {
    Bundle load(File path) throws InternalProcessingError;
    Bundle load(String path) throws InternalProcessingError;
    Collection<Bundle> loadDir(String path) throws InternalProcessingError;
    Collection<Bundle> loadDir(File path) throws InternalProcessingError;
    Collection<Bundle> loadDir(File path, String pattern) throws InternalProcessingError;
    Collection<Bundle> loadDir(File path, FileFilter filter) throws InternalProcessingError;

    Bundle getBundle(File path) throws InternalProcessingError;

    void unload(File path) throws InternalProcessingError;
    void unload(String path) throws InternalProcessingError;

    boolean isClassProvidedByAPlugin(Class c);
    File getPluginForClass(Class c);
    Iterable<File> getPluginAndDependanciesForClass(Class c);
}
