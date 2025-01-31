package org.openmole.core.context

import scala.collection.Iterable

object CompactedContext:
  org.apache.fury.logging.LoggerFactory.disableLogging()

  def empty: CompactedContext = IArray.empty[Any]

  def compactRef(variables: Iterable[Variable[?]]) =
    val middle = variables.size
    val result = Array.ofDim[Any](middle * 2)

    for
      (v, i) <- variables.zipWithIndex
    do
      result(i) = v.asInstanceOf[Variable[Any]].prototype
      result(middle + i) = v.value

    IArray.unsafeFromArray(result)

  inline val minCompressionSize = 10240

  def compactBytes(variables: Iterable[Variable[?]]): CompactedContext =
    val middle = variables.size

    val vals = Array.ofDim[Any](middle)
    val values = Array.ofDim[Any](middle)

    for
      (v, i) <- variables.zipWithIndex
    do
      vals(i) = v.asInstanceOf[Variable[Any]].prototype
      values(i) = v.value

    val serialized = fury.serialize(values)
    val compacted =
      if serialized.length <= minCompressionSize
      then serialized
      else compressArray(serialized)

    IArray[Any](vals, compacted)


  def compressArray(bytes: Array[Byte]): Array[Byte] =
    import java.io.ByteArrayOutputStream
    import java.util.zip.GZIPOutputStream

    if (bytes == null || bytes.length == 0) return new Array[Byte](0)

    val out = new ByteArrayOutputStream()
    val gzip = new GZIPOutputStream(out)
    try gzip.write(bytes)
    finally gzip.close()

    out.toByteArray

  lazy val fury =
    import org.apache.fury.*
    import org.apache.fury.config.*
    import org.apache.fury.serializer.scala.*

    val f =
      Fury.builder().withLanguage(Language.JAVA)
        .withClassLoader(classOf[Fury].getClassLoader)
        .withScalaOptimizationEnabled(true)
        .requireClassRegistration(false)
        .withRefTracking(true)
        .suppressClassRegistrationWarnings(true)
        .withStringCompressed(true)
        .withNumberCompressed(true)
        .buildThreadSafeFury()

    ScalaSerializers.registerSerializers(f)

    f

  def expandVariablesRef(compacted: Array[Any]) =
    val middle = compacted.length / 2
    (0 until middle).map: i =>
      Variable(compacted(i).asInstanceOf[Val[Any]], compacted(middle + i))

  def decompress(bytes: Array[Byte]): Array[Byte] =
    import java.io.*
    import java.util.zip.GZIPInputStream

    if (bytes == null || bytes.length == 0) return new Array[Byte](0)
    val ungzip = new GZIPInputStream(new ByteArrayInputStream(bytes))
    try
      val out = new ByteArrayOutputStream()
      val data = new Array[Byte](8192)
      var nRead = 0
      while
        nRead = ungzip.read(data)
        nRead != -1
      do out.write(data, 0, nRead)

      out.toByteArray
    finally ungzip.close()

  def expandVariablesBytes(compacted: CompactedContext) =
    val values =
      val rawArray = compacted(1).asInstanceOf[Array[Byte]]
      val values =
        if isGzip(rawArray)
        then decompress(rawArray)
        else rawArray

      fury.deserialize(values).asInstanceOf[Array[Any]]

    val vals = compacted(0).asInstanceOf[Array[Any]]

    (vals zip values).map: (v, x) =>
      Variable(v.asInstanceOf[Val[Any]], x)

  lazy val gzipHeader = CompactedContext.compressArray(Array(0)).take(10)
  def isGzip(a: Array[Byte]) = a.startsWith(gzipHeader)

  inline def compact = compactBytes
  def compact(context: Context): CompactedContext =
    compact(context.variables.toSeq.map(_._2))

  inline def expandVariables = expandVariablesBytes
  def expand(compacted: CompactedContext): Context = Context(expandVariables(compacted)*)

  inline def merge = mergeBytes
  def mergeBytes(c1: CompactedContext, c2: CompactedContext): CompactedContext =
    compact:
      Context(expandVariables(c1) ++ expandVariables(c2) *)

  def megeRef(c1: CompactedContext, c2: CompactedContext): CompactedContext =
    val (p1, v1) = c1.splitAt(c1.size / 2)
    val (p2, v2) = c2.splitAt(c2.size / 2)
    (p1 ++ p2 ++ v1 ++ v2)

opaque type CompactedContext = IArray[Any]
