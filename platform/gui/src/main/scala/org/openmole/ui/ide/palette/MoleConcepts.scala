/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.openmole.ui.ide.palette


object MoleConcepts extends Enumeration {
    
  val TASK_INSTANCE= new Concept("task")
  val CAPSULE_INSTANCE= new Concept("capsule")
  val PROTOTYPE_INSTANCE= new Concept("prototy")
  val SAMPLING_INSTANCE= new Concept("sampling")
  
 
  class Concept(name: String) 
  
//  extends Val(name)
//  protected final def Value(name: String): Concept = new Concept(name)
}
  
  
//    type Category= Value
//    val TASK_INSTANCE,
//    CAPSULE_INSTANCE,
//    PROTOTYPE_INSTANCE,
//    SAMPLING_INSTANCE= Value
//  
//  def toString(cat: Category): String= {
//    cat match{
//      case Category.TASK_INSTANCE => "task"
//      case Category.CAPSULE_INSTANCE => "capsule"
//      case Category.PROTOTYPE_INSTANCE => "prototype"
//      case Category.SAMPLING_INSTANCE => "sampling"
//      case _ => "unknown string"
//    }
//  }
//}

  
//public class Category{
//
//    public enum CategoryName {
//	TASK_INSTANCE,
//	CAPSULE_INSTANCE,
//	PROTOTYPE_INSTANCE,
//	SAMPLING_INSTANCE,
//    }

//  public static String toString(CategoryName cat){
//        
//    if (cat == CategoryName.TASK_INSTANCE) return "task";
//    else if(cat == CategoryName.CAPSULE_INSTANCE) return "capsule";
//    else if(cat == CategoryName.PROTOTYPE_INSTANCE) return "prototype";
//    else if(cat == CategoryName.SAMPLING_INSTANCE) return "sampling";
//    return "unknown string";
//  }
//    
//}