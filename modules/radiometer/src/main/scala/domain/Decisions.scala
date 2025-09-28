

package domain

sealed trait PipeDecision

/**
 *  ADT решение для того что делать с обработанным событием
 *  результат обработки процессора
 *  */

/** сохранить измерение  */
final case class StoreMeasurement(
                                   sourceId: String, value: MicroSievertsPerHour, ts: java.time.Instant, severity: String, meta: Option[io.circe.Json]
                                 ) extends PipeDecision
/** сохранить накопленную дозу */
final case class StoreDose(
                            sourceId: String, cumulative: MicroSieverts, ts: java.time.Instant   // на момент ts накопленная доза
                          ) extends PipeDecision
/** игнорировать событие */
final case class Drop(reason: String) extends PipeDecision
