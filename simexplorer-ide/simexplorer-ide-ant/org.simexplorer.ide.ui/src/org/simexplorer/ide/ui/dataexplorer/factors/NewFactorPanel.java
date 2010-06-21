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

import com.rits.cloning.Cloner;
import java.awt.CardLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import org.openide.util.Lookup;
import org.openmole.core.implementation.data.Prototype;
import org.openmole.commons.exception.InternalProcessingError;
import org.simexplorer.ide.ui.PanelFactory;
import org.simexplorer.ide.ui.applicationexplorer.ApplicationsTopComponent;
import org.simexplorer.ide.ui.tools.SimpleWizard;
import org.openmole.core.implementation.plan.Factor;
import org.simexplorer.core.workflow.methods.DomainEditorPanel;
import org.openmole.core.implementation.domain.Domain;
import org.openmole.core.model.data.IPrototype;
import org.openmole.core.model.plan.IFactor;
import org.openmole.core.model.domain.IDomain;
import org.simexplorer.core.workflow.model.metada.Metadata;
import org.simexplorer.ui.ide.workflow.model.MetadataProxy;

public class NewFactorPanel extends SimpleWizard {

    private Factor factor;
    private DomainEditorPanel<Domain> domainEditor;
    private String lastDomainEditorRequested = null;
    private Map<String, Integer> domainEditorIndexes = new HashMap<String, Integer>();
    private ArrayList<String> factorsName;
    private boolean isDuplicated = false;

    /** Creates new form NewFactorPanel */
    public NewFactorPanel() {
        initComponents();
        factor = new Factor("", null, null);
        Domain[] domains = Lookup.getDefault().lookupAll(IDomain.class).toArray(new Domain[]{});
        Logger.getLogger(NewFactorPanel.class.getName()).info("Factors domain found: " + Arrays.toString(domains));
        int i = 0;
        for (Domain domain0 : domains) {
            DomainEditorPanel<Domain> ed = (DomainEditorPanel<Domain>) PanelFactory.getEditor(domain0.getClass());
            domainPanel.add(ed, domain0.getClass().getSimpleName());
            domainEditorIndexes.put(domain0.getClass().getSimpleName(), i++);
            JRadioButton jb = new JRadioButton(domain0.getClass().getSimpleName());
            jb.setActionCommand(domain0.getClass().getSimpleName());
            jb.addActionListener(new java.awt.event.ActionListener() {

                @Override
                public void actionPerformed(java.awt.event.ActionEvent evt) {
                    ((CardLayout) domainPanel.getLayout()).show(domainPanel, evt.getActionCommand());
                    lastDomainEditorRequested = evt.getActionCommand();
                    domainEditor = (DomainEditorPanel<Domain>) domainPanel.getComponent(domainEditorIndexes.get(lastDomainEditorRequested));
                    NewFactorPanel.this.validateContents(NewFactorPanel.this, evt);
                    NewFactorPanel.this.revalidate();
                }
            });
            buttonGroupDomainType.add(jb);
            buttonPanel.add(jb);

        }
        lastDomainEditorRequested = domains[0].getClass().getCanonicalName();
        domainPanel.add(new JPanel(), "null");
        ((CardLayout) domainPanel.getLayout()).show(domainPanel, "null");
        // fetch other factors names
        factorsName = new ArrayList<String>();
        for (IFactor e : ApplicationsTopComponent.findInstance().getExplorationApplication().getFactors()) {
            factorsName.add(e.getPrototype().getName());
        }
    }

    public NewFactorPanel(Factor factor0, boolean isDuplicated) throws InternalProcessingError {
        this();
        if (factor0 != null) {
            this.isDuplicated = isDuplicated;
            if (isDuplicated) {
                this.factor = (Factor) new Cloner().deepClone(factor0);
            } else {
                this.factor = factor0;
                // remove our name from the list
                factorsName.remove(factor0.getPrototype().getName());
            }
            factorName.setText(factor.getPrototype().getName());
            factorAbbreviation.setText(MetadataProxy.getMetadata(factor, "abbreviation"));
            factorUnits.setText(MetadataProxy.getMetadata(factor, "units"));
            factorDescription.setText(MetadataProxy.getMetadata(factor, "description"));
            Enumeration<AbstractButton> b = buttonGroupDomainType.getElements();
            // get the editor component from the panel (Cardlayout seem to only add methods to show ans not to select it)
            for (Component pa : domainPanel.getComponents()) {
                if (pa instanceof DomainEditorPanel) {
                    DomainEditorPanel pa0 = (DomainEditorPanel) pa;
                    if (Arrays.binarySearch(pa0.getTypesEditable(), factor.getDomain().getClass()) > -1) {
                        pa0.setObjectEdited(factor.getDomain());
                        String st = pa0.getClass().getSimpleName();
                        st = st.substring(0, st.lastIndexOf("Panel"));
                        ((CardLayout) domainPanel.getLayout()).show(domainPanel, st);
                        lastDomainEditorRequested = st;
                        domainEditor = pa0;
                        break;
                    }
                }
            }
            while (b.hasMoreElements()) {
                AbstractButton ab = b.nextElement();
                if (ab.getActionCommand().equals(lastDomainEditorRequested)) {
                    buttonGroupDomainType.setSelected(ab.getModel(), true);
                }
            }
            this.revalidate();
        }
    }

