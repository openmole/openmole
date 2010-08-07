package org.simexplorer.ui.ide.workflow.model;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import org.openmole.commons.exception.InternalProcessingError;
import org.openmole.commons.exception.UserBadDataError;
import org.openmole.core.implementation.task.GenericTask;
import org.openmole.core.model.execution.IProgress;
import org.openmole.core.model.job.IContext;


public class TasksList extends GenericTask implements Iterable<GenericTask> {

    private List<GenericTask> children;

    public TasksList(String name) throws UserBadDataError, InternalProcessingError {
        super(name);
        children = new LinkedList<GenericTask>();
    }

    @Override
    public Iterator<GenericTask> iterator() {
        return children.iterator();
    }
    
    public int size() {
        return children.size();
    }

    public GenericTask remove(int i) {
        return children.remove(i);
    }

    public boolean remove(GenericTask o) {
        return children.remove(o);
    }

    public boolean add(GenericTask e) {
        return children.add(e);
    }

    public int indexOf(Object o) {
        return children.indexOf(o);
    }

    public List<GenericTask> getChildren() {
        return children;
    }

    public void add(int i, GenericTask e) {
        children.add(i, e);
    }

    @Override
    protected void process(IContext ic, IContext ic1, IProgress ip) throws UserBadDataError, InternalProcessingError, InterruptedException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
