package radiation.sources.model

import java.time.Instant

final case class RadiationSource(
    id: String,
    radiationType: RadiationType,
    profile: TemporalProfile,
    coordinates: (Double, Double)
) {
  def currentIntensity(time: Instant): Double =
    profile.intensityAt(time)
}
