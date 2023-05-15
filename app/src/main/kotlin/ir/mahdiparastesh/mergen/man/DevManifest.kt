package ir.mahdiparastesh.mergen.man

@Suppress("unused")
class DevManifest(val os: String, val sensors: List<Sensor>) {

    class Sensor(val type: String)
}
