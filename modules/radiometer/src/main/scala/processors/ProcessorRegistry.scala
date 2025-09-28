package radiometer.processors

import cats.effect.kernel.Sync
import cats.syntax.all._
import processors.Processor

/** Контейнер со списком процессоров для пайплайна */
final case class ProcessorRegistry[F[_]](processors: List[Processor[F]])

object ProcessorRegistry {
  def make[F[_]: Sync](processors: List[Processor[F]]): F[ProcessorRegistry[F]] =
    Sync[F].pure(ProcessorRegistry(processors))
}
