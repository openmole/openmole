/*
 *  Copyright © 2008, 2009, Cemagref
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
package org.simexplorer.ide.ui.dataexplorer.variables;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.Collection;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.LookupEvent;
import org.openide.util.LookupListener;
import org.openide.util.NbBundle;
import org.openide.util.Utilities;
import org.openide.util.lookup.Lookups;
import org.simexplorer.ui.ide.workflow.model.MetadataProperties;
import org.simexplorer.ui.ide.workflow.model.ExplorationApplication;
import org.openmole.core.implementation.task.Task;
import org.openmole.core.model.data.IPrototype;
import org.simexplorer.ui.ide.workflow.model.MetadataProxy;

class VariableNode extends AbstractNode {

    private static final Image ICON_NONGATHERED = ImageUtilities.loadImage("org/simexplorer/ide/ui/dataexplorer/variables/noGathered.png");
    private static final Image ICON_NONGATHEREDDISABLE = ImageUtilities.loadImage("org/simexplorer/ide/ui/dataexplorer/variables/noGatheredDisable.png");
    private static final Image ICON_GATHERED = ImageUtilities.loadImage("org/simexplorer/ide/ui/dataexplorer/variables/gathered.png");
    private VariablesChildren variablesChildren;
    private final static String PROP_TYPE = "type";
    private IPrototype variable;
    private boolean isSystem;
    private boolean disable;
    private TypeProperty typeProperty;

    public VariableNode(IPrototype variable, VariablesChildren variablesChildren) {
        super(Children.LEAF, Lookups.singleton(variable));
        setDisplayName(variable.getName());
        this.variablesChildren = variablesChildren;
        this.variable = variable;
        this.isSystem = ExplorationApplication.isVariableSystem(variable);
        this.disable = false;
        Lookup.Template<Task> template = new Lookup.Template<Task>(Task.class);
        final Lookup.Result<Task> result = Utilities.actionsGlobalContext().lookup(template);
        result.addLookupListener(new LookupListener() {

            @Override
            public void resultChanged(LookupEvent e) {
                processorSelectionChanged(result);
            }
        });
        processorSelectionChanged(result);
    }

    @Override
    public Image getIcon(int type) {
        if (ExplorationApplication.isGatheredAfterExploration(variable)) {
            return ICON_GATHERED;
        } else {
            if (disable) {
                return ICON_NONGATHEREDDISABLE;
            } else {
                return ICON_NONGATHERED;
            }
        }
    }

    @Override
    public String getHtmlDisplayName() {
        StringBuilder displayName = new StringBuilder();
        if (disable) {
            displayName.append("<font color='AAAAAA'>");
        }
        if (isSystem) {
            displayName.append("<i>");
        }
        displayName.append(variable.getName());
        if (isSystem) {
            displayName.append("</i>");
        }
        if (disable) {
            displayName.append("</font>");
        }
        return displayName.toString();
    }

    private void processorSelectionChanged(Lookup.Result<Task> result) {
        if (typeProperty != null) {
            Collection<? extends Task> c = result.allInstances();
            if (!c.isEmpty()) {
                if (ExplorationApplication.isAfterExploration(c.iterator().next())) {
                    if (ExplorationApplication.isGatheredAfterExploration(variable)) {
                        // we adjust the type displayed to show if it is an array
                        typeProperty.setDisplayedType("[] "
                                + typeProperty.getDisplayableType());
                        disable = false;
                    } else {
                        // the node will be disabled
                        disable = true;
                    }
                } else {
                    typeProperty.setDisplayedType(typeProperty.getDisplayableType());
                    disable = false;
                }
                this.firePropertyChange(PROP_TYPE, null, null);
                this.fireDisplayNameChange(null, null);
                this.fireIconChange();
            }
        }
    }

    @Override
    public Action[] getActions(boolean arg0) {
        if (ExplorationApplication.isVariableSystem(variable)) {
            return new Action[]{new EditAction()};
        } else {
            return new Action[]{new EditAction(), new DeleteAction()};
        }
    }

    private class DeleteAction extends AbstractAction {

        public DeleteAction() {
            putValue(NAME, "Remove");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            variablesChildren.removeVariable(variable);
        }
    }

    private class EditAction extends AbstractAction {

        private EditVariableAction action;

        public EditAction() {
            putValue(NAME, NbBundle.getMessage(EditVariableAction.class, "CTL_EditVariableAction"));
            action = new EditVariableAction();
            // TODO this should be the best way, but doesn't work
            //wizardAction = (EditVariableAction) Lookups.forPath("Actions/Exploration").lookup(EditVariableAction.class);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            action.performAction(variable);
        }
    }

    @Override
    protected Sheet createSheet() {
        Sheet sheet = super.createSheet();
        Sheet.Set ss = sheet.get(Sheet.PROPERTIES);
        if (ss == null) {
            ss = Sheet.createPropertiesSet();
            sheet.put(ss);
        }
        // put type
        typeProperty = new TypeProperty(variable);
        ss.put(typeProperty);


        // set the metadata properties in a new propertiy tab
        MetadataProperties metadataProperties = new MetadataProperties(MetadataProxy.getMetadata(variable));
        sheet.put(metadataProperties.getProperties());
        return sheet;
    }

    public static class TypeProperty extends PropertySupport.ReadOnly<String> {

        private IPrototype variable;
        private String displayableType;
        private String displayedType;

        public TypeProperty() {
            super(PROP_TYPE, String.class, "Type", "Data type of the variable");
            setValue("suppressCustomEditor", Boolean.TRUE);
        }

        public TypeProperty(IPrototype variable) {
            this();
            this.variable = variable;
            if (ExplorationApplication.isVariableSystem(variable)) {
                displayableType = "System";
            } else {
                displayableType = variable.getType().getSimpleName();
            }
            displayedType = displayableType;
        }

        public String getDisplayableType() {
            return displayableType;
        }

        public void setDisplayedType(String displayedType) {
            this.displayedType = displayedType;
        }

        @Override
        public String getValue() throws IllegalAccessException, InvocationTargetException {
            return displayedType;
        }
    }
}
