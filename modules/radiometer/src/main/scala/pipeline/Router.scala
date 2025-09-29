package radiometer.pipeline

import cats.effect.Async
import cats.syntax.all._
import domain.{PipeDecision, StoreDose, StoreMeasurement}
import radiometer.persistence._

/**
 * Router исполняет решения PipeDecision:
 *  - раскладывает их по типам (measurements, doses),
 *  - вызывает соответствующие upsert'ы в Storage,
 *  - возвращает F[Unit].
 */
final class Router[F[_]: Async](storage: Storage[F]) {

  def route(ds: List[PipeDecision]): F[Unit] = {
    val meas: List[MeasurementRow] = ds.collect {
      case StoreMeasurement(id, rate, ts, sev, meta) =>
        MeasurementRow(id, rate.value, ts, sev, meta)
    }
    val doses: List[DoseRow] = ds.collect {
      case StoreDose(id, cum, ts) =>
        DoseRow(id, cum.value, ts)
    }

    val f1 = if (meas.nonEmpty) storage.upsertMeasurements(meas) else Async[F].pure(0)
    val f2 = if (doses.nonEmpty) storage.upsertDoses(doses)     else Async[F].pure(0)
    for {
      _ <- Async[F].delay(println(s"[router] measurements=${meas.size}, doses=${doses.size}"))
      a <- f1
      b <- f2
      _ <- Async[F].delay(println(s"[router] upserted: measurements=$a, doses=$b"))
    } yield ()
  }
}

object Router {
  def make[F[_]: Async](storage: Storage[F]): F[Router[F]] =
    Async[F].pure(new Router[F](storage))
}
