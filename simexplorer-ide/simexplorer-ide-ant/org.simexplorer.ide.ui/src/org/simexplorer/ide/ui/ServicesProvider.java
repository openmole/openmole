/*
 *  Copyright © 2008, Cemagref
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
package org.simexplorer.ide.ui;

import org.openmole.core.implementation.task.Task;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.simexplorer.ui.ide.workflow.model.ExplorationApplication;
import org.openmole.plugin.task.groovy.GroovyTask;
import org.openmole.core.workflow.methods.task.JavaTask;
import org.openmole.plugin.task.filemanagment.TemplateFileGeneratorFromLocalFileTask;
import org.openmole.plugin.task.systemexec.SystemExecTask;

public class ServicesProvider {

    private List<Class<? extends Task>> processorImplementations;
    private Map<String, List<Class<? extends Task>>> processorsMap;
    private static ServicesProvider instance;

    public ServicesProvider() {
        processorImplementations = new ArrayList<Class<? extends Task>>(Arrays.asList(GroovyTask.class, TemplateFileGeneratorFromLocalFileTask.class, SystemExecTask.class, JavaTask.class));
        processorsMap = new HashMap<String, List<Class<? extends Task>>>();
        addInMap(processorsMap, ExplorationApplication.LABEL_INPUT_GENERATION, TemplateFileGeneratorFromLocalFileTask.class, GroovyTask.class);
        addInMap(processorsMap, ExplorationApplication.LABEL_MODEL_LAUNCHER, SystemExecTask.class, JavaTask.class, GroovyTask.class);
        addInMap(processorsMap, ExplorationApplication.LABEL_OUTPUT_PROCESSING, GroovyTask.class);
        addInMap(processorsMap, ExplorationApplication.LABEL_FINAL_OUTPUT_PROCESSING, GroovyTask.class);
        addInMap(processorsMap, "Init", GroovyTask.class);
    }

    public static ServicesProvider getInstance() {
        if (instance == null) {
            instance = new ServicesProvider();
        }
        return instance;
    }

    // TODO move this in tools package
    private <T, U> void addInMap(Map<T, List<Class<? extends U>>> map, T key, Class<? extends U>... values) {
        List<Class<? extends U>> mapResult = map.get(key);
        if (mapResult == null) {
            mapResult = new ArrayList<Class<? extends U>>(values.length);
            map.put(key, mapResult);
        }
        for (Class<? extends U> value : values) {
            mapResult.add(value);
        }
    }

    public static List<Class<? extends Task>> getProcessors(String processorName) {
        List<Class<? extends Task>> result = getInstance().processorsMap.get(processorName);
        if (result == null) {
            return getInstance().processorImplementations;
        } else {
            return result;
        }
    }
}
