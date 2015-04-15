package org.openmole.rest.client

import java.io.File
import javax.net.ssl.{SSLContext, SSLSocket}

import org.apache.http.client.methods.{CloseableHttpResponse, HttpEntityEnclosingRequestBase, HttpPost}
import org.apache.http.client.utils.URIBuilder
import org.apache.http.conn.ssl.{TrustSelfSignedStrategy, SSLContextBuilder, SSLConnectionSocketFactory}
import org.apache.http.entity.mime._
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.openmole.rest.messages.{Output, ExecutionId, Token}

import scala.concurrent.duration._
import scala.io.Source
import scala.util.{Failure, Success, Try}
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


  val token = client.requestToken(password).get.token

  val script =
    """
      |val i = Val[Double]
      |val res = Val[Double]
      |val exploration = ExplorationTask(i in (0.0 to 100.0 by 1.0))
      |
      |val model =
      |  ScalaTask("val res = i * 2") set (
      |    inputs += i,
      |    outputs += (i, res)
      |    )
      |
      |exploration -< (model on LocalEnvironment(4) hook ToStringHook())
    """.stripMargin

  val id = client.start(token, script, None)
  Thread.sleep(10000)
  println(client.output(token, id.get.id))

}

case class HttpError(code: Int, message: String) extends Exception


trait Client {

  implicit val formats = DefaultFormats

  def address: String
  def timeout: Duration

  def requestToken(password: String): Try[Token] = {
    val uri = new URIBuilder(address + "/token").setParameter("password", password).build
    val post = new HttpPost(uri)
    execute(post) { response =>
      parse(response.getEntity.getContent).extract[Token]
     }
  }

  def start(token: String, script: String, inputFiles: Option[File]): Try[ExecutionId] = {
    def files = inputFiles.map{ f =>
      val builder = MultipartEntityBuilder.create()
      builder addBinaryBody ("inputs", f)
      builder.build
    }
    
    val uri =
      new URIBuilder(address + "/start").
        setParameter("token", token).
        setParameter("script", script).build

    val post = new HttpPost(uri)
    files.foreach(post.setEntity)
    execute(post) { response =>
      parse(response.getEntity.getContent).extract[ExecutionId]
    }
  }

  def output(token: String, id: String): Try[Output] = {
    val uri =
      new URIBuilder(address + "/output").
        setParameter("token", token).
        setParameter("id", id).build

    val post = new HttpPost(uri)
    execute(post) { response =>
      parse(response.getEntity.getContent).extract[Output]
    }
  }

  def execute[T](request: HttpEntityEnclosingRequestBase)(f: CloseableHttpResponse => T): Try[T] = withClient { client =>
    val response = client.execute(request)
    try
      response.getStatusLine.getStatusCode match {
        case c if c < 400 => Success(f(response))
        case c =>
          val error = HttpError(c, responseContent(response).mkString)
          println(error.message)
          Failure(error)
      }
    finally response.close
  }


  def responseContent(response: CloseableHttpResponse) = Source.fromInputStream(response.getEntity.getContent)


  def withClient[T](f: CloseableHttpClient => T): T = {
       val client = HttpClients.custom().setSSLSocketFactory(factory).build()
      try f(client)
    finally client.close
  }

  //def start

  @transient lazy val factory = {
    val builder = new SSLContextBuilder
    builder.loadTrustMaterial(null, new TrustSelfSignedStrategy)
    new SSLConnectionSocketFactory(builder.build, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER)
  }


   /* new SSLConnectionSocketFactory(, SSLConnectionSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER) {
      override protected def prepareSocket(socket: SSLSocket) = {
        socket.setSoTimeout(timeout.toMillis.toInt)
      }
    }*/

}