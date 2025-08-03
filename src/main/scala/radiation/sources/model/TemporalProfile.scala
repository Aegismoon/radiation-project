package radiation.sources.model

import java.time.Instant

sealed trait TemporalProfile {
  def intensityAt(time: Instant): Double
}

final case class ConstantLevel(level: Double) extends TemporalProfile {
  override def intensityAt(time: Instant): Double = level
}

final case class PulsedEmission(
    baseLevel: Double,
    peakLevel: Double,
    pulseIntervalSeconds: Long,
    pulseDurationSeconds: Long,
    startTime: Instant
) extends TemporalProfile {
  override def intensityAt(time: Instant): Double = {
    val elapsed = java.time.Duration.between(startTime, time).getSeconds
    val cyclePos = elapsed % pulseIntervalSeconds
    if (cyclePos < pulseDurationSeconds) peakLevel else baseLevel
  }
}

final case class DegradingSource(
    initialLevel: Double,
    decayRatePerHour: Double,
    startTime: Instant
) extends TemporalProfile {
  override def intensityAt(time: Instant): Double = {
    val hours = java.time.Duration.between(startTime, time).toHours.toDouble
    initialLevel * math.exp(-decayRatePerHour * hours)
  }
}
