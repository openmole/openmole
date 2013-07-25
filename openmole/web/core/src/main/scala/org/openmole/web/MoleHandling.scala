package org.openmole.web

import scala.reflect.ClassTag
import scala.io.Source
import org.scalatra.servlet.FileItem
import java.io.{ PrintStream, InputStream, File }
import org.openmole.core.serializer.SerializerService
import com.thoughtworks.xstream.mapper.CannotResolveClassException
import org.openmole.core.model.mole.IPartialMoleExecution
import org.openmole.core.model.mole.{ IPartialMoleExecution, IMoleExecution, ExecutionContext }
import org.openmole.misc.tools.io.FromString
import org.openmole.core.implementation.validation.DataflowProblem.{ MissingSourceInput, MissingInput }
import org.openmole.core.model.data.{ Context, Prototype, Variable }
import org.openmole.core.implementation.validation.Validation
import javax.sql.rowset.serial.SerialClob
import org.openmole.misc.eventdispatcher.{ EventListener, EventDispatcher }
import org.slf4j.LoggerFactory
import com.jolbox.bonecp.{ BoneCPDataSource, BoneCPConfig }
import akka.actor.ActorSystem

import slick.driver.H2Driver.simple._
import slick.jdbc.meta.MTable

import Database.threadLocalSession
import org.openmole.misc.workspace.Workspace
import java.nio.file.Paths

trait MoleHandling { self: SlickSupport ⇒

  def system: ActorSystem

  protected implicit def executor: concurrent.ExecutionContext = system.dispatcher

  private val cachedMoles = new DataHandler[String, IMoleExecution](system)
  private val moleStats = new DataHandler[String, Stats.Stats](system)

  private val listener: EventListener[IMoleExecution] = new JobEventListener(moleStats)
  private val mStatusListener = new MoleStatusListener(this)

  db withSession {
    if (MTable.getTables("MoleData").list().isEmpty)
      MoleData.ddl.create // check that table exists somehow
  }

  (getUnfinishedMoleKeys map getMole).flatten foreach (_.start)

  def getStatus(exec: IMoleExecution): String = {
    if (!exec.started)
      "Stopped"
    else if (!exec.finished)
      "Running"
    else
      "Finished"
  }

  private def processXMLFile[A: ClassTag](is: Option[InputStream]): (Option[A], String) = { //Make Either[A, String], use Scala-arm.
    is match {
      case Some(stream) ⇒
        try {
          val ret = is.map(SerializerService.deserialize[A](_))
          if (!ret.forall(evidence$1.runtimeClass.isInstance(_)))
            None -> s"The uploaded xml is not a subtype of the type you wished to deserialize to: ${evidence$1.runtimeClass} vs ${ret.get.getClass}"
          else
            ret -> ""
        }
        catch {
          case e: CannotResolveClassException ⇒ None -> "The uploaded xml was not a valid serialized object."
          case c: ClassCastException          ⇒ None -> "Blargh"
        }
      case None ⇒ None -> "No data was uploaded."
    }
  }

  private def reifyCSV(mole: IPartialMoleExecution, csvData: Map[String, String]) = {
    def fromString[T: FromString](s: String) = {
      implicitly[FromString[T]].fromString(s)
    }

    def createVariable[T: FromString](mI: MissingInput) = csvData get mI.data.prototype.name map (d ⇒ Variable[T](mI.data.prototype.asInstanceOf[Prototype[T]], fromString[T](d)))

    val a = Validation(mole.mole, sources = mole.sources, hooks = mole.hooks)
    val mIS = a.map(_ match {
      case x: MissingInput       ⇒ x
      case y: MissingSourceInput ⇒ MissingInput(y.slot, y.input)
      case error                 ⇒ throw new Exception(s"Malformed partial mole: $error")
    })

    val c = mIS.map { mI ⇒
      mI.data.prototype.`type`.erasure match {
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

  private def cacheMole(mole: IMoleExecution) = {
    cachedMoles.add(mole.id, mole)
    EventDispatcher.listen(mole, listener, classOf[IMoleExecution.JobStatusChanged])
    EventDispatcher.listen(mole, listener, classOf[IMoleExecution.JobCreated])
    EventDispatcher.listen(mole, mStatusListener, classOf[IMoleExecution.Starting])
    EventDispatcher.listen(mole, mStatusListener, classOf[IMoleExecution.Finished])

  }

  def createMole(moleInput: ⇒ Option[InputStream], csvInput: ⇒ Option[InputStream], encapsulate: Boolean = false): Either[String, IMoleExecution] = {
    val r = csvInput map Source.fromInputStream

    val regex = """(.*),(.*)""".r
    val csvData = r.map(_.getLines().map(_ match {
      case regex(name: String, data: String) ⇒ name -> data
      case _                                 ⇒ throw new Exception("Invalidly formatted csv file")
    }).toMap) getOrElse Map()

    val moleExec = processXMLFile[IPartialMoleExecution](moleInput)

    val path: Option[File] = if (encapsulate) Some(Workspace.newDir(Paths.get("").toAbsolutePath.toString)) else None

    println(path)

    val context = ExecutionContext(new PrintStream(new File(path.getOrElse(".") + "/out")), path)

    moleExec match {
      case (Some(pEx), _) ⇒ {
        val ctxt = reifyCSV(pEx, csvData)
        val exec = pEx.toExecution(ctxt, context)

        val clob = new SerialClob(SerializerService.serialize(exec).toCharArray)

        db withSession {
          val p = path map (_.getAbsolutePath) getOrElse "."
          MoleData.insert((exec.id, getStatus(exec), clob, p))
        }

        cacheMole(exec)

        Right(exec)
      }
      case (_, error) ⇒ Left(error)
    }
  }

  def getMoleKeys = db withSession {
    (for {
      m ← MoleData
    } yield m.id.asColumnOf[String]).list
  }

  private def getUnfinishedMoleKeys = db withSession {
    (for (m ← MoleData if m.state === "Running") yield m.id.asColumnOf[String]).list
  }

  def getMole(key: String): Option[IMoleExecution] = {
    lazy val mole: Option[IMoleExecution] = db withSession {
      val r = MoleData.filter(_.id === key).map(_.clobbedMole).list().headOption match {
        case Some(head) ⇒ {
          val r = SerializerService.deserialize[IMoleExecution](head.getAsciiStream)
          cacheMole(r)
          Some(r)
        }
        case _ ⇒ None
      }

      r
    }

    cachedMoles get key orElse mole
  }

  def getMoleStats(mole: IMoleExecution) = (moleStats get mole.id getOrElse Stats.empty) + ("totalJobs" -> mole.moleJobs.size)
  def startMole(key: String) { getMole(key) foreach (_.start) }

  def deleteMole(key: String) = {
    val ret = cachedMoles get key map (_.cancel)
    cachedMoles remove key
    db withSession {
      MoleData.filter(_.id === key).delete
    }

    ret
  }

  def setStatus(mole: IMoleExecution, status: String) = {
    db withSession {
      val x = for { m ← MoleData if m.id === mole.id } yield m.state
      x.update(status)
      println(s"updated mole: ${mole.id} to ${status}")
    }
  }

  def isEncapsulated(key: String): Boolean = db withSession {
    (for { m ← MoleData if m.id === key } yield !(m.path === ".")).list.forall(b ⇒ b)
  }
}

object MoleHandling {
  object Status {
    val running = "Running"
    val finished = "Finished"
    val stopped = "Stopped"
  }
}
