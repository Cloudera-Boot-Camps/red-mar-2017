package scenarios

object Measurements {
  final val HBASE_NAME = "measurements"
  final val COL_FAMILY = "cf1"
  final val HBASE_CORE_SITE = "/etc/hbase/conf/core-site.xml"
  final val HBASE_SITE = "/etc/hbase/conf/hbase-site.xml"

  final val KUDU_NAME = "measurements_kudu"
  final val HASH = 3

  final val COL_MEASUREMENTS_ID = "measurement_id"
  final val COL_DETECTOR_ID = "detector_id"
  final val COL_GALAXY_ID = "galaxy_id"
  final val COL_ASTROPHYSICISTS_ID = "astrophysicists_id"
  final val COL_MEASUREMENTS_TIME = "measurement_time"
  final val COL_AMPLITUDE_1 = "amplitude_1"
  final val COL_AMPLITUDE_2 = "amplitude_2"
  final val COL_AMPLITUDE_3 = "amplitude_3"
  final val COL_ANOMALY = "anomaly"
  final val POS_MEASUREMENTS_ID = 0
  final val POS_DETECTOR_ID = 1
  final val POS_GALAXY_ID = 2
  final val POS_ASTROPHYSICISTS_ID = 3
  final val POS_MEASUREMENTS_TIME = 4
  final val POS_AMPLITUDE_1 = 5
  final val POS_AMPLITUDE_2 = 6
  final val POS_AMPLITUDE_3 = 7
  final val POS_ANOMALY = 8
  final val AMP_1_3_CHECK = 0.995
  final val AMP_2_CHECK = 0.005
  final val Y = "Y"
  final val N = "N"

  def anomalyCheck(amp1: Double, amp2: Double, amp3: Double): String = {
    var ret = ""
    if ((amp1 > Measurements.AMP_1_3_CHECK)
      && (amp3 > Measurements.AMP_1_3_CHECK)
      && (amp2 < Measurements.AMP_2_CHECK)) {
      ret = Measurements.Y
    }
    else {
      ret = Measurements.N
    }
    ret
  }
}