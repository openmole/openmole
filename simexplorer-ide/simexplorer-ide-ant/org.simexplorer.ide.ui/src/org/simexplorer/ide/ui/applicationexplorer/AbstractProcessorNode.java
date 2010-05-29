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
package org.simexplorer.ide.ui.applicationexplorer;

import java.lang.reflect.InvocationTargetException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.Lookup;
import org.openmole.core.implementation.task.GenericTask;
import org.simexplorer.core.workflow.model.metada.Metadata;
import org.simexplorer.ide.ui.dataexplorer.MetadataProperties;
import org.simexplorer.core.workflow.model.metada.MetadataLoader;
import org.simexplorer.ui.ide.workflow.model.MetadataProxy;

public abstract class AbstractProcessorNode<T extends GenericTask> extends AbstractNode {

    private final static String PROP_PROCESSOR_NAME = "name";
    private final static String PROP_NODE_TYPE = "nodeType";
    protected T processor;

    public AbstractProcessorNode(T processor, Children children, Lookup lookup) {
        super(children, lookup);
        this.processor = processor;
    }

    @Override
    protected Sheet createSheet() {
        Sheet s = super.createSheet();
        Sheet.Set ss = s.get(Sheet.PROPERTIES);
        if (ss == null) {
            ss = Sheet.createPropertiesSet();
            s.put(ss);
        }
        ss.put(new NameProperty());
        ss.put(new NodeTypeProperty());
        ss.put(new RunProperty<T>(processor));

        // set the metadata properties in a new property items
        MetadataProperties metadataProperties = new MetadataProperties(MetadataProxy.getMetadata(processor));
        s.put(metadataProperties.getProperties());
        return s;
    }

    public static class RunProperty<T extends GenericTask> extends PropertySupport.ReadWrite<Boolean> {

        private T task;
        private static String BYPASS_KEY = "BYPASS";

        public RunProperty() {
            super("Check", Boolean.class, "Run", "Select for run");
        }

        public RunProperty(T processor) {
            this();
            this.task = processor;
        }

        @Override
        public Boolean getValue() throws IllegalAccessException, InvocationTargetException {
            String result = MetadataProxy.getMetadata(task, PROP_NAME);
            if (result == null) {
                return false;
            } else {
                return Boolean.parseBoolean(result);
            }
        }

        @Override
        public void setValue(Boolean arg0) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            MetadataProxy.setMetadata(task, BYPASS_KEY, Boolean.toString(arg0.booleanValue()));
        }
    }

    public class NameProperty extends PropertySupport.ReadOnly<String> {

        public NameProperty() {
            super(PROP_PROCESSOR_NAME, String.class, "Name", "Name of the processor");
        }

        @Override
        public String getValue() throws IllegalAccessException, InvocationTargetException {
            Metadata metadata = MetadataLoader.loadMetadata(processor);
            return metadata.get("name");
        }
    }

    public class NodeTypeProperty extends PropertySupport.ReadOnly<String> {

        public NodeTypeProperty() {
            super(PROP_NODE_TYPE, String.class, "Node Type", "Type of the node used to display this processor");
        }

        @Override
        public String getValue() throws IllegalAccessException, InvocationTargetException {
            return AbstractProcessorNode.this.getClass().getSimpleName();
        }
    }
}
