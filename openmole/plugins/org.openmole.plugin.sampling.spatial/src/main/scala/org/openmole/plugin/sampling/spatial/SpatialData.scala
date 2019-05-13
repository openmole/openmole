package org.openmole.plugin.sampling.spatial

import org.openmole.core.exception.UserBadDataError

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.math._
import scala.util.Random

/**
 *
 */
// FIXME patch putting spatial primitives here before a first stable release of spatial data
object SpatialData {

  object OSMBuildings {

    // FIXME implement using spatialdata (do not import xml reader etc here)
    def buildingGrid(coordinates: Array[(Double, Double)], windowSize: Double, worldSize: Int) = Array.fill(coordinates.size) { Array.fill(worldSize) { Array.fill(worldSize)(0.0) } }
  }

  object RandomGrid {

    /**
     * Generate a sample of random grids
     *
     * @param gridSize
     * @param samples
     * @param rng
     * @return
     */
    def randomGridSample(gridSize: Int, samples: Int, rng: scala.util.Random) = Array.fill(samples, gridSize, gridSize) {
      rng.nextDouble()
    }

  }

  object ExponentialMixture {

    /**
     * Generate one exponential kernel mixture grid
     * -- DEPRECATED, function below is more general --
     *
     * @param gridSize
     * @param nCenters
     * @param maxValue
     * @param kernelRadius
     * @param rng
     * @return
     */
    def expMixtureGrid1D(gridSize: Int, nCenters: Int, maxValue: Double, kernelRadius: Double, rng: scala.util.Random): RasterLayerData[Double] = {
      val arrayVals = Array.fill[Double](gridSize, gridSize) {
        0.0
      }
      val centers = Array.fill[Int](nCenters, 2) {
        rng.nextInt(gridSize)
      }
      for (i ← 0 to gridSize - 1; j ← 0 to gridSize - 1) {
        for (c ← 0 to nCenters - 1) {
          arrayVals(i)(j) = arrayVals(i)(j) + maxValue * math.exp(-math.sqrt(math.pow((i - centers(c)(0)), 2) + math.pow((j - centers(c)(1)), 2)) / kernelRadius)
        }
      }
      arrayVals
    }

    /**
     * A multilayer exponential mixture with same centers
     *
     * @param gridSize
     * @param nCenters
     * @param maxValues
     * @param kernelRadius
     * @param rng
     * @return
     */
    def expMixtureGridSameCenters(
      gridSize:     Either[Int, (Int, Int)],
      nCenters:     Int,
      maxValues:    Either[Double, Seq[Double]],
      kernelRadius: Either[Double, Seq[Double]],
      rng:          scala.util.Random
    ): (RasterData[Double], Seq[Point2D]) = {
      // grid dimensions
      val dims: (Int, Int) = gridSize match {
        case Left(s)  ⇒ (s, s)
        case Right(d) ⇒ d
      }

      // ensure parameters consistency
      val maxVals = maxValues match {
        case Left(d)   ⇒ Seq(d)
        case Right(dd) ⇒ dd
      }
      val radiuses = kernelRadius match {
        case Left(d)   ⇒ Seq(d)
        case Right(dd) ⇒ dd
      }

      if (maxVals.size != radiuses.size) throw new UserBadDataError("Wrong input parameters")
      val layerdim = maxVals.size

      // generate centers
      val centers = Seq.fill[Point2D](nCenters) {
        (rng.nextInt(dims._1).toDouble, rng.nextInt(dims._2).toDouble)
      }

      // fill the empty raster
      val raster = Seq.fill[RasterLayerData[Double]](layerdim) {
        Array.fill(dims._1, dims._2)(0.0)
      }

      for (k ← 0 to layerdim - 1; i ← 0 to dims._1 - 1; j ← 0 to dims._2 - 1; c ← 0 to nCenters - 1) {
        raster(k)(i)(j) = raster(k)(i)(j) + maxVals(k) * math.exp(-math.sqrt(math.pow((i - centers(c)._1), 2) + math.pow((j - centers(c)._2), 2)) / radiuses(k))
      }
      (raster, centers)
    }

    /**
     * Generate a sample of exponent kernel mixture grids
     *
     * @param samples
     * @param gridSize
     * @param nCenters
     * @param maxValue
     * @param kernelRadius
     * @param rng
     */
    def expMixtureGridSameCentersSample(
      samples:      Int,
      gridSize:     Either[Int, (Int, Int)],
      nCenters:     Int                         = 1,
      maxValue:     Either[Double, Seq[Double]] = Left(1.0),
      kernelRadius: Either[Double, Seq[Double]] = Left(1.0),
      rng:          scala.util.Random
    ): Seq[RasterData[Double]] = {
      Seq.fill(samples) {
        expMixtureGridSameCenters(gridSize, nCenters, maxValue, kernelRadius, rng)._1
      }
    }

  }

  object ReactionDiffusion {

    /**
     * Reaction diffusion grid generation
     * @param gridSize
     * @return
     */
    def reactionDiffusionGrid(gridSize: Int, growthRate: Double, totalPopulation: Double, alphaAtt: Double, diffusion: Double, diffusionSteps: Int, rng: scala.util.Random) = {
      var arrayVals = Array.fill(gridSize, gridSize) { 0.0 }
      var population: Double = 0

      while (population < totalPopulation) {

        // add new population following pref att rule
        if (population == 0) {
          //choose random patch
          for (_ ← 1 to growthRate.toInt) { val i = rng.nextInt(gridSize); val j = rng.nextInt(gridSize); arrayVals(i)(j) = arrayVals(i)(j) + 1 }
        }
        else {
          //val oldPop = arrayVals.map((a: Array[Double]) ⇒ a.map((c: Cell) ⇒ math.pow(c.population / population, alphaAtt)))
          val oldPop = arrayVals.map { _.map { case x ⇒ math.pow(x / population, alphaAtt) } }
          val ptot = oldPop.flatten.sum

          for (_ ← 1 to growthRate.toInt) {
            var s = 0.0; val r = rng.nextDouble(); var i = 0; var j = 0
            //draw the cell from cumulative distrib
            while (s < r) {
              s = s + (oldPop(i)(j) / ptot)
              j = j + 1
              if (j == gridSize) { j = 0; i = i + 1 }
            }
            if (j == 0) { j = gridSize - 1; i = i - 1 } else { j = j - 1 };
            arrayVals(i)(j) = arrayVals(i)(j) + 1
          }
        }

        // diffuse
        for (_ ← 1 to diffusionSteps) {
          arrayVals = diffuse(arrayVals, diffusion)
        }

        // update total population
        population = arrayVals.flatten.sum

      }
      //Seq.tabulate(size, size) { (i: Int, j: Int) ⇒ arrayVals(i)(j) }
      arrayVals
    }

