package org.openmole.core.serializer.fury

import com.fasterxml.jackson.databind
import org.openmole.core.serializer.SerializerService
import org.openmole.tool.stream.StringInputStream

import java.io.{FileInputStream, InputStream, OutputStream}
import scala.reflect.ClassTag
import scala.util.NotGiven

/*
 * Copyright (C) 2024 Romain Reuillon
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
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import org.openmole.tool.file.*
import org.apache.fury.*
import org.apache.fury.config.*
import org.apache.fury.io.*

object FurySerializerService:
  def apply() = new SerializerService
  def stub() = apply()


class FurySerializerService:
  def buildFury() = Fury.builder().withLanguage(Language.JAVA)
    .withScalaOptimizationEnabled(true)
    .requireClassRegistration(false)
    .withClassLoader(SerializerService.getClass.getClassLoader)
    .withRefTracking(true)
    .build()

  def deserialize[T](file: File): T = file.withFileInputStream(deserialize[T])
  def deserialize[T](is: InputStream)(using NotGiven[T =:= Nothing]): T = buildFury().deserialize(new FuryInputStream(is)).asInstanceOf[T]

  def listFiles(obj: Any) = FilesListing.list(buildFury(), obj)
  def serialize(obj: Any, os: OutputStream) = buildFury().serialize(os, obj)
