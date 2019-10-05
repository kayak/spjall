package org.kapunga.spjall.web

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import akka.stream.ActorMaterializer
import akka.util.ByteString
import spray.json.JsonFormat

import scala.concurrent.{ ExecutionContext, Future }

/**
 *
 * @author Paul J Thordarson
 */
trait WebApi {
  def performApiRequest[A: JsonFormat](r: HttpRequest)(implicit as: ActorSystem): Future[ApiResponse[A]] = {
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = as.dispatcher

    Http().singleRequest(r)
      .flatMap {
        case HttpResponse(StatusCodes.OK, _, entity, _) =>
          entity.dataBytes.runFold(ByteString(""))(_ ++ _).map(body => ApiResponse.parse[A](body.utf8String))
        case resp @ HttpResponse(code, _, _, _) =>
          resp.discardEntityBytes()
          Future.failed(new Exception(s"Received responseCode '$code' while fetching from "))
      }
  }

  def performPagedApiRequest[A: JsonFormat](
    rg: Option[Meta] => HttpRequest,
    carry: Seq[A] = Nil,
    w: Seq[String] = Nil,
    m: Option[Meta] = None)(implicit as: ActorSystem): Future[ApiResponse[Seq[A]]] = {
    implicit val executionContext: ExecutionContext = as.dispatcher

    if (m.exists(_.nextCursor.isEmpty)) {
      if (w.isEmpty) {
        Future.successful(Okay(carry, None))
      } else {
        Future.successful(Warning(carry, w.toSet.mkString(", "), None))
      }
    } else {
      import spray.json.DefaultJsonProtocol._

      performApiRequest[Seq[A]](rg(m)).flatMap {
        case e @ Error(_) => Future.successful(e)
        case Warning(a, warn, m) => performPagedApiRequest(rg, carry ++ a, w ++ Seq(warn), m.orElse(Some(Meta(None))))
        case Okay(a, m) => performPagedApiRequest(rg, carry ++ a, w, m.orElse(Some(Meta(None))))
      }
    }
  }
}
