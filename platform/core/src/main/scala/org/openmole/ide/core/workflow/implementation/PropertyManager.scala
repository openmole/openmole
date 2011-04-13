/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ide.core.workflow.implementation

import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.IOException
import java.util.Properties
import org.openmole.ide.core.commons.ApplicationCustomize
import org.openmole.ide.core.exception.MoleExceptionManagement
import org.openmole.ide.core.palette.MoleConcepts

object PropertyManager {
  val IMPL: String= "impl"
  val NAME: String= "name"
  val BG_IMG: String= "bg-img"
  val BG_COLOR: String= "bg-color"
  val THUMB_IMG: String= "thumb-img"
  val BORDER_COLOR: String= "border-color"

  def read(path: String): Properties= { 
    val props= new Properties(readDefault)
    var reader = new FileReader(path)
    try{
      reader = new FileReader(path)
    } catch {
      case ex: FileNotFoundException=> MoleExceptionManagement.showException(ex);
      case ex: IOException=> MoleExceptionManagement.showException(ex);
    } finally {
      props.load(reader)
      reader.close
    }
    props
  }
    
  def readProperties(cat: MoleConcepts.Concept)= {
    val path= "resources/"+cat.name
    val actual= new File(path)
    try{
      actual.listFiles.foreach(f=> Preferences.register(cat,Class.forName(f.getName),read(f.getPath)))
    } catch {
      case ex: ClassNotFoundException => MoleExceptionManagement.showException(ex)
    }  
  }
  
  def readDefault = {
    val props = new Properties
//    val reader = new FileReader(defaultPath)
//    props.load(reader)
//    reader.close
    props.setProperty(NAME,"default")
    props.setProperty(BG_IMG,"img/thumb/default.png")
    props.setProperty(THUMB_IMG,"img/thumb/default.png")
    props.setProperty(BORDER_COLOR,"0,0,0")
    props.setProperty(BG_COLOR,"255,255,255")
    props
  }
}
//
//public static final String NAME = "name";
//public static final String IMPL = "impl";
//public static final String BG_COLOR = "bg-color";
//public static final String BORDER_COLOR = "border-color";
//public static final String BG_IMG = "bg-img";
//public static final String THUMB_IMG = "thumb-img";

//public static void readProperties(CategoryName cat) {
//
//        
//  File actual = new File(System.getProperty("user.dir")+"/resources/",Category.toString(cat));
//  for (File f : actual.listFiles()) {
//    Properties props = read(f.getPath());
//    try {
//      Preferences.getInstance().register(cat,
//                                         Class.forName(f.getName()),
//                                         props);
//    } catch (ClassNotFoundException ex) {
//      MoleExceptionManagement.showException(ex);
//    }
//  }
//}
//
//public static Properties read(String path) {
//  try {
//    Properties props = new Properties(readDefault());
//    FileReader reader = new FileReader(path);
//    props.load(reader);
//    reader.close();
//    return props;
//  } catch (FileNotFoundException ex) {
//    MoleExceptionManagement.showException(ex);
//  } catch (IOException ex) {
//    MoleExceptionManagement.showException(ex);
//  }
//  return null;
//}
//
//private static Properties readDefault() throws FileNotFoundException, IOException{
//  Properties props = new Properties();
//  FileReader reader = new FileReader(ApplicationCustomize.TASK_DEFAULT_PROPERTIES);
//  props.load(reader);
//  reader.close();
//  return props;
//}
//}
