package com.r9.spjall.rtm

import cats.syntax.either._
import cats.syntax.functor._
import io.circe.Decoder.Result
import io.circe._
import com.r9.spjall._
import com.r9.spjall.TypeCheckingDecoder

import scala.reflect.ClassTag

sealed trait RtmEvent

sealed trait RtmMsgEvent extends RtmEvent

sealed trait RtmUpdateEvent extends RtmEvent

abstract class RtmDecoder[R <: RtmEvent: ClassTag](
  eventType: String,
  subType: Option[String] = None) extends TypeCheckingDecoder[R] {

  override def canParse(c: HCursor): Result[Boolean] =
    for {
      et <- c.downField("type").as[String]
      st <- c.downField("subtype").as[Option[String]]
    } yield { et == eventType && subTypeMatches(subType, st) }

  private def subTypeMatches(required: Option[String], contains: Option[String]): Boolean =
    (required, contains) match {
      case (Some(r), Some(c)) => r == c
      case (None, None) => true
      case _ => false
    }
}

case object Hello extends RtmEvent {
  implicit val helloDecoder: Decoder[Hello.type] = new RtmDecoder[Hello.type]("hello") {
    override def parse(c: HCursor): Result[Hello.type] = Right(Hello)
  }
}

case class Pong(replyTo: Long, time: Long) extends RtmEvent

object Pong {
  implicit val pongDecoder: Decoder[Pong] = new RtmDecoder[Pong]("pong") {
    override def parse(c: HCursor): Result[Pong] =
      for {
        replyTo <- c.downField("reply_to").as[Long]
        time <- c.downField("time").as[Long]
      } yield { Pong(replyTo, time) }
  }
}

case class ChannelArchive(channel: SlackId, user: SlackId, isMoved: Int, ets: EventTs) extends RtmUpdateEvent

object ChannelArchive {
  implicit val channelArchiveDecoder: Decoder[ChannelArchive] = new RtmDecoder[ChannelArchive]("channel_archive") {
    override def parse(c: HCursor): Result[ChannelArchive] =
      for {
        channel <- c.downField("channel").as[SlackId]
        user <- c.downField("user").as[SlackId]
        isMoved <- c.downField("is_moved").as[Int]
        ets <- c.downField("event_ts").as[EventTs]
      } yield { ChannelArchive(channel, user, isMoved, ets) }
  }
}

case class DndUpdatedUser(user: SlackId, dnd: DndStatus, ets: EventTs) extends RtmUpdateEvent

object DndUpdatedUser {
  implicit val dndUpdatedUserDecoder: Decoder[DndUpdatedUser] =
    new RtmDecoder[DndUpdatedUser]("dnd_updated_user") {
      override def parse(c: HCursor): Result[DndUpdatedUser] =
        for {
          user <- c.downField("user").as[SlackId]
          ds <- c.downField("dnd_status").as[DndStatus]
          ets <- c.downField("event_ts").as[EventTs]
        } yield { DndUpdatedUser(user, ds, ets) }
    }
}

case class EmojiAdded(name: String, value: String, ets: EventTs) extends RtmUpdateEvent

object EmojiAdded {
  implicit val emojiAddedDecoder: Decoder[EmojiAdded] = new RtmDecoder[EmojiAdded]("emoji_changed", Some("add")) {
    override def parse(c: HCursor): Result[EmojiAdded] =
      for {
        name <- c.downField("name").as[String]
        value <- c.downField("value").as[String]
        ets <- c.downField("event_ts").as[EventTs]
      } yield { EmojiAdded(name, value, ets) }
  }
}

case class FileDeleted(id: SlackId, channels: List[SlackId], ets: EventTs) extends RtmUpdateEvent

object FileDeleted {
  implicit val fileDeletedDecoder: Decoder[FileDeleted] = new RtmDecoder[FileDeleted]("file_deleted") {
    override def parse(c: HCursor): Result[FileDeleted] =
      for {
        id <- c.downField("file_id").as[SlackId]
        channels <- c.downField("channel_ids").as[List[SlackId]]
        ets <- c.downField("event_ts").as[EventTs]
      } yield { FileDeleted(id, channels, ets) }
  }
}

case class Msg(
  user: SlackId,
  team: SlackId,
  channel: Option[SlackId],
  text: String,
  edited: Option[Msg.Identifier],
  blocks: List[Block],
  replies: List[Msg.Identifier],
  ts: EventTs,
  threadTs: Option[EventTs]) extends RtmEvent {

  lazy val replyUsers: List[SlackId] = replies.map(_.user).distinct
  lazy val replyCount: Int = replies.size
  lazy val latestReply: EventTs = replies.map(_.ts).max
  lazy val replyUsersCount: Int = replyUsers.size
}

