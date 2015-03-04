/*
 * Copyright (C) 2014 Romain Reuillon
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
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

package org.openmole.plugin.task

import org.openmole.core.macros.Keyword._
import org.openmole.core.workflow.builder._
import org.openmole.core.workflow.data.Prototype

package statistic {
  trait StatisticPackage <: StatisticMethods {
    lazy val statistics = add[{ def addStatistic(sequence: Prototype[Array[Double]], stat: Prototype[Double], agg: StatisticalAggregation[Double]) }]
  }
}

package object statistic extends StatisticPackage
