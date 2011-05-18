/*
 * Copyright (C) 2011 Mathieu leclaire <mathieu.leclaire at openmole.org>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.ide.core.serializer

import com.thoughtworks.xstream.XStream
import java.io.EOFException
import com.thoughtworks.xstream.io.xml.DomDriver
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import org.openmole.ide.core.control.MoleScenesManager
import org.openmole.ide.core.workflow.model.IMoleScene
import org.openmole.ide.core.workflow.implementation.EntitiesUI
import org.openmole.ide.core.workflow.implementation.EntityUI
import org.openmole.ide.core.workflow.implementation.MoleScene
import org.openmole.ide.core.workflow.implementation.TaskUI
import org.openmole.ide.core.commons.Constants

object GUISerializer {
  val moleSceneClass = classOf[MoleScene]
  val prototypeClass = classOf[EntityUI]
  val taskClass = classOf[TaskUI]
  val samplingClass = classOf[EntityUI]
    
  val xstream = new XStream(new DomDriver)
  xstream.registerConverter(new MoleSceneConverter)
  xstream.registerConverter(new PrototypeConverter)
  xstream.registerConverter(new TaskConverter);
  xstream.registerConverter(new SamplingConverter)
  xstream.alias("molescene", moleSceneClass)
  xstream.alias("prototype", prototypeClass)
  xstream.alias("task", taskClass)
  xstream.alias("sampling", samplingClass)
  
  def serialize(toFile: String) = {
    val writer = new FileWriter(new File(toFile))
    
    //root node
    val out = xstream.createObjectOutputStream(writer, "openmole")

    //prototypes
    EntitiesUI.entities(Constants.PROTOTYPE).getAll.foreach(out.writeObject(_))

    //tasks
    EntitiesUI.entities(Constants.TASK).getAll.foreach(out.writeObject(_))
        
    //samplings
    EntitiesUI.entities(Constants.SAMPLING).getAll.foreach(out.writeObject(_))

    //molescenes
//    MoleScenesManager.moleScenes.foreach(out.writeObject(_))

    out.close
  }
  
  def unserialize(fromFile: String) = {
    val reader = new FileReader(new File(fromFile))
    val in = xstream.createObjectInputStream(reader)
   
    EntitiesUI.entities(Constants.PROTOTYPE).clearAll
    EntitiesUI.entities(Constants.TASK).clearAll
    EntitiesUI.entities(Constants.SAMPLING).clearAll
    MoleScenesManager.removeMoleScenes
    
    try {
      while(true) {
        val readObject = in.readObject
        readObject.getClass match{
//          case samplingClass=> SamplingsUI.register(readObject.asInstanceOf[SamplingUI])
//          case prototypeClass=> PrototypesUI.register(readObject.asInstanceOf[PrototypeUI])
//          case taskClass=> TasksUI.register(readObject.asInstanceOf[TaskUI])
//          case moleSceneClass=> MoleScenesManager.addMoleScene(readObject.asInstanceOf[IMoleScene])
      //    case _=> MoleExceptionManagement.showException("Unknown class " + readObject.getClass)
          case _=> 
        }
      }
    } catch {
      case eof: EOFException => println("Ugly stop condition of Xstream reader !")
    }
  }
}
//
//
//    private static GUISerializer instance;
//    XStream xstream = new XStream(new DomDriver());
//    final Class moleSceneClass = MoleScene.class;
//    final Class prototypeClass = PrototypeUI.class;
//    final Class taskClass = TaskUI.class;
//    final Class samplingClass = SamplingUI.class;
//
//    public GUISerializer() {
//        xstream = new XStream(new DomDriver());
//        xstream.registerConverter(new MoleSceneConverter());
//        xstream.registerConverter(new PrototypeConverter());
//        xstream.registerConverter(new TaskConverter());
//        xstream.registerConverter(new SamplingConverter());
//        xstream.alias("molescene", moleSceneClass);
//        xstream.alias("prototype", prototypeClass);
//        xstream.alias("task", taskClass);
//        xstream.alias("sampling", samplingClass);
//    }
//
//    public void serialize(String toFile) throws IOException {
//        FileWriter writer = new FileWriter(new File(toFile));
//
//        //root node
//        ObjectOutputStream out = xstream.createObjectOutputStream(writer, "openmole");
//
//        //prototypes
//        for (Iterator<IEntityUI> itp = PrototypesUI.getInstance().getAll().iterator(); itp.hasNext();) {
//            out.writeObject(itp.next());
//        }
//
//        //tasks
//        for (Iterator<IEntityUI> itt = TasksUI.getInstance().getAll().iterator(); itt.hasNext();) {
//            out.writeObject(itt.next());
//        }
//
//        //samplings
//        for (Iterator<IEntityUI> its = SamplingsUI.getInstance().getAll().iterator(); its.hasNext();) {
//            out.writeObject(its.next());
//        }
//
//        //molescenes
//        for (Iterator<IMoleScene> itms = MoleScenesManager.getInstance().getMoleScenes().iterator(); itms.hasNext();) {
//            out.writeObject(itms.next());
//        }
//
//        out.close();
//    }
//
//    public void unserialize(String fromFile) throws FileNotFoundException, IOException, EOFException {
//
//        FileReader reader = new FileReader(new File(fromFile));
//
//        ObjectInputStream in = xstream.createObjectInputStream(reader);
//        PrototypesUI.getInstance().clearAll();
//        TasksUI.getInstance().clearAll();
//        SamplingsUI.getInstance().clearAll();
//        MoleScenesManager.getInstance().removeMoleScenes();
//        
//        Object readObject;
//        for (int i = 0;; i++) {

//    }
//
//    public static GUISerializer getInstance() {
//        if (instance == null) {
//            instance = new GUISerializer();
//        }
//        return instance;
//    }
//}