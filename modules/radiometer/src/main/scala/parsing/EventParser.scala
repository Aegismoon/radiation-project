package radiometer.parsing

import cats.effect.Sync
import domain.RadiationEvent
import io.circe.parser.decode


trait EventParser[F[_]] {
  def parse(key: Option[String], value: String): F[RadiationEvent]
}

/**  просто decode[RadiationEvent](json). */
final class EventParserCirce[F[_]: Sync] extends EventParser[F] {
  override def parse(key: Option[String], value: String): F[RadiationEvent] =
    Sync[F].fromEither(decode[RadiationEvent](value))
}

object EventParser {
  def circe[F[_]: Sync]: EventParser[F] =
    new EventParserCirce[F]
}
