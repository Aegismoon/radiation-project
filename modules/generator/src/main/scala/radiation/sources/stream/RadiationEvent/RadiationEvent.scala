package radiation.sources.stream

import io.circe.{Decoder, Encoder}
import io.circe.syntax._
import radiation.sources.model._
import java.time.Instant

final case class RadiationEvent(
                                 sourceId: String,
                                 radiationType: RadiationType,
                                 intensity: Double,
                                 coordinates: (Double, Double),
                                 timestamp: Instant
                               )

object RadiationEvent {
  // --- Timestamp как millis ---
  implicit val encodeInstant: Encoder[Instant] =
    Encoder.encodeLong.contramap(_.toEpochMilli)
  implicit val decodeInstant: Decoder[Instant] =
    Decoder.decodeLong.map(Instant.ofEpochMilli)

  // --- Координаты как { "lat": ..., "lon": ... } ---
  implicit val encodeCoords: Encoder[(Double, Double)] =
    Encoder.forProduct2("lat", "lon")(c => (c._1, c._2))
  implicit val decodeCoords: Decoder[(Double, Double)] =
    Decoder.forProduct2("lat", "lon")((lat: Double, lon: Double) => (lat, lon))

  // --- RadiationType как строка ---
  implicit val encodeRadiationType: Encoder[RadiationType] =
    Encoder.encodeString.contramap {
      case RadiationType.Alpha   => "alpha"
      case RadiationType.Beta    => "beta"
      case RadiationType.Gamma   => "gamma"
      case RadiationType.Neutron => "neutron"
    }

  implicit val decodeRadiationType: Decoder[RadiationType] =
    Decoder.decodeString.emap {
      case "alpha"   => Right(RadiationType.Alpha)
      case "beta"    => Right(RadiationType.Beta)
      case "gamma"   => Right(RadiationType.Gamma)
      case "neutron" => Right(RadiationType.Neutron)
      case other     => Left(s"Unknown RadiationType: $other")
    }

  // --- Явные (де)кодеры для события ---
  implicit val eventEncoder: Encoder[RadiationEvent] =
    Encoder.forProduct5("sourceId", "radiationType", "intensity", "coordinates", "timestamp") { e =>
      (e.sourceId, e.radiationType, e.intensity, e.coordinates, e.timestamp)
    }

  implicit val eventDecoder: Decoder[RadiationEvent] =
    Decoder.forProduct5("sourceId", "radiationType", "intensity", "coordinates", "timestamp") {
      (sourceId: String, radiationType: RadiationType, intensity: Double, coordinates: (Double, Double), timestamp: Instant) =>
        RadiationEvent(sourceId, radiationType, intensity, coordinates, timestamp)
    }

  // Утилиты
  def toJson(event: RadiationEvent): String = event.asJson.noSpaces

  def fromJson(str: String): Either[io.circe.Error, RadiationEvent] =
    io.circe.parser.decode[RadiationEvent](str)
}