    /**
     * Diffuse to neighbors proportion alpha of capacities
     *
     * @param a
     */
    def diffuse(a: Array[Array[Double]], alpha: Double): Array[Array[Double]] = {
      val newVals = a.clone()
      val size = a.length

      for (i ← 0 to size - 1; j ← 0 to size - 1) {
        // diffuse in neigh cells
        if (i >= 1) { newVals(i - 1)(j) = newVals(i - 1)(j) + (alpha / 8) * a(i)(j) }
        if (i < size - 1) { newVals(i + 1)(j) = newVals(i + 1)(j) + (alpha / 8) * a(i)(j) }
        if (j >= 1) { newVals(i)(j - 1) = newVals(i)(j - 1) + (alpha / 8) * a(i)(j) }
        if (j < size - 1) { newVals(i)(j + 1) = newVals(i)(j + 1) + (alpha / 8) * a(i)(j) }
        if (i >= 1 && j >= 1) { newVals(i - 1)(j - 1) = newVals(i - 1)(j - 1) + (alpha / 8) * a(i)(j) }
        if (i >= 1 && j < size - 1) { newVals(i - 1)(j + 1) = newVals(i - 1)(j + 1) + (alpha / 8) * a(i)(j) }
        if (i < size - 1 && j >= 1) { newVals(i + 1)(j - 1) = newVals(i + 1)(j - 1) + (alpha / 8) * a(i)(j) }
        if (i < size - 1 && j < size - 1) { newVals(i + 1)(j + 1) = newVals(i + 1)(j + 1) + (alpha / 8) * a(i)(j) }
        //delete in the cell (¡ bord effect : lost portion is the same even for bord cells !)
        // to implement diffuse as in NL, put deletion inside boundary conditions checking
        newVals(i)(j) = newVals(i)(j) - alpha * a(i)(j)
      }
      newVals
    }

    def reactionDiffusionGridSample(samples: Int, gridSize: Int, growthRate: Double, totalPopulation: Double, alphaAtt: Double, diffusion: Double, diffusionSteps: Int, rng: scala.util.Random) = {
      Array.fill(samples) {
        reactionDiffusionGrid(gridSize, growthRate, totalPopulation, alphaAtt, diffusion, diffusionSteps, rng)
      }
    }

  }

  /**
   * From spatialdata
   */

  case class Node(id: Int, x: Double, y: Double)

  case class Link(e1: Node, e2: Node, weight: Double = 1.0, length: Double = 1.0)

  object Link { def apply(e1: Node, e2: Node, weight: Double): Link = Link(e1, e2, weight, math.sqrt((e1.x - e2.x) * (e1.x - e2.x) + (e1.y - e2.y) * (e1.y - e2.y))) }

  case class Network(nodes: Set[Node], links: Set[Link])

  object Network {

    def empty: Network = Network(Set.empty, Set.empty)

    /**
     * percolate each potential link with a zero proba
     * @param network
     * @return
     */
    def percolate(network: Network, percolationProba: Double, linkFilter: Link ⇒ Boolean)(implicit rng: Random): Network = {
      val emptyLinks = network.links.toSeq.filter(linkFilter)
      val fullLinks = network.links.toSeq.filter { l ⇒ !linkFilter(l) }
      val percolated = emptyLinks.map { case l ⇒ if (rng.nextDouble() < percolationProba) { Link(l.e1, l.e2, 1.0) } else { Link(l.e1, l.e2, 0.0) } }
      val newlinks = fullLinks ++ percolated
      val newLinksSet = newlinks.toSet

      Network(network.nodes, newLinksSet)
    }

    /**
     * network to grid
     * @param network
     * @return
     */
    def networkToGrid(network: Network, footPrintResolution: Double = 1.0, linkwidth: Double = 1.0): RasterLayerData[Double] = {
      val xmin = network.nodes.map { _.x }.min; val xmax = network.nodes.map { _.x }.max
      val ymin = network.nodes.map { _.y }.min; val ymax = network.nodes.map { _.y }.max
      def xcor(x: Double): Int = math.max(xmin.toDouble, math.min(xmax.toDouble, math.round(x))).toInt
      def ycor(y: Double): Int = math.max(ymin.toDouble, math.min(ymax.toDouble, math.round(y))).toInt
      val res: Array[Array[Double]] = (xmin to xmax by 1.0).toArray.map { case _ ⇒ (ymin to ymax by 1.0).toArray.map { case _ ⇒ 0.0 } }
      network.links.toSeq.filter { _.weight > 0.0 }.foreach {
        case l ⇒
          val i1 = l.e1.x - xmin; val j1 = l.e1.y - ymin
          val i2 = l.e2.x - xmin; val j2 = l.e2.y - ymin
          val istep = (i1 - i2) match { case x if math.abs(x) < 1e-10 ⇒ 0.0; case _ ⇒ math.cos(math.atan((j2 - j1) / (i2 - i1))) * footPrintResolution }
          val jstep = (j1 - j2) match { case x if math.abs(x) < 1e-10 ⇒ 0.0; case _ ⇒ math.sin(math.atan((j2 - j1) / (i2 - i1))) * footPrintResolution }
          val nsteps = (i1 - i2) match { case x if math.abs(x) < 1e-10 ⇒ (j2 - j1) / jstep; case _ ⇒ (i2 - i1) / istep }
          var x = l.e1.x; var y = l.e1.y
          (0.0 to nsteps by 1.0).foreach { _ ⇒
            for {
              k1 ← -(linkwidth - 1) / 2 to (linkwidth - 1) / 2 by 1.0
              k2 ← -(linkwidth - 1) / 2 to (linkwidth - 1) / 2 by 1.0
            } yield {
              res(xcor(x + k1))(ycor(y + k2)) = 1.0
            }
            x = x + istep; y = y + jstep
          }
      }
      res
    }

