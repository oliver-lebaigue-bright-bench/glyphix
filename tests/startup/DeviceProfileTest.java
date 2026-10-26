import com.glyphix.app.model.DeviceProfile;
import com.glyphix.app.util.PreferenceValues;

public final class DeviceProfileTest {
    private enum Mode { MAX, BEAT }
    private static int checks;
    private static void equal(Object expected, Object actual) {
        checks++;
        if (!expected.equals(actual)) throw new AssertionError(expected + " != " + actual);
    }
    public static void main(String[] args) {
        String[] pro = {"A059P", "a059p", "24111P", "Phone (3a) Pro", "Nothing Phone (3a) Pro", "Phone 3a Pro"};
        for (String model : pro) {
            int device = DeviceProfile.detectModel(model);
            equal(DeviceProfile.DEVICE_NP3APRO, device);
            equal("Phone (3a) Pro", DeviceProfile.deviceName(device));
            equal(36, DeviceProfile.getLedCount(device));
            equal(0, DeviceProfile.getMatrixWidth(device));
            equal(0, DeviceProfile.getMatrixHeight(device));
        }
        equal(DeviceProfile.DEVICE_NP3A, DeviceProfile.detectModel("A059"));
        equal(DeviceProfile.DEVICE_NP3A, DeviceProfile.detectModel("Phone (3a)"));
        equal(DeviceProfile.DEVICE_NP4A, DeviceProfile.detectModel("A069"));
        equal(DeviceProfile.DEVICE_NP4APRO, DeviceProfile.detectModel("A069P"));
        equal("Phone (4a) Pro", DeviceProfile.deviceName(DeviceProfile.DEVICE_NP4APRO));
        equal(169, DeviceProfile.getLedCount(DeviceProfile.DEVICE_NP4APRO));
        equal(13, DeviceProfile.getMatrixWidth(DeviceProfile.DEVICE_NP4APRO));
        equal(DeviceProfile.DEVICE_NP1, DeviceProfile.detectModel("A063"));
        equal(DeviceProfile.DEVICE_NP2, DeviceProfile.detectModel("AIN065"));
        equal(DeviceProfile.DEVICE_NP2A, DeviceProfile.detectModel("A142P"));
        equal(DeviceProfile.DEVICE_NP3, DeviceProfile.detectModel("A024"));
        for (String model : new String[] {null, "", "Pixel 9", "Phone 3ab", "FAKE-A059P"}) {
            equal(DeviceProfile.DEVICE_UNKNOWN, DeviceProfile.detectModel(model));
        }
        equal(Mode.BEAT, PreferenceValues.enumOrDefault(Mode.class, "BEAT", Mode.MAX));
        equal(Mode.MAX, PreferenceValues.enumOrDefault(Mode.class, null, Mode.MAX));
        equal(Mode.MAX, PreferenceValues.enumOrDefault(Mode.class, "removed-beta-mode", Mode.MAX));
        equal(Mode.MAX, PreferenceValues.enumOrDefault(Mode.class, "", Mode.MAX));
        System.out.println(checks + " startup/device assertions passed");
    }
}
