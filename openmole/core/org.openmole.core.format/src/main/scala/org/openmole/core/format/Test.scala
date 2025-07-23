//package org.openmole.core.format
//
///*
// * Copyright (C) 2025 Romain Reuillon
// *
// * This program is free software: you can redistribute it and/or modify
// * it under the terms of the GNU Affero General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU Affero General Public License for more details.
// *
// * You should have received a copy of the GNU Affero General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//
//import com.volkhart.memory.ObjectGraphMeasurer
//import org.apache.fory.*
//import org.apache.fory.config.*
//import org.apache.fory.serializer.scala.*
//
//import java.io.{ByteArrayInputStream, ByteArrayOutputStream}
//import java.util.zip.{GZIPInputStream, GZIPOutputStream}
//
//import org.openmole.tool.file.*
//import org.openmole.core.context.*
//
//@main def test =
//  val random = new java.util.Random(42)
//
//  val context = Context(OMRFormat.variables(File("/tmp/hdose.omr"))(0)._2.filterNot(_.name == "reserve")*)
//
//  println(ObjectGraphMeasurer.measure(context))
//  println(CompactedContext.compact(context).asInstanceOf[IArray[Any]](1).asInstanceOf[Array[Byte]].size)
//
////  val array = Array.ofDim[Byte](1000)
////  random.nextBytes(array)
////  println(CompactedContext.decompress(CompactedContext.compressArray(array)))
//
//
////  val fory =
////    Fory.builder().withLanguage(Language.JAVA)
////      .withScalaOptimizationEnabled(true)
////      .requireClassRegistration(false)
////      .withRefTracking(true)
////      .suppressClassRegistrationWarnings(true)
////      .withStringCompressed(true)
////      .withNumberCompressed(true)
////      .build()
////
////  ScalaSerializers.registerSerializers(fory)
////
////
//
////  val factory = LZ4Factory.fastestInstance();
////  val compressor = factory.highCompressor()
////  val array = Array.ofDim[Byte](1000)
////
////  random.nextBytes(array)
////
////  val maxCompressedLength = compressor.maxCompressedLength(array.length)
////  val compressed = new Array[Byte](maxCompressedLength)
////  val compressedLength = compressor.compress(array.map(x => (x % 14).toByte), 0, array.length, compressed, 0, maxCompressedLength)
////
////
////  println(CompactedContext.compressArray(array.map(x => (x % 14).toByte)).length)
////  println(compressedLength)
////E
//  //
//
////  val t = Val[Array[Boolean]]
////
////  val context =
////    CompactedContext.expand:
////      CompactedContext.compact(Context(Variable(t, array)))
//
////
////  println(ObjectGraphMeasurer.measure(context))
////  println(ObjectGraphMeasurer.measure(compress(fory.serialize(context))))