/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.console;

import org.openmole.misc.pluginmanager.IPluginManager;
import org.openmole.core.structuregenerator.IStructureGenerator;
import org.openmole.ui.console.internal.Activator;

/**
 *
 * @author reuillon
 */
public class Services {

        final IPluginManager plugin = Activator.getPluginManager();
        final IStructureGenerator structure = Activator.getStructureGenerator();
}