    public IFactor<?, ?> getFactor() {
        String name = factorName.getText();
        Metadata metadata = new Metadata();
        metadata.set("description", factorDescription.getText());
        metadata.set("units", factorUnits.getText());
        metadata.set("abbreviation", factorAbbreviation.getText());

        ((DomainEditorPanel<Domain>) domainPanel.getComponent(domainEditorIndexes.get(lastDomainEditorRequested))).applyChanges();
        DomainEditorPanel<Domain> domainEditorPanel = ((DomainEditorPanel<Domain>) domainPanel.getComponent(domainEditorIndexes.get(lastDomainEditorRequested)));
        if (isDuplicated) {
            factor = new Factor(name, domainEditorPanel.getType(), domainEditorPanel.getObjectEdited());
            MetadataProxy.setMetadata(factor, metadata);
        } else {
            IPrototype newProto = new Prototype(name, domainEditorPanel.getType());
            factor.setPrototype(newProto);
            MetadataProxy.setMetadata(factor, metadata);
            factor.setDomain(domainEditorPanel.getObjectEdited());
        }
        return factor;
    }

    public JTextField getFactorName() {
        return factorName;
    }

    @Override
    public String isInputValid() {
        if (factorName.getText().length() <= 0) {
            return "Enter the factor name";
        } else if (factorsName.contains(factorName.getText())) {
            return "This factor name already exists";
        } // TODO fix that?
        /* else if (!VerifyName.correct(factorName.getText())) {
        return VerifyName.ERROR_MESSAGE;
        }*/ else if (buttonGroupDomainType.getSelection() == null) {
            return "Choose a domain type";
        } else {
            return domainEditor != null ? domainEditor.isInputValid() : null;
        }
    }

    @Override
    protected JComponent getDefaultComponent() {
        return factorName;
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        buttonGroupDomainType = new javax.swing.ButtonGroup();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        factorName = new javax.swing.JTextField();
        factorUnits = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        factorAbbreviation = new javax.swing.JTextField();
        jScrollPane1 = new javax.swing.JScrollPane();
        factorDescription = new javax.swing.JTextArea();
        jLabel5 = new javax.swing.JLabel();
        domainPanel = new DomainPanel();
        buttonPanel = new javax.swing.JPanel();
        messageLabel = buildMessageLabel()
        ;

        jLabel1.setText(org.openide.util.NbBundle.getMessage(NewFactorPanel.class, "NewFactorPanel.jLabel1.text")); // NOI18N

        jLabel2.setText(org.openide.util.NbBundle.getMessage(NewFactorPanel.class, "NewFactorPanel.jLabel2.text")); // NOI18N

        factorName.setText(org.openide.util.NbBundle.getMessage(NewFactorPanel.class, "NewFactorPanel.factorName.text")); // NOI18N

        factorUnits.setText(org.openide.util.NbBundle.getMessage(NewFactorPanel.class, "NewFactorPanel.factorUnits.text")); // NOI18N

        jLabel3.setText(org.openide.util.NbBundle.getMessage(NewFactorPanel.class, "NewFactorPanel.jLabel3.text")); // NOI18N

        jLabel4.setText(org.openide.util.NbBundle.getMessage(NewFactorPanel.class, "NewFactorPanel.jLabel4.text")); // NOI18N

        factorDescription.setColumns(20);
        factorDescription.setRows(5);
        jScrollPane1.setViewportView(factorDescription);

        jLabel5.setText(org.openide.util.NbBundle.getMessage(NewFactorPanel.class, "NewFactorPanel.jLabel5.text")); // NOI18N

        domainPanel.setLayout(new java.awt.CardLayout());

        buttonPanel.setLayout(new javax.swing.BoxLayout(buttonPanel, javax.swing.BoxLayout.PAGE_AXIS));

        messageLabel.setText(org.openide.util.NbBundle.getMessage(NewFactorPanel.class, "NewFactorPanel.messageLabel.text")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(messageLabel, javax.swing.GroupLayout.DEFAULT_SIZE, 504, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 504, Short.MAX_VALUE)
                    .addComponent(jLabel4)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel3)
                            .addComponent(jLabel1)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(factorName, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
                            .addComponent(factorUnits, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)
                            .addComponent(factorAbbreviation, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 420, Short.MAX_VALUE)))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addComponent(buttonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 176, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(domainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 310, Short.MAX_VALUE))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel5)
                        .addGap(6, 6, 6)))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(factorName, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(factorUnits, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jLabel3)
                        .addGap(9, 9, 9)
                        .addComponent(jLabel4))
                    .addComponent(factorAbbreviation, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 93, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel5)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(domainPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 206, Short.MAX_VALUE)
                    .addComponent(buttonPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 206, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(messageLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 21, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.ButtonGroup buttonGroupDomainType;
    private javax.swing.JPanel buttonPanel;
    private javax.swing.JPanel domainPanel;
    private javax.swing.JTextField factorAbbreviation;
    private javax.swing.JTextArea factorDescription;
    private javax.swing.JTextField factorName;
    private javax.swing.JTextField factorUnits;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JLabel messageLabel;
    // End of variables declaration//GEN-END:variables
}