    /**
     * Reconstruct a network from the matrix representation of the world
     * (level of the patch, different from the generating network in the case of percolation)
     *  - put links in both senses
     * @param world
     * @return
     */
    def gridToNetwork(world: Array[Array[Double]]): Network = {
      val nodes = new ArrayBuffer[Node]()
      val links = new ArrayBuffer[Link]()
      var nodeid = 0
      for (i ← 0 until world.size; j ← 0 until world(0).size) {
        if (world(i)(j) > 0.0) {
          val currentnode = Node(nodeid, i, j); nodeid = nodeid + 1
          if (i - 1 > 0) { if (world(i - 1)(j) > 0.0) { nodeid = nodeid + 1; links.append(Link(currentnode, Node(nodeid, i - 1, j))) } }
          if (i + 1 < world.size) { if (world(i + 1)(j) > 0.0) { nodeid = nodeid + 1; links.append(Link(currentnode, Node(nodeid, i + 1, j))) } }
          if (j - 1 > 0) { if (world(i)(j - 1) > 0.0) { nodeid = nodeid + 1; links.append(Link(currentnode, Node(nodeid, i, j - 1))) } }
          if (j + 1 < world(0).size) { if (world(i)(j + 1) > 0.0) { nodeid = nodeid + 1; links.append(Link(currentnode, Node(nodeid, i, j + 1))) } }
        }
      }
      Network(links.map { _.e1 }.toSet.union(links.map { _.e2 }.toSet), links.toSet)
    }

    def connectedComponents(network: Network): Seq[Network] = {
      val nlinks = new mutable.HashMap[Node, Seq[Link]]()
      network.links.foreach { l ⇒
        if (nlinks.contains(l.e1)) { nlinks(l.e1) = nlinks(l.e1) ++ Seq(l) } else { nlinks(l.e1) = Seq(l) }
        if (nlinks.contains(l.e2)) { nlinks(l.e2) = nlinks(l.e2) ++ Seq(l) } else { nlinks(l.e2) = Seq(l) }
      }
      network.nodes.foreach { n ⇒ if (!nlinks.contains(n)) { nlinks(n) = Seq.empty } }

      //traverse using the map, using hash consing
      val totraverse = new mutable.HashMap[Node, Node]()
      network.nodes.foreach { n ⇒ totraverse.put(n, n) }
      val res = new ArrayBuffer[Network]()

      def otherend(n: Node, l: Link): Node = { if (l.e1 == n) l.e2 else l.e1 }

      def traversenode(n: Node): (Seq[Node], Seq[Link]) = {
        if (!totraverse.contains(n)) { return ((Seq.empty, nlinks(n))) } // note : a bit redundancy on links here as they are not colored
        totraverse.remove(n)
        val traversed = nlinks(n).map { l ⇒ traversenode(otherend(n, l)) }
        (Seq(n) ++ traversed.map { _._1 }.flatten, traversed.map { _._2 }.flatten)
      }

      while (totraverse.size > 0) {
        val entry = totraverse.values.head
        val currentcomponent = traversenode(entry)
        res.append(Network(currentcomponent._1.toSet, currentcomponent._2.toSet))
      }

      res
    }

    def largestConnectedComponent(network: Network): Network = {
      val components = connectedComponents(network)
      val largestComp = components.sortWith { case (n1, n2) ⇒ n1.nodes.size > n2.nodes.size }(0)
      largestComp
    }

