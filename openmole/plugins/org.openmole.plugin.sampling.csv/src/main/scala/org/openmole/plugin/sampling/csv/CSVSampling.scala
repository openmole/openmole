/*
 * Copyright (C) 2011 mathieu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.openmole.plugin.sampling.csv

import java.io.File
import scala.collection.mutable.HashMap
import scala.collection.immutable.TreeMap
import org.openmole.misc.exception.UserBadDataError
import org.openmole.core.model.data._
import java.io.FileReader
import org.openmole.core.implementation.data._
import org.openmole.core.model.sampling.ISampling
import au.com.bytecode.opencsv.CSVReader
import collection.JavaConversions._

class CSVSampling(val csvFile: File) extends ISampling{
  /**
   * Creates an intstance of CSVPlan.
   *
   * @param csvFilePath, the path of the CSV file as a String
   */
  def this(csvFilePath: String) = this(new File(csvFilePath))
  
  override def prototypes = columns.map{_.proto} ::: fileColumns.map{_.proto}
  
  private var columns = List[PrototypeColumn]()
  private var fileColumns = List[FileProtoypeColumn]()
  
  /**
   * Adds a prototype to be takken into account in the DoE
   *
   * @param proto, the prototyde to be added
   */
  def addColumn(proto: IPrototype[_]): Unit = columns = new PrototypeColumn(proto.name,proto) :: columns
  
  /**
   * 
   * @param dataset
   */
  def addColumn(dataSet: DataSet): Unit = dataSet.foreach{d=> columns= new PrototypeColumn(d.prototype.name,d.prototype)::columns}
  
  /**
   * Adds a prototype taken into account in the DoE and mapped to a csv header
   *
   * @param proto, the prototyde to be added
   * @param headerName, the mapped header
   */
  def addColumnAs(headerName: String, proto: IPrototype[_]): Unit = columns= new PrototypeColumn(headerName, proto):: columns
  
  /**
   * Adds a prototype extended from a File to be takken into account in the DoE
   *
   * @param proto, the prototyde to be added
   * @param basePath, the base path of the considered file (which is thus considered relativaly to this path) as a String
   */
  def addColumn(proto: IPrototype[File],basePath: String): Unit = addColumn(proto, new File(basePath))
    
  /**
   * Adds a prototype extended from a File to be taken into account in the DoE
   *
   * @param proto, the prototyde to be added
   * @param basePath, the base path of the considered file (which is thus considered relativaly to this path) as a File
   */
  def addColumn(proto: IPrototype[File],baseFile: File): Unit = fileColumns= new FileProtoypeColumn(proto.name,proto,baseFile)::fileColumns
  

  /**
   * Adds a prototype extended from a File to be takken into account in the DoE
   *
   * @param headerName, the name of the header to be matched with
   * @param proto, the prototyde to be added
   * @param basePath, the base path of the considered file (which is thus considered relativaly to this path) as a String
   */
  def addColumnAs(headerName: String, proto: IPrototype[File],basePath: String): Unit = addColumnAs(headerName, proto,new File(basePath))
  
  /**
   * Adds a prototype extended from a File to be takken into account in the DoE
   *
   * @param headerName, the name of the header to be matched with
   * @param proto, the prototyde to be added
   * @param basePath, the base path of the considered file (which is thus considered relativaly to this path) as a File
   */
  def addColumnAs(headerName: String, proto: IPrototype[File],baseFile: File): Unit = fileColumns= new FileProtoypeColumn(headerName,proto,baseFile):: fileColumns
  
  
  /**
   * Builds the plan.
   *
   */
  override def build(context: IContext): Iterable[Iterable[IVariable[_]]] = {
    var listOfListOfValues = List[Iterable[IVariable[_]]]()
    val reader = new CSVReader(new FileReader(csvFile))
    val headers = reader.readNext.toArray
    
    //test wether prototype names belong to header names
    columns.foreach(x=> {val i= headers.indexOf(x.name);
                            if(i == -1) throw new UserBadDataError("Unknown column name : " + x.name)
                            else {x.index = i;}})
    
    fileColumns.foreach(x=> {val i= headers.indexOf(x.name)
                            if(i == -1) throw new UserBadDataError("Unknown column name : " + x.name)
                            else {x.index = i}})
    
    var nextLine = reader.readNext
    while(nextLine != null) {
      var values = List[IVariable[_]]()
      columns.filter(_.index != -1).foreach{p=> values = new Variable(p.proto.asInstanceOf[Prototype[Any]], StringConvertor.typeMapping(p.proto.`type`.erasure).convert(nextLine(p.index)))::values}
      fileColumns.foreach{p=> values = new Variable(p.proto, new FileMapping(p.filePath).convert(nextLine(p.index)))::values}
      listOfListOfValues= values::listOfListOfValues
      nextLine = reader.readNext
    }
    reader.close
    listOfListOfValues
  }
  
  class PrototypeColumn(val name: String,val proto: IPrototype[_],var index: Int = -1)
  class FileProtoypeColumn(val name: String,val proto: IPrototype[File], val filePath: File,var index: Int= -1)
  
}