package org.kapunga.spjall.model

import cats.syntax.either._
import cats.syntax.functor._
import io.circe.Decoder.Result
import io.circe._
import io.circe.syntax._
import org.kapunga.spjall._

sealed trait Conversation extends SlackObject

case class Channel(
  id: SlackId,
  name: String,
  nameNormalized: String,
  created: Long,
  creator: SlackId,
  archived: Boolean,
  isGeneral: Boolean,
  members: List[SlackId],
  topic: Conversation.Meta,
  purpose: Conversation.Meta,
  previousNames: List[String],
  numMembers: Option[Int]) extends Conversation {

  override val identifiers: List[String] = name :: Nil
}

object Channel {
  implicit val channelDecoder: Decoder[Channel] = new Decoder[Channel] {
    override def apply(c: HCursor): Result[Channel] = {
      val isChannel: Result[Boolean] =
        for {
          ic <- c.downField("is_channel").as[Option[Boolean]].map(_.getOrElse(false))
          ip <- c.downField("is_private").as[Option[Boolean]].map(_.getOrElse(false))
        } yield { ic && !ip }

      def parseChannel: Result[Channel] = {
        for {
          id <- c.downField("id").as[SlackId]
          name <- c.downField("name").as[String]
          nameNormalized <- c.downField("name_normalized").as[String]
          created <- c.downField("created").as[Long]
          creator <- c.downField("creator").as[SlackId]
          archived <- c.downField("is_archived").as[Boolean]
          isGeneral <- c.downField("is_general").as[Boolean]
          members <- c.downField("members").as[Option[List[SlackId]]].map(_.getOrElse(Nil))
          topic <- c.downField("topic").as[Conversation.Meta]
          purpose <- c.downField("purpose").as[Conversation.Meta]
          previousNames <- c.downField("previous_names").as[List[String]]
          numMembers <- c.downField("num_members").as[Option[Int]]
        } yield {
          Channel(id, name, nameNormalized, created, creator, archived, isGeneral, members, topic, purpose, previousNames, numMembers)
        }
      }

      isChannel.flatMap(if (_) parseChannel else Left(DecodingFailure("JSON is not a Channel", c.history)))
    }
  }

  implicit val channelEncoder: Encoder[Channel] = new Encoder[Channel] {
    override def apply(a: Channel): Json = Json.obj(
      ("id", a.id.asJson),
      ("name", a.name.asJson),
      ("name_normalized", a.nameNormalized.asJson),
      ("created", a.created.asJson),
      ("creator", a.creator.asJson),
      ("is_archived", a.archived.asJson),
      ("is_general", a.isGeneral.asJson),
      ("members", a.members.asJson),
      ("is_channel", true.asJson),
      ("topic", a.topic.asJson),
      ("purpose", a.purpose.asJson),
      ("previous_names", a.previousNames.asJson),
      ("num_members", a.numMembers.asJson))
  }
}

case class Group(
  id: SlackId,
  name: String,
  members: List[SlackId],
  topic: Conversation.Meta,
  purpose: Conversation.Meta,
  created: Long,
  creator: SlackId,
  archived: Boolean) extends Conversation {

  override val identifiers: List[String] = name :: Nil
}

object Group {
  implicit val groupDecoder: Decoder[Group] = new Decoder[Group] {
    override def apply(c: HCursor): Result[Group] = {
      val isGroup: Result[Boolean] =
        for {
          ig <- c.downField("is_group").as[Option[Boolean]].map(_.getOrElse(false))
          imp <- c.downField("is_mpim").as[Option[Boolean]].map(_.getOrElse(false))
        } yield { ig && !imp }

      def parseGroup: Result[Group] = {
        for {
          id <- c.downField("id").as[SlackId]
          name <- c.downField("name").as[String]
          members <- c.downField("members").as[Option[List[SlackId]]].map(_.getOrElse(Nil))
          topic <- c.downField("topic").as[Conversation.Meta]
          purpose <- c.downField("purpose").as[Conversation.Meta]
          created <- c.downField("created").as[Long]
          creator <- c.downField("creator").as[SlackId]
          archived <- c.downField("is_archived").as[Boolean]
        } yield {
          Group(id, name, members, topic, purpose, created, creator, archived)
        }
      }

      isGroup.flatMap(if (_) parseGroup else Left(DecodingFailure("JSON is not a Group", c.history)))
    }
  }

