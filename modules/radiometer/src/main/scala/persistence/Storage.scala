package radiometer.persistence

import cats.effect.Async
import slick.jdbc.PostgresProfile.api._

import java.time.Instant
import java.sql.Timestamp
import io.circe.Json

import scala.concurrent.ExecutionContext.Implicits.global

// доменные строки
final case class MeasurementRow(
                                 sourceId: String,
                                 valueMicroSvH: Double,
                                 tsUtc: Instant,
                                 severity: String,
                                 meta: Option[Json]
                               )

final case class DoseRow(
                          sourceId: String,
                          cumulativeMicroSv: Double,
                          tsUtc: Instant
                        )

trait Storage[F[_]] {
  def upsertMeasurements(rows: List[MeasurementRow]): F[Int]
  def upsertDoses(rows: List[DoseRow]): F[Int]
}

// реализация на Slick
object Storage {
  final class SlickStorage[F[_]: Async](db: Database) extends Storage[F] {
    private def run[A](a: DBIO[A]): F[A] =
      Async[F].fromFuture(Async[F].delay(db.run(a)))

    def upsertMeasurements(rows: List[MeasurementRow]): F[Int] =
      if (rows.isEmpty) Async[F].pure(0)
      else {
        val actions: Seq[DBIO[Int]] = rows.map { r =>
          sqlu"""
            INSERT INTO radiation_measurements (source_id, value_µsv_h, ts_utc, severity, meta)
            VALUES (${r.sourceId},
                    ${r.valueMicroSvH},
                    ${Timestamp.from(r.tsUtc)},
                    ${r.severity},
                    ${r.meta.map(_.noSpaces)}::jsonb)
            ON CONFLICT (source_id, ts_utc) DO UPDATE
            SET value_µsv_h = EXCLUDED.value_µsv_h,
                severity    = EXCLUDED.severity,
                meta        = COALESCE(EXCLUDED.meta, radiation_measurements.meta);
          """
        }
        // Явно подсказываем тип, чтобы не было ambiguous Numeric
        run(DBIO.sequence(actions).map((xs: Seq[Int]) => xs.sum))
      }

    def upsertDoses(rows: List[DoseRow]): F[Int] =
      if (rows.isEmpty) Async[F].pure(0)
      else {
        val actions: Seq[DBIO[Int]] = rows.map { r =>
          sqlu"""
            INSERT INTO radiation_doses (source_id, cumulative_µsv, ts_utc)
            VALUES (${r.sourceId},
                    ${r.cumulativeMicroSv},
                    ${Timestamp.from(r.tsUtc)})
            ON CONFLICT (source_id, ts_utc) DO UPDATE
            SET cumulative_µsv = EXCLUDED.cumulative_µsv;
          """
        }
        run(DBIO.sequence(actions).map((xs: Seq[Int]) => xs.sum))
      }
  }

  def slick[F[_]: Async](db: Database): Storage[F] =
    new SlickStorage[F](db)
}
