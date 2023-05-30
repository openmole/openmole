package org.openmole.site.content.documentation.utilityTask

/*
 * Copyright (C) 2023 Romain Reuillon
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

import scalatags.Text.all.{h2 => _, h3 => _, br => _, code => _, img => _, _}
import org.openmole.site._
import org.openmole.site.tools._
import org.openmole.site.stylesheet._
import DocumentationPages._
import org.openmole.site.Config._
import org.openmole.site.content.Native._

object SpatialTask extends PageContent(html"""



Geosimulation models, in the broad sense of simulation models in which the spatial configuration of agents plays a significant roles in the underlying processes (think e.g. of spatial interaction models), are generally tested for sensitivity on processes or agents parameters, but less frequently on the spatial configuration itself.
A recent ${aa("paper", href:= "http://jasss.soc.surrey.ac.uk/22/4/10.html")} proposed the generation of synthetic spatial configurations as a method to test the sensitivity of geosimulation models to the initial spatial configuration.
Some complementary work (${aa("paper", href:= "https://www.mitpressjournals.org/doi/abs/10.1162/isal_a_00159")}) focused on similar generator at larger scales, namely generators for building configurations at the scale of the district.

$br

More generally, the ${aa("spatial data library", href:= "https://github.com/openmole/spatialdata")} developed by the OpenMOLE team integrates these kind of methods in a larger context, including for example synthetic spatial networks and perturbation of real data, but also spatial interaction models and urban dynamics models.

$br

Some of the corresponding spatial generators are included in OpenMOLE as ${i{"Task"}}.
In the current development version, only some grid generators are included, for a reason of types for output prototypes (synthetic networks are difficult to represent as simple types and to feed as inputs to models).
All generators output the generated grids in a provided ${code{"Val[Array[Array[Double]"}}, along with the generation parameters for the generators taken as arguments.



${h2{"Random grid sampling"}}

A raster with random values:

${hl.openmole("""val myGrid = Val[Array[Array[Double]]]
val myDensity = Val[Double]

val myGenerator =
  RandomSpatialSamplingTask(
    grid = myGrid,
    gridSize = 10,
    density = myDensity
  )

val myModel =
  ScalaTask("println(myGrid.size)") set (
    (inputs, outputs) += myGrid
  )

DirectSampling(
  sampling = myDensity in (0.0 to 1.0 by 0.1),
  evaluation = myGenerator -- myModel
)""", name= "random grid sampling")}

$br

where

${ul(
  li{html"${code{"worldSize"}} is the width of the generated square grid,"},
  li{html"the density parameter is optional and produces a binary grid of given density in average if provided,"}
)}

${h2{"Blocks grid sampling"}}

A binary grid with random blocks (random size and position). With the same arguments as before, except the factors for the generator parameters: ${code{"blocksNumber"}} is the number of blocks positioned, ${code{"blocksMinSize"}}/${code{"blocksMaxSize"}} minimal/maximal (exchanged if needed) width/height of blocks, each being uniformly drawn for each block.

${hl.openmole("""
val myGrid = Val[Array[Array[Double]]]
val myBlocksNumber = Val[Int]
val myBlocksMinSize = Val[Int]
val myBlocksMaxSize = Val[Int]

val myGenerator =
  BlocksGridSpatialSamplingTask(
    grid = myGrid,
    gridSize = 10,
    number = myBlocksNumber,
    minSize = myBlocksMinSize,
    maxSize = myBlocksMaxSize
  )


val myModel =
  ScalaTask("println(myGrid.size)") set (
    (inputs, outputs) += myGrid
  )

DirectSampling(
  sampling =
    (myBlocksNumber in (10 to 15)) x
      (myBlocksMinSize in (1 to 3)) x
      (myBlocksMaxSize in RangeDomain[Int]("myBlocksMinSize + 3", "myBlocksMinSize + 5")),
  evaluation = myGenerator -- myModel
)""", name="block grid sampling")}

${h2{"Thresholded exponential mixture sampling"}}

A binary grid created with an exponential mixture, with kernels of the form ${code{"exp(-r/r0)"}}. A threshold parameter is applied to produce the binary grid.


${hl.openmole("""
val myGrid = Val[Array[Array[Double]]]
val myCenter = Val[Int]
val myRadius = Val[Double]
val myThreshold = Val[Double]

val myGenerator =
  ExpMixtureThresholdSpatialSamplingTask(
    grid = myGrid,
    gridSize = 10,
    center = myCenter,
    radius = myRadius,
    threshold = myThreshold
  )

val myModel =
  ScalaTask("println(myGrid.size)") set (
    (inputs, outputs) += myGrid
  )

DirectSampling(
  sampling =
    (myCenter in (1 to 20)) x
      (myRadius in (1.0 to 20.0)) x
      (myThreshold in (2.0 to 30.0)),
  evaluation = myGenerator -- myModel
)""", name="block grid sampling")}

$br

with the specific parameters as factors for generator parameters:

${ul(
  li{html"${code{"center"}} the number of kernels,"},
  li{html"${code{"radius"}} the range of kernels,"},
  li{html"${code{"threshold"}} the threshold to produce the binary grid."}
)}

${h2{"Percolated grid sampling"}}

${b{"USE WITH CAUTION - SOME PARAMETER VALUES YIELD VERY LONG GENERATION RUNTIME"}}

$br

A binary grid resembling a labyrinthine building organisation, obtained by percolating a grid network (see details in ${aa("paper", href:= "https://www.mitpressjournals.org/doi/abs/10.1162/isal_a_00159")}.
It percolates a grid network until a fixed number of points on the boundaries of the world are linked through the giant cluster.
The resulting network is transposed to a building configuration by assimilating each link to a street with a given width as a parameter.

${hl.openmole("""
val myGrid = Val[Array[Array[Double]]]
val myPercolation = Val[Double]
val myBordPoint = Val[Int]
val myLinkWidth = Val[Double]

val myGenerator =
  PercolationGridSpatialSamplingTask(
    grid = myGrid,
    gridSize = 10,
    percolation = myPercolation,
    bordPoint = myBordPoint,
    linkWidth = myLinkWidth)

val myModel =
  ScalaTask("println(myGrid.size)") set (
    (inputs, outputs) += myGrid
  )

DirectSampling(
  sampling =
    (myPercolation in (0.1 to 1.0 by 0.1)) x
      (myBordPoint in (1 to 30)) x
      (myLinkWidth in (1.0 to 5.0)),
  evaluation = myGenerator -- myModel
)""", name="percolation grid sampling")}

$br

with

${ul(
  li{html"${code{"percolation"}} the percolation probability,"},
  li{html"${code{"bordPoint"}} the number of points on the bord of the grid to belong to the giant cluster,"},
  li{html"${code{"linkWidth"}} the width of the final streets."}
)}

${h2{"Reaction diffusion population grid sampling"}}

Urban morphogenesis model for population density introduced by ${aa("(Raimbault, 2018)", href:= "https://journals.plos.org/plosone/article?id=10.1371/journal.pone.0203516")}.

$br

${b{"USE WITH CAUTION - SOME PARAMETER VALUES YIELD VERY LONG GENERATION RUNTIME"}}

${hl.openmole("""
val myGrid = Val[Array[Array[Double]]]
val myGrowthRate = Val[Double]

val myGenerator =
  ReactionDiffusionSpatialTask(
    grid = myGrid,
    gridSize = 10,
    alpha = 10.0,
    beta = 10.0,
    nBeta = 10,
    growthRate = myGrowthRate,
    totalPopulation = 10)

val myModel =
  ScalaTask("println(myGrid.size)") set (
    (inputs, outputs) += myGrid
  )

DirectSampling(
  sampling =
    (myGrowthRate in (1.0 to 10.0)),
  evaluation = myGenerator -- myModel
)""", name="reaction diffusion grid sampling")}

$br

with

${ul(
  li{html"${code{"gridSize"}} width of the square grid,"},
  li{html"${code{"alpha"}} strength of preferential attachment,"},
  li{html"${code{"beta"}} strength of diffusion,"},
  li{html"${code{"nBeta"}} number of times diffusion is operated at each time step,"},
  li{html"${code{"growthRate"}} number of population added at each step,"},
  li{html"${code{"totalPopulation"}} the final total population."}
)}


${h2{"OpenStreetMap buildings sampling"}}

${i{"Currently being implemented"}}



""")
