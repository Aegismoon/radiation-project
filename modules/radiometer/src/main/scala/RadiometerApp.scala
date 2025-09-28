package radiometer.app

import cats.effect.{IO, IOApp, Resource}
import cats.implicits.catsSyntaxTuple2Semigroupal
import fs2.kafka._
import slick.jdbc.PostgresProfile.api._
import radiometer.parsing._
import radiometer.persistence._
import radiometer.pipeline._
import radiometer.processors._

object RadiometerApp extends IOApp.Simple {

  private def mkConsumer: Resource[IO, KafkaConsumer[IO, String, String]] = {
    val bootstrap = sys.env.get("KAFKA_BOOTSTRAP")
      .orElse(sys.env.get("KAFKA_BOOTSTRAP_SERVERS"))
      .getOrElse("localhost:9092")
    val groupId = sys.env.getOrElse("KAFKA_GROUP_ID", "radiometer-1")

    val settings =
      ConsumerSettings[IO, String, String]
        .withBootstrapServers(bootstrap)
        .withGroupId(groupId)
        .withAutoOffsetReset(AutoOffsetReset.Earliest)
    KafkaConsumer.resource(settings)
  }


  private def mkDatabase: Resource[IO, Database] = {
    val url = sys.env.getOrElse("DB_URL", "jdbc:postgresql://localhost:5432/radiation")
    val user = sys.env.getOrElse("DB_USER", "radiation")
    val pass = sys.env.getOrElse("DB_PASSWORD", "radiation")

    Resource.make {
      IO.delay(Database.forURL(url = url, user = user, password = pass, driver = "org.postgresql.Driver"))
    } { db =>
      IO.delay(db.close()).handleError(_ => ()) // безопасное закрытие
    }
  }

  def run: IO[Unit] = {
    println("[app] Radiometer starting…")

    (mkConsumer, mkDatabase).tupled.use { case (consumer, db) =>
      val topic = "radiation-events"
      for {
        _ <- IO.println(s"[app] Subscribed to topic: $topic")
        // ВАЖНО: подписка и бесконечный стрим records
        _ <- consumer.subscribeTo(topic) *>
          consumer.records
            .evalMap { cr =>
              IO.println(s"[got] key=${cr.record.key} value=${cr.record.value.take(120)}")
            }
            .handleErrorWith(e => Stream.eval(IO.println(s"[consumer ERROR] ${e.getMessage}")) >> Stream.raiseError[IO](e))
            .onFinalize(IO.println("[consumer] stopping"))
            .compile
            .drain
      } yield ()
    }
  }
}