    /**
     * Floid marshall shortest paths
     *
     * - slow in O(N^3) => DO NOT USE FOR LARGE NETWORKS
     *
     * @param network
     * @return
     */
    def allPairsShortestPath(network: Network): Map[(Node, Node), Seq[Link]] = {
      val nodenames = network.nodes.toSeq.map { _.id }
      val nodeids: Map[Int, Int] = nodenames.toSeq.zipWithIndex.toMap
      val revnodes: Map[Int, Node] = network.nodes.toSeq.zipWithIndex.map { case (n, i) ⇒ (i, n) }.toMap
      val nodes = nodeids.keySet
      val mlinks = mutable.Map[Int, Set[Int]]()
      val mlinkweights = mutable.Map[(Int, Int), Double]()
      val linksMap = mutable.Map[(Int, Int), Link]()

      for (link ← network.links) {
        if (!mlinks.keySet.contains(nodeids(link.e1.id))) mlinks(nodeids(link.e1.id)) = Set.empty[Int]
        if (!mlinks.keySet.contains(nodeids(link.e2.id))) mlinks(nodeids(link.e2.id)) = Set.empty[Int]
        // links assumed undirected in our case
        mlinks(nodeids(link.e1.id)) += nodeids(link.e2.id)
        mlinks(nodeids(link.e2.id)) += nodeids(link.e1.id)
        mlinkweights((nodeids(link.e1.id), nodeids(link.e2.id))) = link.weight
        mlinkweights((nodeids(link.e2.id), nodeids(link.e1.id))) = link.weight
        linksMap((nodeids(link.e2.id), nodeids(link.e1.id))) = link
        linksMap((nodeids(link.e1.id), nodeids(link.e2.id))) = link
      }

      val links = mlinks.toMap
      val linkweights = mlinkweights.toMap

      val n = nodes.size
      val inf = Double.MaxValue

      // Initialize distance matrix.
      val ds = Array.fill[Double](n, n)(inf)
      for (i ← 0 until n) ds(i)(i) = 0.0
      for (i ← links.keys) {
        for (j ← links(i)) {
          ds(i)(j) = linkweights((i, j))
        }
      }

      // Initialize next vertex matrix
      // O(N^3)
      val ns = Array.fill[Int](n, n)(-1)
      for (k ← 0 until n; i ← 0 until n; j ← 0 until n)
        if (ds(i)(k) != inf && ds(k)(j) != inf && ds(i)(k) + ds(k)(j) < ds(i)(j)) {
          ds(i)(j) = ds(i)(k) + ds(k)(j)
          ns(i)(j) = k
        }

      // Helper function to carve out paths from the next vertex matrix.
      def extractPath(path: ArrayBuffer[Node], pathLinks: ArrayBuffer[Link], i: Int, j: Int) {
        if (ds(i)(j) == inf) return
        val k = ns(i)(j)
        if (k != -1) {
          extractPath(path, pathLinks, i, k)
          //assert(revnodes.contains(j),"error : "+k)
          path.append(revnodes(k))
          extractPath(path, pathLinks, k, j)
        }
        else {
          assert(linksMap.contains(revnodes(i).id, revnodes(j).id), "error : " + network.links.filter { case l ⇒ l.e1.id == revnodes(i).id && l.e2.id == revnodes(j).id } + " - " + network.links.filter { case l ⇒ l.e2.id == revnodes(i).id && l.e1.id == revnodes(j).id })
          pathLinks.append(linksMap(revnodes(i).id, revnodes(j).id))
        }
      }

      // Extract paths.
      //val pss = mutable.Map[Int, Map[Int, Seq[Int]]]()
      val paths = mutable.Map[(Node, Node), Seq[Link]]()
      for (i ← 0 until n) {
        //val ps = mutable.Map[Int, Seq[Int]]()
        for (j ← 0 until n) {
          if (ds(i)(j) != inf) {
            //val p = new ArrayBuffer[Int]()
            val currentPath = new ArrayBuffer[Node]()
            val currentPathLinks = new ArrayBuffer[Link]()
            currentPath.append(revnodes(i))
            if (i != j) {
              extractPath(currentPath, currentPathLinks, i, j)
              currentPath.append(revnodes(j))
            }
            paths((revnodes(i), revnodes(j))) = currentPathLinks.toSeq
          }
        }
      }

      paths.toMap
    }

  }

  def gridToString(world: RasterLayerData[Double]): String = {
    world.map { _.map(_ match { case x if x > 0.0 ⇒ "+"; case x if x == 0 ⇒ "0"; case _ ⇒ "0" }).mkString("") }.mkString("\n")
  }

  trait GridGenerator { def generateGrid(implicit rng: Random): RasterLayerData[Double] }

  case class BlocksGridGenerator(
    /**
     * size of the grid
     */
    size: RasterDim,

    /**
 * number of blocks randomly added
 */
    blocks: Int,

    /**
 * minimal width/height of blocks
 */
    blockMinSize: Int,

    /**
 * maximal width/height of blocks
 */
    blockMaxSize: Int

  ) extends GridGenerator {

    /**
     *
     * @param rng
     * @return
     */
    override def generateGrid(implicit rng: Random): RasterLayerData[Double] = BlocksGridGenerator.blocksGrid(size, blocks, blockMinSize, blockMaxSize, rng)

  }

  object BlocksGridGenerator {

    /**
     *
     * @param size
     * @return
     */
    def blocksGrid(size: RasterDim, blocks: Int, blockMinSize: Int, blockMaxSize: Int, rng: Random): RasterLayerData[Double] = {
      val maxsize = math.max(blockMinSize, blockMaxSize)
      val minsize = math.min(blockMinSize, blockMaxSize)
      val w = size match { case Left(l) ⇒ l; case Right((w, _)) ⇒ w }
      val h = size match { case Left(l) ⇒ l; case Right((_, h)) ⇒ h }
      val vals = Array.fill(w, h)(0.0)
      for (_ ← 0 to blocks - 1) {
        val (i, j) = (rng.nextInt(w), rng.nextInt(h))
        val (ww, hh) = (minsize + rng.nextInt(maxsize - minsize + 1), minsize + rng.nextInt(maxsize - minsize + 1))
        // convention : if even, center on bottom right corner
        for (di ← 0 to ww - 1) {
          for (dj ← 0 to hh - 1) {
            val (k, l) = (i - ww / 2 + di, j - hh / 2 + dj)
            if (k >= 0 & l >= 0 & k < w & l < h) { vals(k)(l) = vals(k)(l) + 1.0 }
          }
        }

      }
      vals
    }

  }

  case class ExpMixtureGenerator(
    /**
     * size
     */
    size: RasterDim,

    /**
 * Number of centers
 */
    centers: Int,

    /**
 * Value of the exp at 0
 */
    maxValue: Double,

    /**
 * Radius of the exp kernel
 */
    kernelRadius: Double

  ) extends GridGenerator {

    override def generateGrid(implicit rng: Random): RasterLayerData[Double] = {
      def expKernel(x: Double, y: Double): Double = maxValue * exp(-sqrt(pow(x, 2.0) + pow(y, 2.0)) / kernelRadius)
      KernelMixture.kernelMixture(size, Left(centers), expKernel, rng)
    }

  }

  object KernelMixture {

