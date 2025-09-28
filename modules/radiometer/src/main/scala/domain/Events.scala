package domain

import java.time.Instant
import io.circe.{Decoder, DecodingFailure}
import io.circe.generic.semiauto._

// типы радиации
sealed trait RadiationType
object RadiationType {
  case object Alpha   extends RadiationType
  case object Beta    extends RadiationType
  case object Gamma   extends RadiationType
  case object Neutron extends RadiationType

  implicit val decoder: Decoder[RadiationType] =
    Decoder.decodeString.emap {
      case s if s.equalsIgnoreCase("alpha")   => Right(Alpha)
      case s if s.equalsIgnoreCase("beta")    => Right(Beta)
      case s if s.equalsIgnoreCase("gamma")   => Right(Gamma)
      case s if s.equalsIgnoreCase("neutron") => Right(Neutron)
      case other                              => Left(s"unknown radiationType: $other")
    }
}

// единицы измерения
final case class MicroSieverts(value: Double)        extends AnyVal
final case class MicroSievertsPerHour(value: Double) extends AnyVal

// событие
final case class RadiationEvent(
                                 sourceId: String,
                                 radiationType: RadiationType,
                                 intensity: Double,                 // генератор присылает intensity в мкЗв/ч
                                 coordinates: (Double, Double),     // lat, lon
                                 timestamp: Instant
                               )

object RadiationEvent {

  /** Принимаем coordinates как {lat, lon} ИЛИ как [lat, lon] */
  implicit val coordinatesDecoder: Decoder[(Double, Double)] =
    Decoder.instance { c =>
      val asObject =
        for {
          lat <- c.downField("lat").as[Double]
          lon <- c.downField("lon").as[Double]
        } yield (lat, lon)

      asObject.orElse {
        c.as[Vector[Double]].flatMap {
          case Vector(lat, lon) => Right((lat, lon))
          case _ =>
            Left(DecodingFailure("coordinates must be [lat, lon] or {lat, lon}", c.history))
        }
      }
    }

  /** timestamp как epoch millis */
  implicit val instantDecoder: Decoder[Instant] =
    Decoder.decodeLong.map(Instant.ofEpochMilli)

  implicit val decoder: Decoder[RadiationEvent] = deriveDecoder
}
