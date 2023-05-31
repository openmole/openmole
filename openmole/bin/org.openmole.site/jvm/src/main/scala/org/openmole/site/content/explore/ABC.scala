package org.openmole.site.content.explore

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

import org.openmole.site.content.header.*

object ABC extends PageContent(html"""

${h2{"ABC"}}

Calibration is about tuning a model parameter values. Generally, we want to set values that are consistent with data. One example, typical of the way we do it in OpenMOLE, is when we don’t have any data with which we can directly compute parameter values (e.g. by taking the mean of the data), but have data that we can compare to the model output. The problem of calibration is then to estimate parameter values with which the model output reproduces the data.

The objective of ${aa("Approximate Bayesian computation (ABC)", href := "https://en.wikipedia.org/wiki/Approximate_Bayesian_computation")} is to account for uncertainty when performing calibration. We have already approached calibration using optimisation with NSGA2, but it is little equipped to deal with the model stochasticity. Optimisation-based calibration searches for parameter values which minimize a distance between the model output and the data. When the model is stochastic the distance can vary from one simulation run to the next, even though the parameter values are kept constant. How, then, can we chose the parameter values with the smallest distance when they don’t always give the same? With NSGA2, we usually run several simulations with the same parameter values and compare the data to the median or average over the replications. We are circumventing the problem by using quantities that are more stable than individual simulation outputs. But the median or average is not always representative of the model individual output (think of a model output which has a bimodal distribution). Moreover, we are deliberately discarding information about the model randomness, and that information is valuable.

${h2{"An example"}}

${hl.openmole(s"""

val theta1 = Val[Double]
val theta2 = Val[Double]
val theta3 = Val[Double]
val o1 = Val[Array[Double]]

val model = ScalaTask ($tq
    import org.apache.commons.math3.distribution.MixtureMultivariateNormalDistribution
    val theta = Vector(theta1, theta2, theta3)

    val cov1: Array[Array[Double]] = Array(
      Array(1.0 / 2.0, -0.4, 0),
      Array(-0.4, 1.0 / 2.0, 0),
      Array(0, 0, 1.0/2.0))

    val cov2: Array[Array[Double]] = Array(
      Array(1 / 100.0, 0.0, 0.0),
      Array(0.0, 1 / 100.0, 0.0),
      Array(0.0, 0.0, 1/100.0))

    val mixtureWeights = Array(0.5, 0.5)
    val mean1 = Array(theta1 - 1, theta2 - 1, theta3)
    val mean2 = Array(theta1 + 1, theta2 + 1, theta3).toArray
    val dist = new MixtureMultivariateNormalDistribution(
      mixtureWeights, Array(mean1, mean2), Array(cov1, cov2))

    //val Array(o1, o2, o3) = dist.sample
    val o1 = dist.sample
$tq) set(
  inputs += (theta1, theta2, theta3),
  outputs += (o1),
  outputs += (theta1, theta2, theta3)
)

// The script writes results in csv files in the directory
// `posteriorSample`. Each file contains a weighted sample. The
// column `weight` gives the weights, and the columns `theta0` and
// `theta1` give the parameter values for each dimension. (For more
// information about the other columns, see M. Lenormand, F. Jabot,
// G. Deffuant; Adaptive approximate Bayesian computation for complex
// models. 2012. Computational Statistics 28)
IslandABC(
  evaluation = model,
  //The parameters prior distribution is uniform between -10 and 10 for
  //all dimensions.
  prior = Seq(
    theta1 in (-10.0, 10.0),
    theta2 in (-10.0, 10.0),
    theta3 in (-10.0, 10.0)),
  //The observed value is `(0, 0, 0)`.
  observed = Seq(o1 -> Array(0.0,0.0,0.0)),
  sample = 1000,
  generated = 5000,
  minAcceptedRatio = 0.01,
  stopSampleSizeFactor = 5,
  parallelism=4
) hook(workDirectory / "posteriorSample")""")}

""")
