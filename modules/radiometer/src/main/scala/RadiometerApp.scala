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
    val settings = ConsumerSettings[IO, String, String]
      .withBootstrapServers(sys.env.getOrElse("KAFKA_BOOTSTRAP", "localhost:29092"))
      .withGroupId(sys.env.getOrElse("KAFKA_GROUP_ID", "radiometer-3"))
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
      for {
        dose <- DoseAccumulatorProcessor.make[IO]
        rate <- RateThresholdProcessor.make[IO](TypeAwareThresholds())
        reg <- ProcessorRegistry.make[IO](List(rate, dose))

        parser = EventParser.circe[IO]
        store = Storage.slick[IO](db)
        router <- Router.make[IO](store)

        pipeline <- FanoutPipeline.make[IO](consumer, parser, reg, router)
        _ <- IO.delay(println("[app] Subscribed to topic: radiation-events"))
        _ <- pipeline.run("radiation-events")
          .handleErrorWith(e => IO.println(s"[ERROR] ${e.getMessage}"))
      } yield ()
    }
  }
}