    def kernelMixture(
      worldSize: RasterDim,
      centers:   Either[Int, Seq[Seq[Int]]],
      kernel:    (Double, Double) ⇒ Double,
      rng:       Random
    ): Array[Array[Double]] //Seq[Seq[(Double,(Int,Int))]]
    = {
      //val vals = Seq.fill(worldSize,worldSize)(0.0)
      val w = worldSize match { case Left(l) ⇒ l; case Right((w, _)) ⇒ w }
      val h = worldSize match { case Left(l) ⇒ l; case Right((_, h)) ⇒ h }
      val vals = Array.fill(w, h)(0.0)
      val coords = centers match {
        case Left(i)  ⇒ Seq.fill(i) { Seq(rng.nextInt(w), rng.nextInt(h)) }
        case Right(c) ⇒ c
      }
      for (i ← 0 to w - 1; j ← 0 to h - 1) {
        for (c ← coords) {
          vals(i)(j) = vals(i)(j) + kernel((i - c(0)), (j - c(1)))
        }
      }
      //array to seq
      //Seq.tabulate(w,h){(i:Int,j:Int)=>(vals(i)(j),(i,j))}
      vals
    }

  }

  case class PercolationGridGenerator(
    size:             Int,
    percolationProba: Double,
    bordPoints:       Int,
    linkwidth:        Double,
    maxIterations:    Int
  ) extends GridGenerator {

    override def generateGrid(implicit rng: Random): RasterLayerData[Double] = {
      Network.networkToGrid(PercolationNetworkGenerator(size, percolationProba, bordPoints, linkwidth, maxIterations).generateNetwork(rng), linkwidth = linkwidth).map {
        _.map { 1.0 - _ }
      }
    }

  }

  case class RandomGridGenerator(
    /**
     * The size of generated grids
     */
    size: RasterDim,

    /**
 * Number of layers
 */
    layers: Int = 1
  ) extends GridGenerator {

    override def generateGrid(implicit rng: Random): RasterLayerData[Double] = RandomGridGenerator.randomGrid(size, rng)

  }

  object RandomGridGenerator {

    def apply(size: Int): RandomGridGenerator = new RandomGridGenerator(size)

    /**
     * Random layer
     *
     * @param size
     * @param samples
     * @param rng
     * @return
     */
    def randomGrid(size: RasterDim, rng: Random): RasterLayerData[Double] = {
      size match {
        case Left(size)    ⇒ Array.fill(size, size) { rng.nextDouble() }
        case Right((w, h)) ⇒ Array.fill(w, h) { rng.nextDouble() }
      }
    }

  }

  trait NetworkGenerator { def generateNetwork(implicit rng: Random): Network }

  case class GridNetworkGenerator(
    size:          Int,
    xstep:         Int,
    ystep:         Int,
    withDiagLinks: Boolean = false
  ) extends NetworkGenerator {

    override def generateNetwork(implicit rng: Random): Network = GridNetworkGenerator.gridNetwork(xstep = xstep, ystep = ystep, size = size, diagLinks = withDiagLinks)

  }

  object GridNetworkGenerator {

    def apply(size: Int): GridNetworkGenerator = GridNetworkGenerator(size, size / 10, size / 10)

    /**
     * spatial grid network
     * @param xstep
     * @param ystep
     * @param size
     * @return
     */
    def gridNetwork(xstep: Int, ystep: Int, size: Int, diagLinks: Boolean = false): Network = {

      // create nodes
      val ycoords = (0 to size by ystep); val xcoords = (0 to size by xstep)
      val coords: Seq[(Double, Double)] = xcoords.map { case xx: Int ⇒ ycoords.map { case yy: Int ⇒ (xx.toDouble, yy.toDouble) } }.flatten
      val nodes: Seq[Seq[Node]] = coords.zipWithIndex.map { case c ⇒ Node(c._2, c._1._1, c._1._2) }.sliding(ycoords.size, ycoords.size).toSeq
      //println(nodes.size)
      //println(nodes(0).size)
      // create edges
      val edges = ArrayBuffer[Link]()
      //dirty
      for (i ← 0 to nodes.size - 1; j ← 0 to nodes(0).size - 1) {
        if (i - 1 > 0) {
          if (diagLinks && j - 1 > 0) { edges.append(Link(nodes(i)(j), nodes(i - 1)(j - 1), 0.0)) }
          edges.append(Link(nodes(i)(j), nodes(i - 1)(j), 0.0))
          if (diagLinks && j + 1 < nodes(0).size) { edges.append(Link(nodes(i)(j), nodes(i - 1)(j + 1), 0.0)) }
        }
        if (j - 1 > 0) {
          edges.append(Link(nodes(i)(j), nodes(i)(j - 1), 0.0))
        }
        if (j + 1 < nodes(0).size) {
          edges.append(Link(nodes(i)(j), nodes(i)(j + 1), 0.0))
        }
        if (i + 1 < nodes.size) {
          if (diagLinks && j - 1 > 0) { edges.append(Link(nodes(i)(j), nodes(i + 1)(j - 1), 0.0)) }
          edges.append(Link(nodes(i)(j), nodes(i + 1)(j), 0.0))
          if (diagLinks && j + 1 < nodes(0).size) { edges.append(Link(nodes(i)(j), nodes(i + 1)(j + 1), 0.0)) }
        }
      }
      //println("grid nw links = "+edges.size)
      //println("grid nw unique links = "+edges.map{case e => e.e1.id+"-"+e.e2.id+"-"+e.weight}.size)
      Network(nodes.flatten.toSet, edges.toSet)
    }

  }

  case class PercolationNetworkGenerator(
    worldSize:        Int,
    percolationProba: Double,
    bordPoints:       Int,
    linkwidth:        Double,
    maxIterations:    Int
  ) extends NetworkGenerator {
    override def generateNetwork(implicit rng: Random): Network = PercolationNetworkGenerator.bondPercolatedNetwork(worldSize, percolationProba, bordPoints, linkwidth, maxIterations)
  }

  object PercolationNetworkGenerator {

