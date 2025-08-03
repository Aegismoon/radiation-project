package radiation.sources.model

sealed trait RadiationType
object RadiationType {
  case object Alpha extends RadiationType
  case object Beta extends RadiationType
  case object Gamma extends RadiationType
  case object Neutron extends RadiationType
}