object Msg {
  case class Identifier(user: SlackId, ts: EventTs)

  object Identifier {
    implicit val editDecoder: Decoder[Identifier] = new Decoder[Identifier] {
      override def apply(c: HCursor): Result[Identifier] =
        for {
          user <- c.downField("user").as[SlackId]
          ts <- c.downField("ts").as[EventTs]
        } yield { Identifier(user, ts) }
    }
  }

  implicit val msgDecoder: Decoder[Msg] = new RtmDecoder[Msg]("message") {
    override def parse(c: HCursor): Result[Msg] = {
      for {
        user <- c.downField("user").as[SlackId]
        team <- c.downField("team").as[SlackId]
        channel <- c.downField("channel").as[Option[SlackId]]
        text <- c.downField("text").as[String]
        edited <- c.downField("edited").as[Option[Identifier]]
        blocks <- c.downField("blocks").as[List[Block]]
        replies <- c.downField("replies").as[Option[List[Identifier]]].map(_.getOrElse(Nil))
        ts <- c.downField("ts").as[EventTs]
        threadTs <- c.downField("thread_ts").as[Option[EventTs]]
      } yield { Msg(user, team, channel, text, edited, blocks, replies, ts, threadTs) }
    }
  }
}

case class MsgAck(replyTo: Long, text: String, ts: EventTs) extends RtmMsgEvent

object MsgAck {
  implicit val msgAckDecoder: Decoder[MsgAck] = new Decoder[MsgAck] {
    override def apply(c: HCursor): Result[MsgAck] =
      for {
        ok <- c.downField("ok").as[Boolean]
        replyTo <- c.downField("reply_to").as[Long]
        text <- c.downField("text").as[String]
        ts <- c.downField("ts").as[EventTs]
      } yield { MsgAck(replyTo, text, ts) }
  }
}

case class MsgChanged(msg: Msg, prev: Msg, channel: SlackId, ts: EventTs) extends RtmMsgEvent

object MsgChanged {
  implicit val msgChangedDecoder: Decoder[MsgChanged] = new RtmDecoder[MsgChanged]("message", Some("message_changed")) {
    override def parse(c: HCursor): Result[MsgChanged] =
      for {
        msg <- c.downField("message").as[Msg]
        prev <- c.downField("previous_message").as[Msg]
        channel <- c.downField("channel").as[SlackId]
        ts <- c.downField("ts").as[EventTs]
      } yield { MsgChanged(msg, prev, channel, ts) }
  }
}

case class MsgReplied(msg: Msg, channel: SlackId, ts: EventTs) extends RtmMsgEvent

object MsgReplied {
  implicit val msgRepliedDecoder: Decoder[MsgReplied] = new RtmDecoder[MsgReplied]("message", Some("message_replied")) {
    override def parse(c: HCursor): Result[MsgReplied] =
      for {
        msg <- c.downField("message").as[Msg]
        channel <- c.downField("channel").as[SlackId]
        ts <- c.downField("ts").as[EventTs]
      } yield { MsgReplied(msg, channel, ts) }
  }
}

case class ReactionAdded(
  user: SlackId,
  item: Item,
  reaction: String,
  itemUser: SlackId,
  ts: EventTs) extends RtmMsgEvent

object ReactionAdded {
  implicit val reactionAddedDecoder: Decoder[ReactionAdded] = new RtmDecoder[ReactionAdded]("reaction_added") {
    override def parse(c: HCursor): Result[ReactionAdded] =
      for {
        user <- c.downField("user").as[SlackId]
        item <- c.downField("item").as[Item]
        reaction <- c.downField("reaction").as[String]
        itemUser <- c.downField("item_user").as[SlackId]
        ts <- c.downField("ts").as[EventTs]
      } yield { ReactionAdded(user, item, reaction, itemUser, ts) }
  }
}

case class SubteamMembersChanged(
  ug: SlackId,
  team: SlackId,
  update: Long,
  previousUpdate: Long,
  added: List[SlackId],
  removed: List[SlackId],
  ets: EventTs) extends RtmUpdateEvent

object SubteamMembersChanged {
  implicit val subteamMembersChangedDecoder: Decoder[SubteamMembersChanged] =
    new RtmDecoder[SubteamMembersChanged]("subteam_members_changed") {
      override def parse(c: HCursor): Result[SubteamMembersChanged] =
        for {
          ug <- c.downField("subteam_id").as[SlackId]
          team <- c.downField("team_id").as[SlackId]
          update <- c.downField("date_update").as[Long]
          previousUpdate <- c.downField("date_previous_update").as[Long]
          added <- c.downField("added_users").as[List[SlackId]]
          removed <- c.downField("removed_users").as[List[SlackId]]
          ets <- c.downField("event_ts").as[EventTs]
        } yield { SubteamMembersChanged(ug, team, update, previousUpdate, added, removed, ets) }
    }
}

