package processors

import domain.{PipeDecision, RadiationEvent}

trait Processor[F[_]] {
  def name: String
  def process(e: RadiationEvent): F[List[PipeDecision]]
}
