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

  def compactVariablesBytes(variables: Iterable[Variable[?]]): CompactedContext =
    val size = variables.size
    val vals = Array.ofDim[Any](size)
    val values = Array.ofDim[Any](size)

    for
      (v, i) <- variables.zipWithIndex
    do
      vals(i) = v.asInstanceOf[Variable[Any]].prototype
      values(i) = v.value

    val serialized = fury.serialize(values)

    if serialized.length <= minCompressionSize
    then compactRef(variables)
    else IArray[Any](vals, compressArray(serialized))

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

  def expandVariablesRef(compacted: CompactedContext) =
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

  def expandVariablesBytes(compacted: CompactedContext): Seq[Variable[?]] =
    def expandBytes(compactedContext: CompactedContext) =
      val values =
        def rawArray = compacted(1).asInstanceOf[Array[Byte]]
        val values = decompress(rawArray)
        fury.deserialize(values).asInstanceOf[Array[Any]]

      val vals = compacted(0).asInstanceOf[Array[Any]]

      (vals zip values).map: (v, x) =>
        Variable(v.asInstanceOf[Val[Any]], x)

    if compacted.isEmpty
    then Seq.empty
    else
      if isCompactedRef(compacted)
      then expandVariablesRef(compacted)
      else expandBytes(compacted)


  def isCompactedRef(compacted: CompactedContext) = classOf[Val[?]].isAssignableFrom(compacted.head.getClass)

  inline def compactVariables = compactRef
  def compact(context: Context): CompactedContext =
    compactVariables(context.variables.toSeq.map(_._2))

  def expandVariables(compacted: CompactedContext): Seq[Variable[?]] =
    expandVariablesRef(compacted)


  def expand(compacted: CompactedContext): Context = Context(expandVariables(compacted)*)

  def merge(c1: CompactedContext, c2: CompactedContext): CompactedContext =
    compact:
      Context(expandVariables(c1) ++ expandVariables(c2)*)

  def mergeRef(c1: CompactedContext, c2: CompactedContext): CompactedContext =
    val (p1, v1) = c1.splitAt(c1.size / 2)
    val (p2, v2) = c2.splitAt(c2.size / 2)
    p1 ++ p2 ++ v1 ++ v2

opaque type CompactedContext = IArray[Any]

object CompactedArray:
  import CompactedContext.*
  lazy val gzipHeader = CompactedContext.compressArray(Array(0)).take(10)

  def isGzip(a: IArray[Byte]) = a.startsWith(gzipHeader)

  def compact(values: Seq[Any]): CompactedArray =
    val serialized = fury.serialize(values)

    if serialized.length <= minCompressionSize
    then IArray.unsafeFromArray(serialized)
    else IArray.unsafeFromArray(compressArray(serialized))

  def expand(compactedArray: CompactedArray) =
    if isGzip(compactedArray)
    then fury.deserialize(decompress(IArray.wrapByteIArray(compactedArray).unsafeArray))
    else fury.deserialize(IArray.wrapByteIArray(compactedArray).unsafeArray)

opaque type CompactedArray = IArray[Byte]