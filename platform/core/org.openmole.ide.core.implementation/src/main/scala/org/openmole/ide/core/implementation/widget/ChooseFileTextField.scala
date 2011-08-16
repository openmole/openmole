/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.implementation.widget

import java.awt.Dimension
import javax.swing.filechooser.FileNameExtensionFilter
import scala.swing.FileChooser.SelectionMode.Value
import scala.swing.FileChooser
import scala.swing.FileChooser.Result.Approve
import scala.swing.TextField
import scala.swing.event.FocusGained

class ChooseFileTextField(fc: FileChooser, initialText: String) extends TextField {
  def this(filter: FileNameExtensionFilter, chooserTitle: String,t: String) = this(new FileChooser {
      fileFilter = filter
      title = chooserTitle},t)
  def this(chooserTitle: String,t: String, sm: Value) = this(new FileChooser{
      title = chooserTitle
      fileSelectionMode = sm},t)
  
  maximumSize = new Dimension(150,30)
    reactions += {
      case FocusGained(peer,_,false) =>
        focusable = false
        if (fc.showDialog(this,"OK") == Approve) text = fc.selectedFile.getPath
        focusable = true
    }
    text = initialText
  listenTo(this)
}
