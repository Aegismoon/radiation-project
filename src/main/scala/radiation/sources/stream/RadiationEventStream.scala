package radiation.sources.stream

import cats.effect.Temporal
import cats.syntax.all._
import fs2.Stream
import radiation.sources.model._
import java.time.Instant
import scala.concurrent.duration._

final case class RadiationEvent(
    sourceId: String,
    radiationType: RadiationType,
    intensity: Double,
    coordinates: (Double, Double),
    timestamp: Instant
)

object RadiationEventStream {

  def fromSources[F[_]: Temporal](
      sources: List[RadiationSource],
      interval: FiniteDuration
  ): Stream[F, RadiationEvent] =
    Stream
      .awakeEvery[F](interval)
      .evalMap { _ =>
        val now = Instant.now()
        sources.traverse { src =>
          RadiationEvent(
            sourceId = src.id,
            radiationType = src.radiationType,
            intensity = src.currentIntensity(now),
            coordinates = src.coordinates,
            timestamp = now
          ).pure[F]
        }
      }
      .flatMap(Stream.emits)
}
