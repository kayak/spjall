package org.kapunga.spjall.rtm

import akka.actor._
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.ws.{ Message, TextMessage, WebSocketRequest }
import akka.stream.{ ActorMaterializer, OverflowStrategy, QueueOfferResult }
import akka.stream.scaladsl.{ Sink, Source }
import akka.util.Timeout
import io.circe.syntax._
import org.kapunga.spjall.web.Rtm

import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.postfixOps
import scala.util.{ Failure, Success }

class RtmApiActor(initialSubscribers: Set[ActorRef]) extends Actor with ActorLogging {
  import RtmApiActor._
  import context.dispatcher

  implicit val as: ActorSystem = context.system
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val timeout: Timeout = Timeout(60 seconds)

  val rtmConnectResponse: Rtm.RtmConnectResponse = Await.result(Rtm.connect, timeout.duration).get

  val msgCount: Iterator[Int] = Stream.from(1).iterator

  val queue =
    Source.queue[Message](Int.MaxValue, OverflowStrategy.backpressure)
      .throttle(15, 15 second)
      .via(Http(context.system).webSocketClientFlow(WebSocketRequest(rtmConnectResponse.url)))
      .to(Sink.actorRefWithAck(self, Init, Ack, Complete))
      .run()

  val ticker = context.system.scheduler.schedule(30 seconds, 30 seconds, self, Tick)

  var subscribers: Set[ActorRef] = initialSubscribers
  var unAcked: Set[Long] = Set()

  override def receive: Receive = {
    case Init =>
      sender ! Ack
    case Tick =>
      self ! nextPing
    case Hello =>
      log.info("Successfully connected to Slack RTM API")
    case Pong(r, ts) =>
      unAcked = unAcked - r
      log.debug(s"Received ping response to '$r' sent at '$ts' ")
    case Ignored(t, _) =>
      log.debug(s"Ignoring message of type '$t'")
    case Unsupported(mType, sType, content) =>
      log.warning(s"Unsupported message type '$mType' ${sType.map("('" + _ + "') ").getOrElse("")}received:\n$content")
    case re: RtmEvent =>
      if (subscribers.isEmpty) {
        log.warning(s"Subscriber list is empty, discarding message: $re")
      } else {
        subscribers.foreach(_ ! (rtmConnectResponse.self, re))
      }
    case Subscribe(a) =>
      subscribers = subscribers + a
      a ! (rtmConnectResponse.self, rtmConnectResponse.team)
    case Unsubscribe(a) =>
      subscribers = subscribers - a
    case rc: RtmCommand =>
      sendMessage(rc)
    case TextMessage.Strict(text) =>
      sender ! Ack
      parseEvent(text).foreach(self ! _)
    case Complete =>
      throw RtmClosedException(subscribers)
  }

  def nextPing: Ping = {
    val nextPing = Ping()
    if (unAcked.size > pingWarnSize) log.warning(s"Un-acked pings: ${unAcked.size}")
    unAcked = unAcked + nextPing.id
    nextPing
  }

  def parseEvent(text: String): Option[RtmEvent] = {
    log.debug(s"Parsing JSON:\n$text")
    RtmEvent.parse(text) match {
      case Left(df) =>
        log.warning(s"Failed to decode: '${df.message}'\nFor message: ${df.message}")
        None
      case Right(rtme) =>
        Some(rtme)
    }
  }

  def sendMessage(rc: RtmCommand): Unit = {
    val cmd = rc.asJson
    log.debug(s"Sending RTM command:\n${cmd.noSpaces}")
    queue.offer(TextMessage(cmd.noSpaces)).onComplete({
      case Success(x) =>
        x match {
          case QueueOfferResult.Enqueued => log.debug(s"Enqueue of ${rc.getClass.getSimpleName} succeeded.")
          case QueueOfferResult.Dropped => log.warning(s"Enqueue failed, dropped: ${cmd.noSpaces}")
          case QueueOfferResult.Failure(cause) => log.warning(s"Enqueue failed: $cause")
          case QueueOfferResult.QueueClosed => log.warning(s"Queue is closed")
        }
      case Failure(e) => log.warning(s"Enqueue failed to complete: $e")
    })
  }

  /**
   * Notify subscribers. This should only be called on start, not restart as subscribers
   * are persisted and will already have received this notification.
   */
  override def preStart(): Unit = subscribers.foreach(_ ! (rtmConnectResponse.self, rtmConnectResponse.team))

  /**
   * This should not call `preStart`, no need to re-notify persisted subscribers
   * @param reason The reason for the restart
   */
  override def postRestart(reason: Throwable): Unit = reason match {
    case rce: RtmClosedException => subscribers = rce.subscribers
    case t: Throwable => log.warning(s"Restarted with unexpected throwable type: ${t.getClass.getName}")
  }

  /**
   * We just need to cancel the ticker when stopped.
   */
  override def postStop(): Unit = ticker.cancel()
}

object RtmApiActor {
  case class Subscribe(actor: ActorRef)
  case class Unsubscribe(actor: ActorRef)

  private case object Tick
  private case object Init
  private case object Ack
  private case object Complete

  def props(initialSubscribers: Set[ActorRef] = Set()): Props =
    Props(new RtmApiActor(initialSubscribers))
}