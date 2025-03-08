package org.openmole.rest.server.mole

/*import java.io._
import javax.sql.rowset.serial.{ SerialBlob, SerialClob }

import com.ice.tar.{ Tar, TarInputStream }
import com.thoughtworks.xstream.mapper.CannotResolveClassException
import org.openmole.core.eventdispatcher.{ EventDispatcher, EventListener }
import org.openmole.core.serializer.SerialiserService
import org.openmole.core.tools.io.FromString
import org.openmole.core.workflow.data._
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.validation.DataflowProblem.{ MissingInput, MissingSourceInput }
import org.openmole.core.workflow.validation.Validation
import org.openmole.core.workspace.Workspace
import org.openmole.rest.server.cache.{ Cache, Status, Stats }
import org.openmole.rest.server.cache.Status.Stopped
import org.openmole.rest.server.db.SlickDB
import org.openmole.web.cache.Status
import org.openmole.web.db.MoleStats
import org.openmole.web.db.tables.MoleData
import org.scalatra.ScalatraBase
import org.scalatra.servlet.FileItem
import resource._

import scala.io.{ Codec, Source }
import scala.reflect.ClassTag
import scala.slick.driver.H2Driver.simple._
import scala.util.Try*/

/*trait MoleHandling { self: ScalatraBase =>

  implicit val codec = Codec.UTF8

  val database: SlickDB
  val db = database.db
  val cache = new Cache(database)

  private val listener: EventListener[MoleExecution] = new JobEventListener(this)
  private val mStatusListener = new MoleStatusListener(this)

  def csvToContext(csv: FileItem)(partialMoleExecution: PartialMoleExecution) = {
    val is = Source.fromInputStream(csv.getInputStream)
    val csvData =
      try {
        val regex = """(.*),(.*)""".r
        is.getLines().map{
          _ match {
            case regex(name: String, data: String) => name -> data
            case _ => throw new RuntimeException("Invalidly formatted csv file")
          }
        }.toMap
      } finally is.close
    reifyCSV(partialMoleExecution, csvData)
  }

  def reifyCSV(mole: PartialMoleExecution, csvData: Map[String, String]) = {
    def fromString[T: FromString](s: String) = implicitly[FromString[T]].from(s)

    def createVariable[T: FromString](mI: MissingInput) =
      csvData get mI.data.prototype.name map (d => Variable[T](mI.data.prototype.asInstanceOf[Prototype[T]], fromString[T](d)))

    val a = Validation(mole.mole, sources = mole.sources, hooks = mole.hooks)
    val mIS = a.map(_ match {
      case x: MissingInput       => x
      case y: MissingSourceInput => MissingInput(y.slot, y.input)
      case error                 => throw new RuntimeException(s"Malformed partial mole: $error")
    })

    val c = mIS.map {
      mI =>
        mI.data.prototype.`type`.runtimeClass match {
          case t if t.equals(classOf[Int])    => createVariable[Int](mI)
          case t if t.equals(classOf[Double]) => createVariable[Double](mI)
          case t if t.equals(classOf[Float])  => createVariable[Float](mI)
          case t if t.equals(classOf[BigInt]) => createVariable[BigInt](mI)
          case t if t.equals(classOf[String]) => createVariable[String](mI)
          case t if t.equals(classOf[File])   => createVariable[File](mI)
          case _                              => throw new RuntimeException(s"The missing parameter type: ${mI.data.prototype.`type`} is not known to the reification system.")
        }
    }

    if (!mIS.isEmpty && c.isEmpty) throw new RuntimeException("No parameters given.")

    Context(c.map(_.getOrElse(throw new RuntimeException("CSV file does not have data on all missing variables."))))
  }

  private def createMoleExecution(pMole: PartialMoleExecution, context: Option[Context], encapsulated: Boolean, outputDir: Option[File] = None) = {
    val path: Option[File] = outputDir orElse (if (encapsulated) Some(Workspace.newDir) else None)
    val executionContext = ExecutionContext(new PrintStream(new File(path.getOrElse(".") + "/out")), path)
    val mole = pMole.toExecution(context.getOrElse(Context.empty))(executionContext = executionContext)

    EventDispatcher.listen(mole, listener, classOf[MoleExecution.JobStatusChanged])
    EventDispatcher.listen(mole, listener, classOf[MoleExecution.JobCreated])
    EventDispatcher.listen(mole, mStatusListener, classOf[MoleExecution.Starting])
    EventDispatcher.listen(mole, mStatusListener, classOf[MoleExecution.Finished])
    (mole, path)
  }

  def registerMole(
    mole: PartialMoleExecution,
    context: Option[PartialMoleExecution => Context],
    encapsulate: Boolean,
    packed: Boolean) = {

    val moleBinary = {
      val is = Source.fromInputStream(mole.getInputStream)
      try is.toArray
      finally is.close
    }

    val contextChar = {
      val ctx = context()
    }

  }

  def deserializePartialMole(mole: FileItem, packed: Boolean, directory: File): PartialMoleExecution =
    packed match {
      case true =>
        val is = new TarInputStream(mole.getInputStream)
        try SerialiserService.deserialiseAndExtractFiles[PartialMoleExecution](is, directory)
        finally is.close
      case false =>
        val is = mole.getInputStream
        try SerialiserService.deserialise[PartialMoleExecution](is)
        finally is.close
    }



  def createMole(
    mole: FileItem,
    context: Option[PartialMoleExecution => Context],
    encapsulate: Boolean,
    packed: Boolean): Try[MoleExecution] = Try {

    val moleStream = moleBinary map (b => new ByteArrayInputStream(b map (_.toByte)))

    val (moleExec, genPath) = if (pack)
    moleExec match {
      case Left(pEx) => {
        val ctxt = reifyCSV(pEx, csvData)

        val clob = new SerialBlob(moleBinary)

        val ctxtClob = new SerialClob(SerialiserService.serialise(ctxt).toCharArray)

        val outputBlob = new SerialBlob(Array[Byte]())
        //val id = UUID.randomUUID().toString

        val (me, path) = createMoleExecution(pEx, ctxt, encapsulate, genPath)
        db withSession { implicit session =>
          MoleData.instance.insert((me.id, name, Stopped.toString, clob, ctxtClob, encapsulate, pack, outputBlob))
          MoleStats.instance.insert((me.id, 0, 0, 0, 0, 0))
        }
        cache.cacheMoleExecution(me, path, me.id)
        Right(me)
      }
      case Right(error) => Left(error)
    }
  }

  def getMoleKeys = cache.getMoleKeys

  def getMole(key: String): Option[MoleExecution] = { //TODO: move a large part of this to cache
    lazy val mole: Option[MoleExecution] = db withSession { implicit session =>
      val row = MoleData.instance filter (_.id === key)
      val molePack = (row map (_.molePackage)).list.headOption.getOrElse(false)
      val outputDir = if (molePack) Some(Workspace.newDir) else None
      val moleDeserialiser: InputStream => PartialMoleExecution = outputDir map (dir =>
        (in: InputStream) => SerialiserService.deserialiseAndExtractFiles[PartialMoleExecution](new TarInputStream(in), dir)
      ) getOrElse (SerialiserService.deserialise[PartialMoleExecution](_))

      val r = (row map (r => (r.clobbedMole, r.clobbedContext, r.encapsulated))).list.headOption map {
        case (pMClob, ctxtClob, e) => (moleDeserialiser(pMClob.getAsciiStream), SerialiserService.deserialise[Context](ctxtClob.getAsciiStream), e)
      }

      r map Function.tupled(createMoleExecution(_, _, _, outputDir)) map Function.tupled(cache.cacheMoleExecution(_, _, key))
    }

    cache.getOrElseUpdateMoleExecution(key, mole)
  }

  def getMoleResult(key: String) = cache.getMoleResult(key)

  def getMoleStats(mole: MoleExecution) = cache.getMoleStats(cache.getCacheId(mole))

  def getMoleStats(key: String) = cache.getMoleStats(key)

  def startMole(key: String) { getMole(key) foreach (_.start) }

  def deleteMole(key: String) = {
    val ret = getMole(key) map (_.cancel)
    cache.deleteMole(key)
    ret
  }

  def getWebStats(key: String) = {
    val statNames = List("Ready", "Running", "Completed", "Failed", "Cancelled")
    val stats = getMoleStats(key)
    statNames zip stats.getJobStatsAsSeq
  }

  def updateStats(mole: MoleExecution, stats: Stats) {
    cache.updateStats(mole, stats)
  }

  //Called automatically when execution is complete.
  def decacheMole(mole: MoleExecution) = cache.decacheMole(mole)

  //todo fix to remove decached moles
  def setStatus(mole: MoleExecution, status: Status) = cache.setStatus(mole, status)

  def isEncapsulated(key: String): Boolean = db withSession { implicit session =>
    (for { m ← MoleData.instance if m.id === key } yield m.encapsulated).list.forall(b => b)
  }

  //TODO - FRAGILE
  def storeResultBlob(exec: MoleExecution) = {
    val mPath = cache.getCapsule(exec)
    for (path ← mPath) {
      val outFile = new File(Workspace.newFile.toString + ".tar")
      outFile.createNewFile()

        Tar.createDirectoryTar(path, outFile)

        for (tis ← managed(Source.fromFile(outFile)(Codec.ISO8859))) {
          val arr = tis.iter.toArray.map(_.toByte)
          cache.storeResultBlob(exec, new SerialBlob(arr))
        }
    }
  }
}

*/ 