case class SubteamUpdated(ug: UserGroup, ets: EventTs) extends RtmUpdateEvent

object SubteamUpdated {
  implicit val subteamUpdatedDecoder: Decoder[SubteamUpdated] = new RtmDecoder[SubteamUpdated]("subteam_updated") {
    override def parse(c: HCursor): Result[SubteamUpdated] =
      for {
        st <- c.downField("subteam").as[UserGroup]
        ets <- c.downField("event_ts").as[EventTs]
      } yield { SubteamUpdated(st, ets) }
  }
}

case class UserChange(user: User, channel: Option[SlackId], cacheTs: Long, ets: EventTs) extends RtmUpdateEvent

object UserChange {
  implicit val userChangeDecoder: Decoder[UserChange] = new RtmDecoder[UserChange]("user_change") {
    override def parse(c: HCursor): Result[UserChange] =
      for {
        user <- c.downField("user").as[User]
        channel <- c.downField("channel").as[Option[SlackId]]
        cts <- c.downField("cache_ts").as[Long]
        ets <- c.downField("event_ts").as[EventTs]
      } yield { UserChange(user, channel, cts, ets) }
  }
}

case class UserTyping(channel: SlackId, user: SlackId) extends RtmEvent

object UserTyping {
  implicit val userTypingDecoder: Decoder[UserTyping] = new RtmDecoder[UserTyping]("user_typing") {
    override def parse(c: HCursor): Result[UserTyping] =
      for {
        channel <- c.downField("channel").as[SlackId]
        user <- c.downField("user").as[SlackId]
      } yield { UserTyping(channel, user) }
  }
}

case class TeamJoin(user: User, cacheTs: Long, ets: EventTs) extends RtmEvent

object TeamJoin {
  implicit val teamJoinDecoder: Decoder[TeamJoin] = new RtmDecoder[TeamJoin]("team_join") {
    override def parse(c: HCursor): Result[TeamJoin] =
      for {
        user <- c.downField("user").as[User]
        cts <- c.downField("cache_ts").as[Long]
        ets <- c.downField("event_ts").as[EventTs]
      } yield { TeamJoin(user, cts, ets) }
  }
}

case class Ignored(eType: String, content: String) extends RtmEvent

object Ignored {
  implicit val ignoredDecoder: Decoder[Ignored] = new TypeCheckingDecoder[Ignored] {
    override def canParse(c: HCursor): Result[Boolean] =
      c.downField("type").as[String].map(s => ignoredMsgTypes.contains(s))

    override def parse(c: HCursor): Result[Ignored] =
      c.downField("type").as[String].map(s => Ignored(s, c.value.spaces2))
  }
}

case class Unsupported(eType: String, subtype: Option[String], content: String) extends RtmEvent

object Unsupported {
  implicit val unsupportedDecoder: Decoder[Unsupported] = new Decoder[Unsupported] {
    override def apply(c: HCursor): Result[Unsupported] = {
      for {
        eType <- c.downField("type").as[String]
        sType <- c.downField("subtype").as[Option[String]]
      } yield { Unsupported(eType, sType, c.value.spaces2) }
    }
  }
}

object RtmEvent {
  def parse(text: String): Result[RtmEvent] =
    parser.parse(text)
      .leftMap(f => DecodingFailure(s"Parsing failed: ${f.message}", Nil))
      .flatMap(_.as[RtmEvent])

  implicit val rtmEventDecoder: Decoder[RtmEvent] = List[Decoder[RtmEvent]](
    Hello.helloDecoder.widen,
    Pong.pongDecoder.widen,
    Ignored.ignoredDecoder.widen, // This should go in early to make sure ignored messages are blocked.
    ChannelArchive.channelArchiveDecoder.widen,
    DndUpdatedUser.dndUpdatedUserDecoder.widen,
    EmojiAdded.emojiAddedDecoder.widen,
    FileDeleted.fileDeletedDecoder.widen,
    Msg.msgDecoder.widen,
    MsgAck.msgAckDecoder.widen,
    MsgChanged.msgChangedDecoder.widen,
    MsgReplied.msgRepliedDecoder.widen,
    ReactionAdded.reactionAddedDecoder.widen,
    SubteamMembersChanged.subteamMembersChangedDecoder.widen,
    SubteamUpdated.subteamUpdatedDecoder.widen,
    TeamJoin.teamJoinDecoder.widen,
    UserChange.userChangeDecoder.widen,
    UserTyping.userTypingDecoder.widen,
    Unsupported.unsupportedDecoder.widen).reduce(_ or _)
}
