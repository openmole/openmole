/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.control

import javax.swing.JEditorPane
import javax.swing.text.Document
import org.openide.filesystems.FileUtil
import org.openide.loaders.DataObject
import org.openide.text.CloneableEditorSupport
import org.openmole.ide.core.workflow.model.ICapsuleUI

object TaskSettingsManager extends TabManager{

  override def addTab= this
  
  override def addTab(displayed: Object)= {
    
    val tcv= displayed.asInstanceOf[ICapsuleUI]
    
    val editorPane= new JEditorPane
    val kit= CloneableEditorSupport.getEditorKit("text/x-groovy")
    editorPane.setEditorKit(kit)
    val fob= FileUtil.createMemoryFileSystem().getRoot().createData("tmp","groovy")
    val dob= DataObject.find(fob)
    editorPane.getDocument.putProperty(Document.StreamDescriptionProperty, dob)
    editorPane.setText("package dummy;")
    
    addMapping(tcv, editorPane,tcv.dataProxy.get.dataUI.name)
    MoleScenesManager.addChild(tcv.scene, editorPane)
    editorPane
  } 
}
