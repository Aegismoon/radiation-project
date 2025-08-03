package radiation.sources.stream

import cats.effect.{Temporal, Ref}
import cats.syntax.all._
import fs2.Stream
import radiation.sources.model._
import radiation.sources.generator.RadiationSourceGenerator
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

  def hybridSourceStream[F[_]: Temporal](
                                          initialCount: Int,
                                          interval: FiniteDuration
                                        ): Stream[F, RadiationEvent] = {

    // Добавляет новый источник, удаляя самый старый, если их стало больше 10
    def updateSources(sourcesRef: Ref[F, List[RadiationSource]]): F[Unit] =
      for {
        newSource <- RadiationSourceGenerator.randomSource[F]
        _ <- sourcesRef.update { sources =>
          val trimmed = if (sources.size >= 10) sources.drop(1) else sources
          trimmed :+ newSource
        }
      } yield ()

    for {
      // Инициализация списка источников и хранение его в Ref
      ref <- Stream.eval(
        List.fill(initialCount)(RadiationSourceGenerator.randomSource[F])
          .sequence
          .flatMap(Ref.of[F, List[RadiationSource]])
      )
      // Тики времени
      tick <- Stream.awakeEvery[F](interval)
      now <- Stream.eval(Temporal[F].realTimeInstant)

      // С ~30% вероятностью добавляется новый источник
      _ <- Stream.eval(updateSources(ref).whenA(scala.util.Random.nextDouble() < 0.3))

      // Получение актуального списка источников
      sources <- Stream.eval(ref.get)

      // Преобразование источников в события
      events <- Stream.emits(sources.map { src =>
        RadiationEvent(
          sourceId = src.id,
          radiationType = src.radiationType,
          intensity = src.currentIntensity(now),
          coordinates = src.coordinates,
          timestamp = now
        )
      })
    } yield events
  }
}
