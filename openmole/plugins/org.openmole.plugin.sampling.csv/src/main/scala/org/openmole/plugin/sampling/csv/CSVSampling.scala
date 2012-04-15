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
import org.openmole.core.implementation.sampling.Sampling
import org.openmole.core.implementation.sampling.SamplingBuilder
import org.openmole.core.model.data._
import java.io.FileReader
import org.openmole.core.implementation.data._
import org.openmole.core.model.sampling.ISampling
import au.com.bytecode.opencsv.CSVReader
import collection.JavaConversions._

object CSVSampling {
  
  def apply(
    file: File    
  ) = new SamplingBuilder { builder =>
    var _columns: List[(String, IPrototype[_])] = List.empty
    var _fileColumns: List[(String, File, IPrototype[File])] = List.empty
    
    def columns = new {
      
      def +=(proto: IPrototype[_]): builder.type = this.+=(proto.name, proto)
      def +=(name: String, proto: IPrototype[_]): builder.type = {
        _columns ::= (name, proto)
        builder
      }
      def +=(name: String, dir: File, proto: IPrototype[File]): builder.type = {
        _fileColumns ::= ((name, dir, proto))
        builder
      }
      
      def +=(dir: File, proto: IPrototype[File]): builder.type = this.+=(proto.name, dir, proto)
    }
    
    
    def toSampling = new CSVSampling(file) {
      val columns = builder._columns.reverse
      val fileColumns = builder._fileColumns.reverse
    }
  }
  
}


abstract sealed class CSVSampling(file: File) extends Sampling {

  override def prototypes = 
    columns.map{case(_, p) => p} ::: 
  fileColumns.map{case(_, _, p) => p} ::: Nil
  
  def columns: List[(String, IPrototype[_])]
  def fileColumns: List[(String, File, IPrototype[File])]
  
 
  
  /**
   * Builds the plan.
   *
   */
  override def build(context: IContext): Iterator[Iterable[IVariable[_]]] = {
    var listOfListOfValues = List[Iterable[IVariable[_]]]()
    val reader = new CSVReader(new FileReader(file))
    val headers = reader.readNext.toArray
    
    //test wether prototype names belong to header names
    val columnsIndexes = columns.map {
      case(name, _) => 
        val i = headers.indexOf(name)
        if(i == -1) throw new UserBadDataError("Unknown column name : " + name)
        else i
    }
    
    val fileColumnsIndexes = 
      fileColumns.map{
        case(name,_,_)=> 
          val i = headers.indexOf(name)
          if(i == -1) throw new UserBadDataError("Unknown column name : " + name)
          else i
      }
      
    Iterator.continually(reader.readNext).takeWhile(_ != null).map {
      line =>
      (columns zip columnsIndexes).map{
        case((_, p), i) => new Variable(p.asInstanceOf[Prototype[Any]], StringConvertor.typeMapping(p.`type`.erasure).convert(line(i)))
      } :::
      (fileColumns zip fileColumnsIndexes).map{
        case((_, f, p), i) => new Variable(p, new FileMapping(f).convert(line(i)))
      } ::: Nil
    }
    
   
  }
  
   
}