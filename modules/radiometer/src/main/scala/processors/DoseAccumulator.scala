package radiometer.processors

import cats.effect.kernel.{Ref, Sync}
import cats.syntax.all._
import domain.{MicroSieverts, PipeDecision, RadiationEvent, StoreDose}
import processors.Processor


final case class DoseState(cumulativeMicroSv: Double)

final class DoseAccumulatorProcessor[F[_]: Sync](
                                                  state: Ref[F, Map[String, DoseState]]
                                                ) extends Processor[F] {

  val name: String = "dose-accumulator"

  def process(e: RadiationEvent): F[List[PipeDecision]] =
    state.modify { m =>
      val prev = m.getOrElse(e.sourceId, DoseState(0.0))
      val cum  = prev.cumulativeMicroSv + e.intensity
      val next = DoseState(cum)
      val out  = StoreDose(e.sourceId, MicroSieverts(cum), e.timestamp) :: Nil
      println(s"[dose] source=${e.sourceId} +${e.intensity} µSv => cum=${cum} µSv at ${e.timestamp}")
      (m.updated(e.sourceId, next), out)
    }
}

object DoseAccumulatorProcessor {
  def make[F[_]: Sync]: F[DoseAccumulatorProcessor[F]] =
    Ref.of[F, Map[String, DoseState]](Map.empty).map(new DoseAccumulatorProcessor[F](_))
}