  implicit val groupEncoder: Encoder[Group] = new Encoder[Group] {
    override def apply(a: Group): Json = Json.obj(
      ("id", a.id.asJson),
      ("is_group", true.asJson),
      ("is_mpim", false.asJson),
      ("name", a.name.asJson),
      ("members", a.members.asJson),
      ("topic", a.topic.asJson),
      ("purpose", a.purpose.asJson),
      ("created", a.created.asJson),
      ("creator", a.creator.asJson),
      ("is_archived", a.archived.asJson))
  }
}

case class Mpim(
  id: SlackId,
  name: String,
  members: List[SlackId],
  created: Long,
  creator: SlackId) extends Conversation {

  override val identifiers: List[String] = name :: Nil
}

object Mpim {
  implicit val mpimDecoder: Decoder[Mpim] = new Decoder[Mpim] {
    override def apply(c: HCursor): Result[Mpim] = {
      val isMpim: Result[Boolean] = c.downField("is_mpim").as[Option[Boolean]].map(_.getOrElse(false))

      def parseMpim: Result[Mpim] = {
        for {
          id <- c.downField("id").as[SlackId]
          name <- c.downField("name").as[String]
          members <- c.downField("members").as[Option[List[SlackId]]].map(_.getOrElse(Nil))
          created <- c.downField("created").as[Long]
          creator <- c.downField("creator").as[SlackId]
        } yield {
          Mpim(id, name, members, created, creator)
        }
      }

      isMpim.flatMap(if (_) parseMpim else Left(DecodingFailure("JSON is not an MPIM", c.history)))
    }
  }

  implicit val mpimEncoder: Encoder[Mpim] = new Encoder[Mpim] {
    override def apply(a: Mpim): Json = Json.obj(
      ("id", a.id.asJson),
      ("is_group", false.asJson),
      ("is_mpim", true.asJson),
      ("name", a.name.asJson),
      ("members", a.members.asJson),
      ("created", a.created.asJson),
      ("creator", a.creator.asJson))
  }
}

case class Im(
  id: SlackId,
  user: SlackId,
  created: Long,
  priority: Double,
  isUserDeleted: Boolean) extends Conversation {

  override val identifiers: List[String] = user.id :: Nil
}

object Im {
  implicit val imDecoder: Decoder[Im] = new Decoder[Im] {
    override def apply(c: HCursor): Result[Im] = {
      val isIm: Result[Boolean] = c.downField("is_im").as[Boolean]

      def parseIm: Result[Im] = {
        for {
          id <- c.downField("id").as[SlackId]
          user <- c.downField("user").as[SlackId]
          created <- c.downField("created").as[Long]
          priority <- c.downField("priority").as[Double]
          isUserDeleted <- c.downField("is_user_deleted").as[Boolean]
        } yield {
          Im(id, user, created, priority, isUserDeleted)
        }
      }

      isIm.flatMap(if (_) parseIm else Left(DecodingFailure("JSON is not an IM", c.history)))
    }
  }

  implicit val imEncoder: Encoder[Im] = new Encoder[Im] {
    override def apply(a: Im): Json = Json.obj(
      ("is_im", true.asJson),
      ("id", a.id.asJson),
      ("user", a.user.asJson),
      ("created", a.created.asJson),
      ("priority", a.priority.asJson),
      ("is_user_deleted", a.isUserDeleted.asJson))
  }
}

object Conversation {
  case class Meta(value: String, creator: Option[SlackId], lastSet: Long)

  object Meta {
    implicit val metaDecoder: Decoder[Meta] = new Decoder[Meta] {
      override def apply(c: HCursor): Result[Meta] =
        for {
          value <- c.downField("value").as[String]
          creator <- c.downField("creator").as[String]
          lastSet <- c.downField("last_set").as[Long]
        } yield {
          Meta(value, emptyStringOption(creator).map(SlackId(_)), lastSet)
        }
    }

    implicit val metaEncoder: Encoder[Meta] = new Encoder[Meta] {
      override def apply(a: Meta): Json = Json.obj(
        ("value", a.value.asJson),
        ("creator", a.creator.map(_.id).getOrElse("").asJson),
        ("last_set", a.lastSet.asJson))
    }
  }

  implicit val conversationDecoder: Decoder[Conversation] =
    List[Decoder[Conversation]](
      Channel.channelDecoder.widen,
      Group.groupDecoder.widen,
      Mpim.mpimDecoder.widen,
      Im.imDecoder.widen).reduceLeft(_ or _)

  implicit val conversationEncoder: Encoder[Conversation] = new Encoder[Conversation] {
    override def apply(a: Conversation): Json =
      a match {
        case channel: Channel => channel.asJson
        case group: Group => group.asJson
        case mpim: Mpim => mpim.asJson
        case im: Im => im.asJson
      }
  }
}

