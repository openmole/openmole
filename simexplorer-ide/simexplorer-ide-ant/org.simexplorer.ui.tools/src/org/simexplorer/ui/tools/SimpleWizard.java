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
package org.simexplorer.ui.tools;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Font;
import java.util.logging.Logger;
import javax.swing.JComponent;
import javax.swing.JLabel;
import org.netbeans.spi.wizard.WizardPage;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;

/**
 * <p>This class give a simple way to build a dialog box with a form, and input
 * validation message.
 * You have to : <ul>
 * <li>implement the method {@link SimpleWizard#isInputValid()} to check your
 * form and return the result message.</li>
 * <li>add the JLabel built by the method {@link SimpleWizard#buildMessageLabel()} that will display the messages and is gived by
 * {@link SimpleWizard#isInputValid()} in your form.</li>
 * </ul></p>
 *
 * <p>This class extends {@link WizardPage} to have the "Automatic listening to child
 * components" feature.</p>
 *
 */
public abstract class SimpleWizard extends WizardPage {

    private DialogDescriptor dialogDescriptor;
    private JLabel defaultMessageLabel;

    /**
     * Embeds the panel in a dialog box.
     * @param title The title of the dialog box.
     * @return true if the dialog box has been valided with the OK button.
     */
    public final boolean showDialog(String title) {
        dialogDescriptor = new DialogDescriptor(this, title);
        dialogDescriptor.setValid(false);
        Dialog dialog = DialogDisplayer.getDefault().createDialog(dialogDescriptor);
        dialog.setVisible(true);
        dialog.toFront();
        return dialogDescriptor.getValue() == DialogDescriptor.OK_OPTION;
    }

    /**
     * This method init a suitable JLabel to display the error message.
     * @return
     */
    protected final JLabel buildMessageLabel() {
        defaultMessageLabel = new JLabel();
        defaultMessageLabel.setForeground(Color.RED);
        defaultMessageLabel.setFont(defaultMessageLabel.getFont().deriveFont(Font.ITALIC));
        defaultMessageLabel.setHorizontalAlignment(JLabel.RIGHT);
        return defaultMessageLabel;
    }

    /**
     * Override this method to describe a specific way to display the validity message.
     * The common way is to invoke {@link SimpleWizard#buildMessageLabel} to get the default JLabel
     * that will be used to display messages.
     * @param message
     */
    protected void setValidityMessage(String message) {
        if (defaultMessageLabel != null) {
            defaultMessageLabel.setText(message);
        } else {
            Logger.getLogger(SimpleWizard.class.getName()).warning("It seems that nothing has been done to display validity message of your panel");
        }
    }

    /**
     * Override this method to achieve the validity tests of your form.
     * @return The error message or null (or empty String) if the form is valid.
     */
    protected abstract String isInputValid();

    /**
     * Call this method if you need to force the refresh of the validation of your form.
     * It is needed if you add you own swing component in the form, that will not
     * be taken into account for the autoaware input validation.
     */
    public final void validateContents() {
       validateContents(this, null);
    }

    @Override
    protected final String validateContents(Component component, Object event) {
        String isInputValid = isInputValid();
        if (dialogDescriptor != null) {
            if (isInputValid == null || isInputValid.length() == 0) {
                setValidityMessage("");
                dialogDescriptor.setValid(true);
            } else {
                dialogDescriptor.setValid(false);
                setValidityMessage(isInputValid);
            }
        }
        return isInputValid;
    }

    /**
     * This method could be overrided to set the component that will get the focus
     * @return
     */
    protected JComponent getDefaultComponent() {
        return null;
    }

    @Override
    public final void requestFocus() {
        JComponent defaultComponent = getDefaultComponent();
        if (defaultComponent != null) {
            defaultComponent.requestFocus();
        } else {
            super.requestFocus();
        }
    }
}
