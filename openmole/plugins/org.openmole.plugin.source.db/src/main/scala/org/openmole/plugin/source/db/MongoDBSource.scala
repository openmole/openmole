/*
 * Copyright (C) 09/07/13 Romain Reuillon
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

package org.openmole.plugin.source.db

import java.io.File

import monocle.macros.Lenses
import org.openmole.core.dsl
import org.openmole.core.dsl._
import org.openmole.core.workflow.builder.{InputOutputBuilder, InputOutputConfig}
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.tools.ExpandedString
import com.mongodb.casbah.Imports._

object MongoDBSource {

  implicit def isIO = InputOutputBuilder(MongoDBSource.config)

  def apply(
     host:      ExpandedString,
     port: Int,
     database: ExpandedString,
     collection: ExpandedString,
     query: (Context, MongoCollection) => String,
     prototype: Prototype[String]) =
    new MongoDBSource(
      host,
      port,
      database,
      collection,
      query,
      prototype,
      config = InputOutputConfig()
    ) set (dsl.outputs += prototype)

}

@Lenses case class MongoDBSource(
  host:      ExpandedString,
  port: Int,
  database: ExpandedString,
  collection: ExpandedString,
  query: (Context, MongoCollection) => String,
  prototype: Prototype[String],
  config:    InputOutputConfig
) extends Source {

  override def process(context: Context, executionContext: MoleExecutionContext)(implicit rng: RandomProvider) = {
    val mongoClient = MongoClient(host.from(context), port)
    val db = mongoClient(database.from(context))
    val c = db(collection.from(context))

    Variable(
      prototype,
      query(context, c)
    )
  }
}