    /**
     * Basic bond percolation in an overlay network
     * (iterated until having one connected component with a specified number of points on the boundary,
     * keep the largest component at each step)
     * @param worldSize
     * @param percolationProba
     * @return
     */
    def bondPercolatedNetwork(worldSize: Int, percolationProba: Double, bordPoints: Int, linkwidth: Double, maxIterations: Int)(implicit rng: Random): Network = {
      var network = GridNetworkGenerator(worldSize).generateNetwork //.gridNetwork(worldSize/10,worldSize/10,worldSize)
      var bordConnected = 0
      val xmin = network.nodes.map { _.x }.min; val xmax = network.nodes.map { _.x }.max
      val ymin = network.nodes.map { _.y }.min; val ymax = network.nodes.map { _.y }.max
      var iteration = 0
      while (bordConnected < bordPoints && iteration < maxIterations) {
        network = Network.percolate(network, percolationProba, linkFilter = {
          l: Link ⇒
            l.weight == 0.0 && (
              (((l.e1.x != xmin) && (l.e2.x != xmin)) || ((l.e1.x == xmin) && (l.e2.x != xmin)) || ((l.e2.x == xmin) && (l.e1.x != xmin))) &&
              (((l.e1.x != xmax) && (l.e2.x != xmax)) || ((l.e1.x == xmax) && (l.e2.x != xmax)) || ((l.e2.x == xmax) && (l.e1.x != xmax))) &&
              (((l.e1.y != ymin) && (l.e2.y != ymin)) || ((l.e1.y == ymin) && (l.e2.y != ymin)) || ((l.e2.y == ymin) && (l.e1.y != ymin))) &&
              (((l.e1.y != ymax) && (l.e2.y != ymax)) || ((l.e1.y == ymax) && (l.e2.y != ymax)) || ((l.e2.y == ymax) && (l.e1.y != ymax)))
            )
        })
        val giantcomp = Network.largestConnectedComponent(Network(network.nodes, network.links.filter { _.weight > 0 }))

        val nodesOnBord = giantcomp.nodes.filter { case n ⇒ n.x == xmin || n.x == xmax || n.y == ymin || n.y == ymax }
        bordConnected = nodesOnBord.size

        iteration = iteration + 1
      }
      network
    }

  }

  case class GridGeneratorLauncher(
    generatorType: String,

    /**
 * Size of the (square) grid
 */
    gridSize: Int,

    /**
 * Random
 */
    randomDensity: Double,

    /**
 * ExpMixture
 */
    expMixtureCenters:   Int,
    expMixtureRadius:    Double,
    expMixtureThreshold: Double,

    /**
 * blocks
 */
    blocksNumber:  Int,
    blocksMinSize: Int,
    blocksMaxSize: Int,

    /**
 * percolation
 */
    percolationProba:      Double,
    percolationBordPoints: Int,
    percolationLinkWidth:  Double

  ) {

    def density(world: Array[Array[Double]]): Double = world.flatten.map { x ⇒ if (x > 0.0) 1.0 else 0.0 }.sum / world.flatten.size

    def emptyGrid(array: Array[Array[Double]]): Array[Array[Double]] = Array.tabulate(array.length)(i ⇒ Array.fill(array(i).length)(0.0))

    /**
     *
     * @param rng
     * @return
     */
    def getGrid(implicit rng: Random): RasterLayerData[Double] = {
      val world: Array[Array[Double]] = generatorType match {
        case "random"      ⇒ RandomGridGenerator(gridSize).generateGrid(rng).map { _.map { case d ⇒ if (d < randomDensity) 1.0 else 0.0 } }
        case "expMixture"  ⇒ ExpMixtureGenerator(gridSize, expMixtureCenters, 1.0, expMixtureRadius).generateGrid(rng).map { _.map { case d ⇒ if (d > expMixtureThreshold) 1.0 else 0.0 } }
        case "blocks"      ⇒ BlocksGridGenerator(gridSize, blocksNumber, blocksMinSize, blocksMaxSize).generateGrid(rng).map { _.map { case d ⇒ if (d > 0.0) 1.0 else 0.0 } }
        case "percolation" ⇒ PercolationGridGenerator(gridSize, percolationProba, percolationBordPoints, percolationLinkWidth, 10000).generateGrid(rng)
        case _             ⇒ { assert(false, "Error : the requested generator does not exist"); Array.empty }
      }

      if (density(world) > 0.8) emptyGrid(world) else world
    }

  }

  case class Morphology(
    height:            Double,
    width:             Double,
    area:              Double,
    moran:             Double,
    avgDistance:       Double,
    density:           Double,
    components:        Double,
    avgDetour:         Double,
    avgBlockArea:      Double,
    avgComponentArea:  Double,
    fullDilationSteps: Double,
    fullErosionSteps:  Double,
    fullClosingSteps:  Double,
    fullOpeningSteps:  Double
  ) {

    def toTuple: (Double, Double, Double, Double, Double, Double, Double, Double, Double, Double, Double, Double) =
      (height, width, area, moran, avgDistance, density, components, avgDetour, avgBlockArea, avgComponentArea, fullDilationSteps, fullErosionSteps)

    def toArray(n: Int = -1): Array[Double] = {
      n match {
        case -1                ⇒ toTuple.productIterator.toArray.map { _.asInstanceOf[Double] }
        case 0                 ⇒ Array.empty
        case nn: Int if nn > 0 ⇒ toTuple.productIterator.toArray.map { _.asInstanceOf[Double] }.takeRight(nn + 3).take(nn)
      }
    }

  }

  object Morphology {

    def apply(grid: RasterLayerData[Double]): Morphology = {
      val cachedNetwork = Network.gridToNetwork(grid)
      Morphology(
        grid.size, grid(0).size,
        grid.flatten.sum,
        moranDirect(grid),
        distanceMeanDirect(grid),
        density(grid),
        components(grid, Some(cachedNetwork)),
        0.0, //avgDetour(grid, Some(cachedNetwork)),
        avgBlockArea(grid, Some(cachedNetwork)),
        avgComponentArea(grid),
        fullDilationSteps(grid),
        fullErosionSteps(grid),
        // FIXME opening and closing are interesting as profile of mask radius (always one or two with the smaller mask)
        //  : too complicated/costly to compute
        0.0, //fullClosingSteps(grid),
        0.0 //fullOpeningSteps(grid)
      )
    }

