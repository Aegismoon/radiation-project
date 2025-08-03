package radiation.sources.generator

import cats.effect.Sync
import cats.syntax.all._
import java.time.Instant
import java.util.UUID

import radiation.sources.model._
import scala.util._

object RadiationSourceGenerator {
  def randomSource[F[_]: Sync]: F[RadiationSource] = Sync[F].delay {
    val types = List(RadiationType.Alpha, RadiationType.Beta, RadiationType.Gamma, RadiationType.Neutron)
    val rType = types(Random.nextInt(types.size))

    val profile = Random.nextInt(3) match {
      case 0 => ConstantLevel(0.1 + Random.nextDouble() * 5)
      case 1 => PulsedEmission(0.05, 5.0, 3600, 300, Instant.now())
      case 2 => DegradingSource(10.0, 0.01 + scala.util.Random.nextDouble() * 0.1, Instant.now())
    }

    RadiationSource(
      id = UUID.randomUUID().toString,
      radiationType = rType,
      profile = profile,
      coordinates = (
        scala.util.Random.nextDouble() * 100,
        scala.util.Random.nextDouble() * 100
      )
    )
  }
}
