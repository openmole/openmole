package org.openmole.rest.client

import java.io.File
import javax.net.ssl.{ SSLContext, SSLSocket }

import org.apache.http.client.methods.{ CloseableHttpResponse, HttpEntityEnclosingRequestBase, HttpPost }
import org.apache.http.client.utils.URIBuilder
import org.apache.http.conn.ssl.{ TrustSelfSignedStrategy, SSLContextBuilder, SSLConnectionSocketFactory }
import org.apache.http.entity.mime._
import org.apache.http.impl.client.{ CloseableHttpClient, HttpClients }
import org.openmole.rest.message._
import org.openmole.tool.tar._
import org.openmole.tool.file._

import scala.concurrent.duration._
import scala.io.Source
import scala.util.{ Failure, Success, Try }
import org.json4s._
import org.json4s.jackson.JsonMethods._

object RESTClient extends App {

  val url = args(0)
  val password = args(1)

  val client =
    new Client {
      override def address: String = url
      override def timeout: Duration = 5 minutes
    }

  val token = client.requestToken(password).left.get.token

  val file = new File("/tmp/test.txt")
  file.content = "test"

  val archive = new File("/tmp/archive.tgz")
  archive.withTarGZOutputStream { tos ⇒
    tos.addFile(file, file.getName)
  }

  val script =
    """
      |val i = Val[Double]
      |val res = Val[Double]
      |val test = Val[File]
      |
      |val file = inputDirectory / "test.txt"
      |
      |val exploration = ExplorationTask(i in (0.0 to 100.0 by 1.0))
      |
      |val model =
      |  ScalaTask("val res = i * 2") set (
      |    inputs += i,
      |    outputs += (i, res, test),
      |    test := file
      |  )
      |
      |exploration -< (model on LocalEnvironment(4) hook ToStringHook())
    """.stripMargin

  val id = client.start(token, script, Some(archive))
  println(id)
  Iterator.continually(client.state(token, id.left.get.id)).takeWhile(_.left.get.state == running).foreach { s ⇒
    println(s)
    Thread.sleep(1000)
  }

  println(client.state(token, id.left.get.id))
  println(client.output(token, id.left.get.id))
  println(client.remove(token, id.left.get.id))

}

case class HttpError(code: Int, error: Option[Error])

trait Client {

  implicit val formats = DefaultFormats

  def address: String
  def timeout: Duration

  def requestToken(password: String): Either[Token, HttpError] = {
    val uri = new URIBuilder(address + "/token").setParameter("password", password).build
    val post = new HttpPost(uri)
    execute(post) { response ⇒
      parse(response.content).extract[Token]
    }
  }

  def start(token: String, script: String, inputFiles: Option[File]): Either[ExecutionId, HttpError] = {
    def files = inputFiles.map { f ⇒
      val builder = MultipartEntityBuilder.create()
      builder addBinaryBody ("inputDirectory", f)
      builder.build
    }

    val uri =
      new URIBuilder(address + "/start").
        setParameter("token", token).
        setParameter("script", script).build

    val post = new HttpPost(uri)
    files.foreach(post.setEntity)
    execute(post) { response ⇒
      parse(response.content).extract[ExecutionId]
    }
  }

  def state(token: String, id: String): Either[State, HttpError] = {
    val uri =
      new URIBuilder(address + "/state").
        setParameter("token", token).
        setParameter("id", id).build
    val post = new HttpPost(uri)

    execute(post) { response ⇒ parse(response.content).extract[State] }
  }

  def output(token: String, id: String): Either[Output, HttpError] = {
    val uri =
      new URIBuilder(address + "/output").
        setParameter("token", token).
        setParameter("id", id).build

    val post = new HttpPost(uri)
    execute(post) { response ⇒ parse(response.content).extract[Output] }
  }

  def remove(token: String, id: String): Either[Unit, HttpError] = {
    val uri =
      new URIBuilder(address + "/remove").
        setParameter("token", token).
        setParameter("id", id).build
    val post = new HttpPost(uri)
    execute(post) { _ ⇒ Unit }
  }

  def execute[T](request: HttpEntityEnclosingRequestBase)(f: CloseableHttpResponse ⇒ T): Either[T, HttpError] = withClient { client ⇒
    val response = client.execute(request)
    try
      response.getStatusLine.getStatusCode match {
        case c if c < 400 ⇒ Left(f(response))
        case c            ⇒ Right(HttpError(c, parse(response.content).extractOpt[Error]))
      }
    finally response.close
  }

  implicit class ResponseDecorator(response: CloseableHttpResponse) {
    def content = {
      val source = Source.fromInputStream(response.getEntity.getContent)
      try source.mkString
      finally source.close
    }
  }

  def withClient[T](f: CloseableHttpClient ⇒ T): T = {
    val client = HttpClients.custom().setSSLSocketFactory(factory).build()
    try f(client)
    finally client.close
  }

  @transient lazy val factory = {
    val builder = new SSLContextBuilder
    builder.loadTrustMaterial(null, new TrustSelfSignedStrategy)
    new SSLConnectionSocketFactory(builder.build, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER) {
      override protected def prepareSocket(socket: SSLSocket) = {
        socket.setSoTimeout(timeout.toMillis.toInt)
      }
    }
  }

}