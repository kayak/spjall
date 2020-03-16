package com.r9.spjall.web

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import akka.stream.ActorMaterializer
import akka.util.ByteString
import io.circe.Decoder
import com.r9.spjall.web.ApiResponse._

import scala.concurrent.{ ExecutionContext, Future }

/**
 *
 * @author Paul Thordarson - thor@kayak.com
 */
trait WebApi {
  def performApiRequest[A: Decoder](r: HttpRequest, contentField: Option[String] = None)(implicit as: ActorSystem): Future[ApiResponse[A]] = {
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContext = as.dispatcher

    Http().singleRequest(r)
      .flatMap {
        case HttpResponse(StatusCodes.OK, _, entity, _) =>
          entity.dataBytes.runFold(ByteString(""))(_ ++ _)
            .map(body => ApiResponse.parse[A](body.utf8String, contentField))
        case resp @ HttpResponse(code, _, _, _) =>
          resp.discardEntityBytes()
          Future.failed(new Exception(s"Received responseCode '$code' while fetching from "))
      }
  }

  /**
   * Performs paged Web API Request. Slack has support
   * @param rg
   * @param contentField
   * @param carry
   * @param w
   * @param m
   * @param as
   * @tparam A
   * @return
   */
  def performPagedApiRequest[A: Decoder](
    rg: Option[Meta] => HttpRequest,
    contentField: String,
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
      performApiRequest[Seq[A]](rg(m), Some(contentField)).flatMap {
        case e @ Error(_) => Future.successful(e)
        case Warning(a, warn, m) => performPagedApiRequest(rg, contentField, carry ++ a, w ++ Seq(warn), m.orElse(Some(Meta(None))))
        case Okay(a, m) => performPagedApiRequest(rg, contentField, carry ++ a, w, m.orElse(Some(Meta(None))))
      }
    }
  }
}
