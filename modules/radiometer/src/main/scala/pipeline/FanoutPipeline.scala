package radiometer.pipeline

import cats.effect.kernel.Async
import cats.syntax.all._
import fs2.kafka._
import radiometer.parsing._
import radiometer.processors._

final class FanoutPipeline[F[_]: Async](
                                         consumer: KafkaConsumer[F, String, String],
                                         parser: EventParser[F],
                                         registry: ProcessorRegistry[F],
                                         router: Router[F]
                                       ) {

  def run(topic: String): F[Unit] =
    consumer.subscribeTo(topic) *>
      consumer.records.evalMap { comm =>
        val rec = comm.record
        val key = rec.key
        val raw = rec.value
        for {
          _         <- Async[F].delay(println(s"[pipeline] raw: $raw"))
          event     <- parser.parse(Some(key), raw)
          _         <- Async[F].delay(println(s"[pipeline] parsed event: $event"))
          decisions <- registry.processors.traverse(_.process(event)).map(_.flatten)
          _         <- Async[F].delay(println(s"[pipeline] decisions: ${decisions.map(_.getClass.getSimpleName)}"))
          _         <- router.route(decisions)
        } yield ()
      }.compile.drain
}

object FanoutPipeline {
  def make[F[_]: Async](
                         consumer: KafkaConsumer[F, String, String],
                         parser: EventParser[F],
                         registry: ProcessorRegistry[F],
                         router: Router[F]
                       ): F[FanoutPipeline[F]] =
    Async[F].pure(new FanoutPipeline[F](consumer, parser, registry, router))
}
