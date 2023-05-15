package ir.mahdiparastesh.mergen.mem

@Suppress("unused")
class DevManifest(val os: String, val sensors: List<Sensor>) {

    class Sensor(val type: String)
}
