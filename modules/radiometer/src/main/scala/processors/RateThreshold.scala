package radiometer.processors

import cats.effect.kernel.Sync
import domain.{MicroSievertsPerHour, PipeDecision, RadiationEvent, RadiationType, StoreMeasurement}
import processors.Processor

final case class RateThresholds(elevated: Double, danger: Double)

final case class TypeAwareThresholds(
                                      alpha:   RateThresholds = RateThresholds(0.3, 2.0),
                                      beta:    RateThresholds = RateThresholds(0.5, 3.0),
                                      gamma:   RateThresholds = RateThresholds(0.2, 1.0),
                                      neutron: RateThresholds = RateThresholds(0.1, 0.5),
                                      default: RateThresholds = RateThresholds(0.3, 2.0)
                                    ) {
  def forType(rt: RadiationType): RateThresholds = rt match {
    case RadiationType.Alpha   => alpha
    case RadiationType.Beta    => beta
    case RadiationType.Gamma   => gamma
    case RadiationType.Neutron => neutron
  }
}

final class RateThresholdProcessor[F[_]: Sync](perType: TypeAwareThresholds) extends Processor[F] {
  val name = "rate-threshold"

  private def severity(v: Double, th: RateThresholds): String =
    if (v >= th.danger) "Опасная"
    else if (v >= th.elevated) "Повышенная"
    else "Норма"

  def process(e: RadiationEvent): F[List[PipeDecision]] = {
    val rate = MicroSievertsPerHour(e.intensity)               // трактуем intensity как µSv/h
    val th   = perType.forType(e.radiationType)
    val sev  = severity(rate.value, th)
    Sync[F].delay({ println(s"[rate] source=${e.sourceId} type=${e.radiationType} rate=${rate.value} µSv/h => ${sev}"); List(StoreMeasurement(e.sourceId, rate, e.timestamp, sev, None)) })
  }
}

object RateThresholdProcessor {
  def make[F[_]: Sync](perType: TypeAwareThresholds): F[RateThresholdProcessor[F]] =
    Sync[F].pure(new RateThresholdProcessor[F](perType))
}
