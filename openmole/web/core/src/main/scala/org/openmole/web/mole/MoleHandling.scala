package org.openmole.web.mole

import org.openmole.core.eventdispatcher.{ EventDispatcher, EventListener }
import org.openmole.core.tools.io.FromString
import org.openmole.core.workflow.execution.local.LocalEnvironment
import org.openmole.core.workflow.validation.{ Validation, DataflowProblem }
import org.openmole.core.workspace.Workspace

import scala.reflect.ClassTag
import scala.io.{ Codec, Source }
import java.io._
import org.openmole.core.serializer.SerialiserService
import com.thoughtworks.xstream.mapper.CannotResolveClassException
import org.openmole.core.workflow.mole._
import org.openmole.core.workflow.data._
import javax.sql.rowset.serial.{ SerialBlob, SerialClob }
import akka.actor.ActorSystem

import resource._

import slick.driver.H2Driver.simple._
import com.ice.tar.{ TarInputStream, Tar }
import scala.Some
import DataflowProblem.MissingSourceInput
import DataflowProblem.MissingInput
import org.scalatra.ScalatraBase
import org.openmole.web.db.tables.{ MoleData, MoleStats }
import org.openmole.web.cache.{ Stats, Status, Cache, DataHandler }
import org.openmole.web.db.{ SlickDB }

trait MoleHandling { self: ScalatraBase ⇒
  def system: ActorSystem
  val database: SlickDB
  val db = database.db
  val cache = new Cache(system, database)

  protected implicit def executor: concurrent.ExecutionContext = system.dispatcher

  private val listener: EventListener[MoleExecution] = new JobEventListener(this)
  private val mStatusListener = new MoleStatusListener(this)

  (cache.getUnfinishedMoleKeys map getMole).flatten foreach (_.start)

  def getStatus(moleId: String): String =
    db withSession { implicit session ⇒
      (for (m ← MoleData.instance if m.id === moleId) yield m.state).list.headOption.getOrElse("Doesn't Exist")
    }

  private def processXMLFile[A: ClassTag](is: Option[InputStream]): Either[A, String] = is match {
    case Some(stream) ⇒
      try {
        val ret = SerialiserService.deserialise[A](stream)
        Left(ret)
      }
      catch { //TODO: Make error messages more verbose
        case e: CannotResolveClassException ⇒ Right("The uploaded xml was not a valid serialized object.")
        case c: ClassCastException          ⇒ Right("Blargh")
        case e: Exception                   ⇒ Right("Could not parse the given mole")
      }
    case None ⇒ Right("No data was uploaded..")
  }

  private def processPack(is: Option[InputStream]): (Either[PartialMoleExecution, String], Option[File]) = is match {
    case Some(stream) ⇒
      try {
        val p = Workspace.newDir // Todo: make sure that the encapsulate flag is implicit for packs
        val ret = managed(new TarInputStream(stream)) acquireAndGet { SerialiserService.deserialiseAndExtractFiles[PartialMoleExecution](_, p) }
        Left(ret) -> Some(p)
      }
      catch {
        case e: CannotResolveClassException ⇒ Right("The uploaded pack was not a valid tar file") -> None
        case c: ClassCastException          ⇒ Right("Blargh") -> None
        case e: Exception                   ⇒ Right("Could not parse the given mole") -> None
      }
    case None ⇒ Right("No data was uploaded") -> None
  }

  private def reifyCSV(mole: PartialMoleExecution, csvData: Map[String, String]) = {
    def fromString[T: FromString](s: String) = {
      implicitly[FromString[T]].from(s)
    }

    def createVariable[T: FromString](mI: MissingInput) = csvData get mI.data.prototype.name map (d ⇒ Variable[T](mI.data.prototype.asInstanceOf[Prototype[T]], fromString[T](d)))

    val a = Validation(mole.mole, sources = mole.sources, hooks = mole.hooks)
    val mIS = a.map(_ match {
      case x: MissingInput       ⇒ x
      case y: MissingSourceInput ⇒ MissingInput(y.slot, y.input)
      case error                 ⇒ throw new Exception(s"Malformed partial mole: $error")
    })

    val c = mIS.map {
      mI ⇒
        mI.data.prototype.`type`.runtimeClass match {
          case t if t.equals(classOf[Int])    ⇒ createVariable[Int](mI)
          case t if t.equals(classOf[Double]) ⇒ createVariable[Double](mI)
          case t if t.equals(classOf[Float])  ⇒ createVariable[Float](mI)
          case t if t.equals(classOf[BigInt]) ⇒ createVariable[BigInt](mI)
          case t if t.equals(classOf[String]) ⇒ createVariable[String](mI)
          case t if t.equals(classOf[File])   ⇒ createVariable[File](mI)
          case _                              ⇒ throw new Exception(s"The missing parameter type: ${mI.data.prototype.`type`} is not known to the reification system.")
        }
    }

    if (!mIS.isEmpty && c.isEmpty) throw new Exception("No parameters given")

    Context(c.map(_.getOrElse(throw new Exception("CSV file does not have data on all missing variables"))))
  }

