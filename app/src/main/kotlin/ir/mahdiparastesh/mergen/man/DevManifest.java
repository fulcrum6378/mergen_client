package ir.mahdiparastesh.mergen.man;

import java.util.List;

@SuppressWarnings("unused,InnerClassMayBeStatic")
public class DevManifest {
    public String OS;
    public List<Sensor> sensors;

    public class Sensor {
        public String type;
    }
}
