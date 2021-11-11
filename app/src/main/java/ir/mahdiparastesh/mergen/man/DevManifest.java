package ir.mahdiparastesh.mergen.man;

import java.util.List;

public class DevManifest {
    public String OS;
    public List<Sensor> sensors;

    public DevManifest(String OS, List<Sensor> sensors) {
        this.OS = OS;
        this.sensors = sensors;
    }

    public static class Sensor {
        String type;

        public Sensor() {
            this.type = type;
        }
    }
}