    def components(world: Array[Array[Double]], cachedNetwork: Option[Network] = None): Double = {
      val network = cachedNetwork match { case None ⇒ Network.gridToNetwork(world); case n ⇒ n.get }
      val components = Network.connectedComponents(network)
      //println("components = "+components.size)
      components.size
    }

    /**
     * average block area
     * @param world
     * @return
     */
    def avgBlockArea(world: Array[Array[Double]], cachedNetwork: Option[Network] = None): Double = {
      //val inversedNetwork = Network.gridToNetwork(world.map{_.map{case x => 1.0 - x}})
      val network = cachedNetwork match { case None ⇒ Network.gridToNetwork(world); case n ⇒ n.get }
      val components = Network.connectedComponents(network)
      val avgblockarea = components.size match { case n if n == 0 ⇒ 0.0; case n ⇒ components.map { _.nodes.size }.sum / components.size }
      //println("avgblockarea = "+avgblockarea)
      avgblockarea
    }

    /**
     * avg component area
     * @param world
     * @return
     */
    def avgComponentArea(world: Array[Array[Double]]): Double = {
      val inversedNetwork = Network.gridToNetwork(world.map { _.map { case x ⇒ 1.0 - x } })
      val components = Network.connectedComponents(inversedNetwork)
      //println("avgblockarea = "+avgblockarea)
      if (components.size > 0) {
        components.map { _.nodes.size }.sum / components.size
      }
      else 0.0
    }

    /**
     * average detour compared to euclidian
     * @param world
     * @param cachedNetwork
     * @param sampledPoints
     * @return
     */
    /*def avgDetour(world: Array[Array[Double]],cachedNetwork: Option[Network] = None,sampledPoints: Int=50): Double = {
      if(world.flatten.sum==world.map{_.length}.sum){return(0.0)}
      val network = cachedNetwork match {case None => Network.gridToNetwork(world);case n => n.get}
      // too costly to do all shortest paths => sample
      //val shortestPaths = Network.allPairsShortestPath(network)
      //val avgdetour = shortestPaths.values.map{_.map{_.weight}.sum}.zip(shortestPaths.keys.map{case (n1,n2)=> math.sqrt((n1.x-n2.x)*(n1.x-n2.x)+(n1.y-n2.y)*(n1.y-n2.y))}).map{case (dn,de)=>dn/de}.sum/shortestPaths.size
      //println("avgdetour = "+avgdetour)
      // should sample points within connected components
      val sampled = network.nodes.toSeq.take(sampledPoints)
      val paths = Network.shortestPathsScalagraph(network,sampled)

      val avgdetour = paths.filter{!_._2._2.isInfinite}.map{
        case (_,(nodes,d))=>
          val (n1,n2) = (nodes(0),nodes.last)
          val de = math.sqrt((n1.x-n2.x)*(n1.x-n2.x)+(n1.y-n2.y)*(n1.y-n2.y))
          //println(d,de)
          d/de
      }.filter{!_.isNaN}.filter{!_.isInfinite}.sum / paths.size
      avgdetour
    }*/

    /**
     * Global density
     * @param world
     * @return
     */
    def density(world: Array[Array[Double]]): Double = world.flatten.map { x ⇒ if (x > 0.0) 1.0 else 0.0 }.sum / world.flatten.size

    /**
     * Distance kernel
     *
     * @param n
     * @return
     */
    def distanceMatrix(n: Int): Array[Array[Double]] = {
      Array.tabulate(n, n) { (i, j) ⇒ math.sqrt((i - n / 2) * (i - n / 2) + (j - n / 2) * (j - n / 2)) }
    }

    /**
     * Default spatial weights for Moran
     * @param n
     * @return
     */
    def spatialWeights(matrix: Array[Array[Double]]): Array[Array[Double]] = {
      val n: Int = 2 * matrix.length - 1
      Array.tabulate(n, n) { (i, j) ⇒ if (i == n / 2 && j == n / 2) 0.0 else 1 / math.sqrt((i - n / 2) * (i - n / 2) + (j - n / 2) * (j - n / 2)) }
    }

    /**
     * Average distance between individuals in the population
     * (direct computation)
     *
     * @param matrix
     * @return
     */
    def distanceMeanDirect(matrix: Array[Array[Double]]): Double = {

      def totalQuantity = matrix.flatten.sum

      def numerator =
        (for {
          (c1, p1) ← zipWithPosition(matrix)
          (c2, p2) ← zipWithPosition(matrix)
        } yield distance(p1, p2) * c1 * c2).sum

      def normalisation = matrix.length / math.sqrt(math.Pi)

      if (totalQuantity == 0.0 || normalisation == 0.0) return (0.0)

      (numerator / (totalQuantity * totalQuantity)) / normalisation
    }

    def distance(p1: (Int, Int), p2: (Int, Int)): Double = {
      val (i1, j1) = p1
      val (i2, j2) = p2
      val a = i2 - i1
      val b = j2 - j1
      math.sqrt(a * a + b * b)
    }

    def zipWithPosition(m: Array[Array[Double]]): Seq[(Double, (Int, Int))] = {
      for {
        (row, i) ← m.zipWithIndex
        (content, j) ← row.zipWithIndex
      } yield (content, (i, j))
    }

