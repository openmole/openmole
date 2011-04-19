/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.openmole.ide.plugin.task.groovy;

import org.openide.util.Task;
import org.openmole.ide.core.workflow.model.IEntityUI;
import org.openide.util.lookup.ServiceProvider;

/**
 *
 * @author mathieu
 */
@ServiceProvider(service = IEntityUI.class,
position = 10)
public class GroovyTaskModelUIProxy implements IEntityUI {
    GroovyTaskModelUI task;

    public GroovyTaskModelUIProxy() {
        task = new GroovyTaskModelUI();
    }

    public String name() {
        return task.name();
    }

    public Class entityType() {
        return task.entityType();
    }
}
