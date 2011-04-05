/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.control

import javax.swing.JEditorPane
import javax.swing.text.Document
import org.openide.filesystems.FileUtil
import org.openide.loaders.DataObject
import org.openide.text.CloneableEditorSupport
import org.openmole.ui.ide.workflow.model.ICapsuleView

object TaskSettingsManager extends TabManager{

  override def addTab(displayed: Object)= {
    
    val tcv= displayed.asInstanceOf[ICapsuleView]
    
    val editorPane= new JEditorPane
    val kit= CloneableEditorSupport.getEditorKit("text/x-groovy")
    editorPane.setEditorKit(kit)
    val fob= FileUtil.createMemoryFileSystem().getRoot().createData("tmp","groovy")
    val dob= DataObject.find(fob)
    editorPane.getDocument.putProperty(Document.StreamDescriptionProperty, dob)
    editorPane.setText("package dummy;")
    
    addMapping(tcv, editorPane,tcv.capsuleModel.taskModel.get.name)
    MoleScenesManager.addChild(tcv.scene, editorPane)
  } 
}

//public class TaskSettingsManager extends TabManager {
//
//  private static TaskSettingsManager instance = null;
//
//
//  @Override
//  public void addTab(Object displayed) {
//    ICapsuleView tcv = (ICapsuleView) displayed;
//    JEditorPane editorPane = new JEditorPane();
//
//    EditorKit kit = CloneableEditorSupport.getEditorKit("text/x-groovy");
//    editorPane.setEditorKit(kit);
//    FileObject fob;
//    try {
//      fob = FileUtil.createMemoryFileSystem().getRoot().createData("tmp",
//                                                                   "groovy");
//      //fob = FileUtil.getConfigRoot().createData("tmp", "groovy");
//      DataObject dob = DataObject.find(fob);
//      editorPane.getDocument().putProperty(
//        Document.StreamDescriptionProperty,
//        dob);
//      //  DialogBinding.bindComponentToFile(fob, 0, 0, editorPane);
//      editorPane.setText("package dummy;");
//    } catch (IOException ex) {
//      Exceptions.printStackTrace(ex);
//    }
//
////        taskSettingMap.put(tcv, editorPane);
////        tabbedPane.add(tcv.getName(), taskSettingMap.get(tcv));
//    addMapping(tcv, editorPane,tcv.getCapsuleModel().getTaskModel().getName());
//    MoleScenesManager.getInstance().addChild(tcv.getMoleScene(), editorPane);
//  }
//
//
////    private void addTaskSettingTab(ICapsuleView tcv) {
////        //    taskSettingMap.put(tcv,new ContainerComposerBuilder().setSplitOrientation(JSplitPane.VERTICAL_SPLIT)
////    /*    taskSettingMap.put(tcv,new ContainerComposerBuilder().setSplitOrientation(JSplitPane.VERTICAL_SPLIT)
////        .addComponent(new IOContainer())
////        .addComponent(new IOContainer())
////        //                                                                                   .setSplitOrientation(JSplitPane.HORIZONTAL_SPLIT)
////        .addComponent(new IOContainer())
////        .addComponent(new IOContainer())
////        .addComponent(new IOContainer())
////        .addComponent(new IOContainer())
////        .build());*/
////
////        JEditorPane editorPane = new JEditorPane();
////
////         EditorKit kit = CloneableEditorSupport.getEditorKit("text/x-groovy");
////        editorPane.setEditorKit(kit);
////        FileObject fob;
////        try {
////            fob = FileUtil.createMemoryFileSystem().getRoot().createData("tmp",
////                    "groovy");
////            //fob = FileUtil.getConfigRoot().createData("tmp", "groovy");
////            DataObject dob = DataObject.find(fob);
////            editorPane.getDocument().putProperty(
////                    Document.StreamDescriptionProperty,
////                    dob);
////            //DialogBinding.bindComponentToFile(fob, 0, 0, editorPane);
////            editorPane.setText("package dummy;");
////        } catch (IOException ex) {
////            Exceptions.printStackTrace(ex);
////        }
////        taskSettingMap.put(tcv, editorPane);
////        tabbedPane.add(tcv.getName(), taskSettingMap.get(tcv));
////    }
//
//  public static TaskSettingsManager getInstance() {
//    if (instance == null) {
//      instance = new TaskSettingsManager();
//    }
//    return instance;
//  }

//}
