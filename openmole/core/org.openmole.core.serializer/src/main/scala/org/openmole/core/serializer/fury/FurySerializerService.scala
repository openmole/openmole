package org.openmole.core.serializer.fury

import org.openmole.core.serializer.SerializerService

import java.io.{OutputStream, InputStream}

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

  private def fileListing() = new FilesListing(buildFury())
  def listFiles(obj: Any) = fileListing().list(obj)
  def serialize(obj: Any, os: OutputStream) = buildFury().serialize(os, obj)
  def deserialize[T](is: InputStream): T = buildFury().deserialize(new FuryInputStream(is)).asInstanceOf[T]
