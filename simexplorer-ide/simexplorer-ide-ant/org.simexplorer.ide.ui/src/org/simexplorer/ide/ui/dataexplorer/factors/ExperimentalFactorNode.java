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
package org.simexplorer.ide.ui.dataexplorer.factors;

import org.simexplorer.ui.ide.workflow.model.MetadataProperties;
import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.NbBundle;
import org.openide.util.lookup.Lookups;
import org.openmole.core.implementation.plan.Factor;
import org.openmole.core.model.plan.IFactor;
import org.simexplorer.ui.ide.workflow.model.MetadataProxy;

class ExperimentalFactorNode extends AbstractNode {
        
    private ExperimentalFactorsChildren experimentalFactorsChildren;
    private final static String PROP_TYPE = "type";
    private final static String DOMAIN_TYPE = "domain";
    private IFactor<?,?> factor;
    
    public ExperimentalFactorNode(IFactor<?,?> factor, ExperimentalFactorsChildren experimentalFactorsChildren) {
        super(Children.LEAF, Lookups.singleton(factor));
        setDisplayName(factor.getPrototype().getName());
        this.factor = factor;
        this.experimentalFactorsChildren = experimentalFactorsChildren;
    }

    @Override
    public Action[] getActions(boolean arg0) {
        return new Action[]{new EditAction(), new DeleteAction(), new DuplicateAction()};
    }
    private class DeleteAction extends AbstractAction {

        public DeleteAction() {
            putValue(NAME, "Remove");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            experimentalFactorsChildren.removeFactor(getLookup().lookup(Factor.class));
        }
    }

    private class EditAction extends AbstractAction {

        private EditFactorAction action;

        public EditAction() {
            putValue(NAME, NbBundle.getMessage(EditFactorAction.class, "CTL_EditFactorAction"));
            action = new EditFactorAction();
            // TODO this should be the best way, but doesn't work
            //wizardAction = (EditFactorAction) Lookups.forPath("Actions/Exploration").lookup(EditFactorAction.class);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            action.performAction(getLookup().lookup(Factor.class),false);
        }
    }

    private class DuplicateAction extends AbstractAction {

        private EditFactorAction action;

        public DuplicateAction() {
            putValue(NAME, NbBundle.getMessage(EditFactorAction.class, "CTL_DuplicateFactorAction"));
            action = new EditFactorAction();
            // TODO this should be the best way, but doesn't work
            //wizardAction = (EditFactorAction) Lookups.forPath("Actions/Exploration").lookup(EditFactorAction.class);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            action.performAction(getLookup().lookup(Factor.class),true);
        }
    }

    @Override
    protected Sheet createSheet() {
        Sheet s = super.createSheet();
        Sheet.Set ss = s.get(Sheet.PROPERTIES);
        if (ss == null) {
            ss = Sheet.createPropertiesSet();
            s.put(ss);
        }
        ss.put(new TypeProperty(factor));
        ss.put(new DomainProperty(factor));
        // set the metadata properties in a new propertiy tab
        MetadataProperties metadataProperties=new MetadataProperties(MetadataProxy.getMetadata(factor));
        s.put(metadataProperties.getProperties());
        return s;
    }
    
    public static class TypeProperty extends PropertySupport.ReadOnly<String> {

        private IFactor<?,?> factor;

        public TypeProperty() {
            super(PROP_TYPE, String.class, "Type", "Data type of the factor");
            setValue("suppressCustomEditor",Boolean.TRUE);
        }

        public TypeProperty(IFactor<?,?> factor) {
            this();
            this.factor = factor;
        }

        @Override
        public String getValue() throws IllegalAccessException, InvocationTargetException {
            return factor.getPrototype().getType().getSimpleName();
        }

    }

    public static class DomainProperty extends PropertySupport.ReadOnly<String> {

        private IFactor<?,?> factor;

        public DomainProperty() {
            super(DOMAIN_TYPE, String.class, "Domain", "Domain of the factor");
            setValue("suppressCustomEditor",Boolean.TRUE);
        }

        public DomainProperty(IFactor<?,?> factor) {
            this();
            this.factor = factor;
        }

        @Override
        public String getValue() throws IllegalAccessException, InvocationTargetException {
            return factor.getDomain().getClass().getSimpleName();
        }

    }

}
