import cats.effect.{IO, IOApp}
import cats.implicits.toTraverseOps
import radiation.sources.generator.RadiationSourceGenerator
import radiation.sources.stream.RadiationEventStream

import scala.concurrent.duration._

object RadiationSourcesApp extends IOApp.Simple {

  def run: IO[Unit] = for {
    sources <- List.fill(5)(RadiationSourceGenerator.randomSource[IO]).sequence
    _ <- RadiationEventStream
      .fromSources[IO](sources, interval = 2.seconds)
      .evalMap(event => IO.println(event))
      .compile
      .drain
  } yield ()
}
