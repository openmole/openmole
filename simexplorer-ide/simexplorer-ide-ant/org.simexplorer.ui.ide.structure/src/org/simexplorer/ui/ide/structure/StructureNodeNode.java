/*
 *
 *  Copyright (c) 2009, Cemagref
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
package org.simexplorer.ui.ide.structure;

import java.awt.datatransfer.Transferable;
import java.awt.dnd.DnDConstants;
import java.awt.event.ActionEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.Children;
import org.openide.nodes.PropertySupport;
import org.openide.nodes.Sheet;
import org.openide.util.Utilities;
import org.openide.util.datatransfer.PasteType;
import org.openide.util.lookup.Lookups;
import org.openmole.core.structuregenerator.ComplexNode;
import org.openmole.core.structuregenerator.PrototypeNode;
import org.openmole.core.structuregenerator.SequenceNode;
import org.openmole.core.structuregenerator.StructureNode;
import org.simexplorer.ui.ide.structure.csv.CsvOutputImporterDialog;
import org.simexplorer.ui.ide.workflow.model.MetadataProperties;
import org.simexplorer.ui.ide.workflow.model.MetadataProxy;

public abstract class StructureNodeNode<ST extends StructureNode> extends AbstractNode {

    protected ComplexNodeChildren parent;
    protected Class<ST> nodeClass;
    protected ST nodeValue;
    private final static String PROP_TYPE = "type";
    private TypeProperty typeProperty;

    /**
     * A lookup is built using the nodeValue object.
     * @param children The children object
     * @param nodeValue The data to store in this node
     * @param parent The parent children object
     */
    public StructureNodeNode(Children children, ST nodeValue, ComplexNodeChildren parent) {
        this(children, nodeValue, nodeValue, parent);
    }

    /**
     *
     * @param children The children object
     * @param nodeValue The data to store in this node
     * @param itemForLookup The object used to build the lookup
     * @param parent The parent children object
     */
    public StructureNodeNode(Children children, ST nodeValue, StructureNode itemForLookup, ComplexNodeChildren parent) {
        super(children, Lookups.singleton(itemForLookup));
        this.nodeValue = nodeValue;
        this.parent = parent;
        setDisplayName(nodeValue.getName());
    }

    public static StructureNodeNode createNode(StructureNode node, ComplexNodeChildren parent) {
        if (node instanceof PrototypeNode) {
            return new PrototypeNodeNode((PrototypeNode) node, parent);
        } else if (node instanceof SequenceNode) {
            return new SequenceNodeNode((SequenceNode) node, parent);
        } else {
            return new ComplexNodeNode((ComplexNode) node, parent);
        }
    }

    public PasteType getDropType(Transferable t, final int action, int index) {
/*        final Node dropNode = NodeTransfer.node( t,
                DnDConstants.ACTION_COPY_OR_MOVE+NodeTransfer.CLIPBOARD_CUT );
        if( null != dropNode ) {
            final Movie movie = (Movie)dropNode.getLookup().lookup( Movie.class );
            if( null != movie  && !this.equals( dropNode.getParentNode() )) {
                return new PasteType() {
                    public Transferable paste() throws IOException {
                        getChildren().add( new Node[] { new MovieNode(movie) } );
                        if( (action & DnDConstants.ACTION_MOVE) != 0 ) {
                            dropNode.getParentNode().getChildren().remove( new Node[] {dropNode} );
                        }
                        return null;
                    }
                };
            }
        }
  */      return null;
    }

   protected void createPasteTypes(Transferable t, List s) {
        super.createPasteTypes(t, s);
        PasteType paste = getDropType( t, DnDConstants.ACTION_COPY, -1 );
        if( null != paste )
            s.add( paste );
    }

    public boolean canDestroy() {
        return true;
    }



    @Override
    public Action[] getActions(boolean context) {
        return new Action[]{new EditAction(), new DeleteAction()};
    }

    protected class NewAction extends AbstractAction {

        public NewAction() {
            putValue(NAME, "Add a child…");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NewStructurePanel newStructurePanel = new NewStructurePanel();
            if (newStructurePanel.showDialog("New structure")) {
                Utilities.actionsGlobalContext().lookup(ComplexNode.class).add(newStructurePanel.getStructure());
                InputStructureTopComponent.findInstance().applicationUpdated();
                OutputStructureTopComponent.findInstance().applicationUpdated();
            }
        }
    }

    protected class ImportAction extends AbstractAction {

        public ImportAction() {
            putValue(NAME, "Import structure from csv file…");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            CsvOutputImporterDialog csvOutputImporterDialog=new  CsvOutputImporterDialog(null, true,(ComplexNode)nodeValue);
            csvOutputImporterDialog.setVisible(true);
        }
    }

    protected class EditAction extends AbstractAction {

        public EditAction() {
            putValue(NAME, "Edit…");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NewStructurePanel newStructurePanel = new NewStructurePanel(nodeValue, parent.getInputStructure());
            if (newStructurePanel.showDialog("Edit structure")) {
                newStructurePanel.getStructure();
                InputStructureTopComponent.findInstance().applicationUpdated();
                OutputStructureTopComponent.findInstance().applicationUpdated();
            }
        }
    }

    protected class DeleteAction extends AbstractAction {

        public DeleteAction() {
            putValue(NAME, "Remove");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            parent.removeNode(nodeValue);
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
        typeProperty = new TypeProperty(nodeValue);
        ss.put(typeProperty);


        // set the metadata properties in a new propertiy tab
        MetadataProperties metadataProperties = new MetadataProperties(MetadataProxy.getMetadata(nodeValue));
        sheet.put(metadataProperties.getProperties());
        return sheet;
    }

    public static class TypeProperty extends PropertySupport.ReadOnly<String> {

        private StructureNode data;

        public TypeProperty() {
            super(PROP_TYPE, String.class, "Type", "Type of the data");
            setValue("suppressCustomEditor",Boolean.TRUE);
        }

        public TypeProperty(StructureNode data) {
            this();
            this.data = data;
        }

        private StringBuilder getTypeAsString(StructureNode data) {
            if (data instanceof PrototypeNode) {
                return new StringBuilder(((PrototypeNode) data).getPrototype().getType().getSimpleName());
            } else if (data instanceof ComplexNode) {
                return new StringBuilder("Complex");
            } else {
                return getTypeAsString(((SequenceNode) data).getInnerNode()).append(" []");
            }
        }

        @Override
        public String getValue() {
            return getTypeAsString(data).toString();
        }
    }
}