  private def createMoleExecution(pMole: PartialMoleExecution, context: Context, encapsulated: Boolean, mPath: Option[File] = None) = {
    val path: Option[File] = mPath orElse (if (encapsulated) Some(Workspace.newDir("")) else None)
    val executionContext = ExecutionContext(new PrintStream(new File(path.getOrElse(".") + "/out")), path)
    val mole = pMole.toExecution(context)(executionContext = executionContext)

    EventDispatcher.listen(mole, listener, classOf[MoleExecution.JobStatusChanged])
    EventDispatcher.listen(mole, listener, classOf[MoleExecution.JobCreated])
    EventDispatcher.listen(mole, mStatusListener, classOf[MoleExecution.Starting])
    EventDispatcher.listen(mole, mStatusListener, classOf[MoleExecution.Finished])
    (mole, path)
  }

  def createMole(moleInput: ⇒ Option[InputStream], csvInput: ⇒ Option[InputStream], encapsulate: Boolean = false, pack: Boolean = false, name: String = ""): Either[String, MoleExecution] = {
    val r = csvInput map Source.fromInputStream

    val regex = """(.*),(.*)""".r
    lazy val csvData = r.map(_.getLines().map(_ match {
      case regex(name: String, data: String) ⇒ name -> data
      case _                                 ⇒ throw new Exception("Invalidly formatted csv file")
    }).toMap) getOrElse Map()

    val moleBinary = moleInput map { str ⇒ { val arr = Source.fromInputStream(str)(Codec.ISO8859).toArray; str.close(); arr } }

    val moleStream = moleBinary map (b ⇒ new ByteArrayInputStream(b map (_.toByte)))

    val (moleExec, genPath) = if (pack) processPack(moleStream) else (processXMLFile[PartialMoleExecution](moleStream), None)

    moleExec match {
      case Left(pEx) ⇒ {
        val ctxt = reifyCSV(pEx, csvData)

        val clob = new SerialClob(moleBinary.get)

        val ctxtClob = new SerialClob(SerialiserService.serialise(ctxt).toCharArray)

        val outputBlob = new SerialBlob(Array[Byte]())
        //val id = UUID.randomUUID().toString

        val (me, path) = createMoleExecution(pEx, ctxt, encapsulate, genPath)
        db withSession { implicit session ⇒
          MoleData.instance.insert((me.id, name, Status.Stopped.toString, clob, ctxtClob, encapsulate, pack, outputBlob))
          MoleStats.instance.insert((me.id, 0, 0, 0, 0, 0))
        }
        cache.cacheMoleExecution(me, path, me.id)
        Right(me)
      }
      case Right(error) ⇒ Left(error)
    }
  }

  def getMoleKeys = cache.getMoleKeys

  def getMole(key: String): Option[MoleExecution] = { //TODO: move a large part of this to cache
    lazy val mole: Option[MoleExecution] = db withSession { implicit session ⇒

      val row = MoleData.instance filter (_.id === key)
      val molePack = (row map (_.molePackage)).list.headOption.getOrElse(false)
      val workDir = if (molePack) Some(Workspace.newDir) else None
      val moleDeserialiser: InputStream ⇒ PartialMoleExecution = workDir map (dir ⇒
        (in: InputStream) ⇒ SerialiserService.deserialiseAndExtractFiles[PartialMoleExecution](new TarInputStream(in), dir)
      ) getOrElse (SerialiserService.deserialise[PartialMoleExecution](_))

      val r = (row map (r ⇒ (r.clobbedMole, r.clobbedContext, r.encapsulated))).list.headOption map {
        case (pMClob, ctxtClob, e) ⇒ (moleDeserialiser(pMClob.getAsciiStream), SerialiserService.deserialise[Context](ctxtClob.getAsciiStream), e)
      }

      r map Function.tupled(createMoleExecution(_, _, _, workDir)) map Function.tupled(cache.cacheMoleExecution(_, _, key))
    }

    cache.getMole(key) orElse mole
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

  def isEncapsulated(key: String): Boolean = db withSession { implicit session ⇒
    (for { m ← MoleData.instance if m.id === key } yield m.encapsulated).list.forall(b ⇒ b)
  }

  //TODO - FRAGILE
  def storeResultBlob(exec: MoleExecution) = {
    val mPath = cache.getCapsule(exec)
    for (path ← mPath) {
      val outFile = new File(Workspace.newFile.toString + ".tar")
      outFile.createNewFile()

      try {
        Tar.createDirectoryTar(path, outFile)

        for (tis ← managed(Source.fromFile(outFile)(Codec.ISO8859))) {
          val arr = tis.iter.toArray.map(_.toByte)
          cache.storeResultBlob(exec, new SerialBlob(arr))
        }
      }
      catch {
        case e: Exception ⇒ e.printStackTrace(System.out)
      }
    }
  }
}

