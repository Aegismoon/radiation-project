package domain

import java.time.Instant
import io.circe.Decoder
import io.circe.generic.semiauto._

sealed trait RadiationType
object RadiationType {
  case object Alpha   extends RadiationType
  case object Beta    extends RadiationType
  case object Gamma   extends RadiationType
  case object Neutron extends RadiationType

  // самый простой декодер по строке: "alpha"|"beta"|"gamma"|"neutron" (регистр игнорируем)
  implicit val decoder: Decoder[RadiationType] =
    Decoder.decodeString.emap {
      case s if s.equalsIgnoreCase("alpha")   => Right(Alpha)
      case s if s.equalsIgnoreCase("beta")    => Right(Beta)
      case s if s.equalsIgnoreCase("gamma")   => Right(Gamma)
      case s if s.equalsIgnoreCase("neutron") => Right(Neutron)
      case other => Left(s"unknown radiationType: $other")
    }
}

// единицы —
final case class MicroSieverts(value: Double)        extends AnyVal
final case class MicroSievertsPerHour(value: Double) extends AnyVal

final case class RadiationEvent(
                                 sourceId: String,
                                 radiationType: RadiationType,
                                 intensity: Double,                 // генератор присылает одно поле intensity
                                 coordinates: (Double, Double),     // Circe декодирует Tuple2 из JSON-массива: [x, y]
                                 timestamp: Instant
                               )

object RadiationEvent {
  // deriveDecoder понимает Tuple2[Double,Double] как массив [x,y]
  implicit val decoder: Decoder[RadiationEvent] = deriveDecoder
}
