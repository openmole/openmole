/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.widget

import javax.swing.filechooser.FileNameExtensionFilter

class CSVChooseFileTextField(initialText: String) extends ChooseFileTextField(new FileNameExtensionFilter("CSV files","csv","CSV"),"Select a csv file",initialText)