    /**
     * Direct computation of Moran index (in O(N^4))
     * @param matrix
     * @return
     */
    def moranDirect(matrix: Array[Array[Double]]): Double = {
      def flatCells = matrix.flatten
      val totalPop = flatCells.sum
      val averagePop = totalPop / matrix.flatten.length

      def vals =
        for {
          (c1, p1) ← zipWithPosition(matrix)
          (c2, p2) ← zipWithPosition(matrix)
        } yield (decay(p1, p2) * (c1 - averagePop) * (c2 - averagePop), decay(p1, p2))

      def numerator: Double = vals.map { case (n, _) ⇒ n }.sum
      def totalWeight: Double = vals.map { case (_, w) ⇒ w }.sum

      def denominator =
        flatCells.map {
          p ⇒
            if (p == 0) 0
            else math.pow(p - averagePop.toDouble, 2)
        }.sum

      if (denominator == 0) 0
      else (matrix.flatten.length / totalWeight) * (numerator / denominator)
    }

    def decay(p1: (Int, Int), p2: (Int, Int)) = {
      if (p1 == p2) 0.0
      else 1 / distance(p1, p2)
    }

    def dilation(
      matrix: Array[Array[Double]],
      convol: (Array[Array[Double]], Array[Array[Double]], (Double ⇒ Double)) ⇒ Array[Array[Double]] = convolutionDirect): Array[Array[Double]] =
      convol(matrix, Array(Array(0.0, 1.0, 0.0), Array(1.0, 1.0, 1.0), Array(0.0, 1.0, 0.0)), { case d ⇒ if (d > 0.0) 1.0 else 0.0 })

    def erosion(
      matrix: Array[Array[Double]],
      convol: (Array[Array[Double]], Array[Array[Double]], (Double ⇒ Double)) ⇒ Array[Array[Double]] = convolutionDirect): Array[Array[Double]] = {
      val mask = Array(Array(0.0, 1.0, 0.0), Array(1.0, 1.0, 1.0), Array(0.0, 1.0, 0.0))
      convol(
        matrix,
        mask,
        { case d ⇒ if (d == mask.flatten.sum) 1.0 else 0.0 }
      )
    }

    /**
     * Number of steps to fully close the image (morpho maths)
     *
     * @param matrix
     * @return
     */
    def fullDilationSteps(
      matrix: Array[Array[Double]],
      convol: (Array[Array[Double]], Array[Array[Double]], (Double ⇒ Double)) ⇒ Array[Array[Double]] = convolutionDirect
    ): Double = {
      var steps = 0
      var complete = false
      var currentworld = matrix
      //if(matrix.flatten.sum==0){return(Double.PositiveInfinity)}
      if (matrix.flatten.sum == 0) { return (0.0) }
      while (!complete) {
        //println("dilating "+steps+" ; "+currentworld.flatten.sum+"/"+currentworld.flatten.length+" ; "+currentworld.length+" - "+currentworld(0).length)
        //println(Grid.gridToString(currentworld)+"\n\n")
        currentworld = dilation(currentworld, convol)
        complete = currentworld.flatten.sum == currentworld.flatten.length
        steps = steps + 1
      }
      steps
    }

    /**
     * Number of steps to fully erode the image
     * @param matrix
     * @return
     */
    def fullErosionSteps(
      matrix: Array[Array[Double]],
      convol: (Array[Array[Double]], Array[Array[Double]], (Double ⇒ Double)) ⇒ Array[Array[Double]] = convolutionDirect
    ): Double = {
      var steps = 0
      var complete = false
      var currentworld = matrix
      //if(matrix.flatten.sum==matrix.flatten.length){return(Double.PositiveInfinity)}
      if (matrix.flatten.sum == matrix.flatten.length) { return (0.0) }
      while (!complete) {
        //println("eroding "+steps+" ; "+currentworld.flatten.sum+"/"+currentworld.flatten.length)
        //println(Grid.gridToString(currentworld)+"\n\n")
        currentworld = erosion(currentworld, convol)
        complete = currentworld.flatten.sum == 0
        steps = steps + 1
      }
      steps
    }

    def convolutionDirect(matrix: Array[Array[Double]], mask: Array[Array[Double]],
                          //operator: Array[Double]=>Double = {case a => if(a.filter(_>0.0).size>0)1.0 else 0.0})
                          filter: Double ⇒ Double = { case d ⇒ if (d > 0.0) 1.0 else 0.0 }): Array[Array[Double]] = {
      assert(mask.length % 2 == 1 && mask(0).length % 2 == 1, "mask should be of uneven size")
      val sizes = matrix.map(_.length); assert(sizes.max == sizes.min, "array should be rectangular")
      val masksizes = mask.map(_.length); assert(masksizes.max == masksizes.min, "mask should be rectangular")
      val (paddingx, paddingy) = ((mask.length - 1) / 2, (mask(0).length - 1) / 2)
      val padded = Array.tabulate(matrix.length + 2 * paddingx, matrix(0).length + 2 * paddingy) {
        case (i, j) if i < paddingx || i >= (matrix.length + paddingx) || j < paddingy || j >= (matrix(0).length + paddingy) ⇒ 0.0
        case (i, j) ⇒ matrix(i - paddingx)(j - paddingy)
      }
      val res = Array.fill(matrix.length + 2 * paddingx, matrix(0).length + 2 * paddingy)(0.0)
      for (i ← paddingx until (res.length - paddingx); j ← paddingy until (res(0).length - paddingy)) {
        val masked = Array.fill(mask.size, mask(0).size)(0.0)
        for (k ← -paddingx to paddingx; l ← -paddingy to paddingy) {
          //assert(i+k<matrix.length&j+l<matrix(0).length,"size : "+i+" "+j+" "+k+" "+" "+l+" for a matrix of size "+matrix.length+";"+matrix(0).length)
          masked(k + paddingx)(l + paddingy) = padded(i + k)(j + l) * mask(k + paddingx)(l + paddingy)
        }
        res(i)(j) = filter(masked.flatten.sum)
      }
      //res.zip(matrix).map{case (row,initrow) => row.take(initrow.length + paddingy).takeRight(initrow.length)}.take(matrix.length+paddingx).takeRight(matrix.length)
      res.map { case row ⇒ row.slice(paddingy, row.length - paddingy) }.slice(paddingx, res.length - paddingx)
    }

  }

}

