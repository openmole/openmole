
package org.openmole.ui.ide.workflow.implementation;

import java.util.ArrayList;
import java.util.Collection;
import org.openide.util.lookup.ServiceProvider;
import org.openmole.ui.ide.control.TableType;
import org.openmole.ui.ide.control.TableType.Name;
import org.openmole.core.model.task.IGenericTask;
import org.openmole.plugin.task.groovy.GroovyTask;

/**
 *
 * @author mathieu
 */
public class GroovyTaskModelUI <T extends IGenericTask> extends GenericTaskModelUI<T>{

    public GroovyTaskModelUI(Properties...properties){
        super(properties);
    }
    
    @Override
    public void updateData() {
       // throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Collection<Name> getFields() {
       // setFields();
        return fields;
    }

   /* @Override
    public void setFields() {
        if (fields == null) {
            fields = new ArrayList<TableType.Name>();
            fields.add(Name.INPUT_PARAMETER);
            fields.add(Name.OUTPUT_PARAMETER);
        }
    }*/
}
