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
  /**
   * Performs a singe Web API Request against a web API
   *
   * @param r            An `HttpRequest` instance used for accessing the API.
   * @param contentField This is the name of the field in the API response that is expected to
   *                     to have the actually list of returned content. Used in parsing.
   * @param as           ActorSystem used to execute this function call.
   * @tparam A           The type of the element expected to be returned by the API call.
   * @return             An `ApiResponse` item of A.
   */
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
   * Performs a paged web API Request. Many Slack API calls have page limits and support paging
   * through the results. This function performs a series of API calls that pages through a
   * given endpoint and returns the complete set of items from all of the requests. This function
   * operates recursively by passing the meta from a returned API call (which contains the next
   * page index) on until there is no next page.
   *
   * @param rg           A function that takes an optional meta field from a previous function
   *                     call and returns an HttpRequest.
   * @param contentField This is the name of the field in the API response that is expected to
   *                     to have the actually list of returned content. Used in parsing.
   * @param carry        This is a Seq containing an aggregation of previously returned elements.
   *                     Used in performing a tail recursive call to pass the "results so far"
   *                     on to the call.
   * @param w            An aggregation of warnings returned by the API so far.
   * @param m            An optional meta returned by the previous call. Passed to `rg` to generate
   *                     the next request if needed.
   * @param as           ActorSystem used to execute this function call.
   * @tparam A           The type of the elements expected to be returned by the API call.
   * @return             An `ApiResponse` item of Seq[A].
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
