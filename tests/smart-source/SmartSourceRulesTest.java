import com.glyphix.app.logic.smartcapture.SmartSourceRules;
import java.util.Locale;

public final class SmartSourceRulesTest {
    private static void check(boolean condition) { if (!condition) throw new AssertionError(); }
    public static void main(String[] args) throws Exception {
        String key = SmartSourceRules.key("Björk", "Jóga", 300000, "ranges1");
        check(key.matches("v2_[a-f0-9]{64}"));
        check(key.equals(SmartSourceRules.key(" BJÖRK ", "Jóga", 300000, "ranges1")));
        check(!key.equals(SmartSourceRules.key("Bjork", "Joga", 300000, "ranges1")));
        check(!key.equals(SmartSourceRules.key("Björk", "Jóga", 300001, "ranges1")));
        check(!key.equals(SmartSourceRules.key("Björk", "Jóga", 300000, "ranges2")));
        Locale original = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr-TR"));
        check(SmartSourceRules.key("I", "TITLE", 1000, "x").equals(SmartSourceRules.key("i", "title", 1000, "x")));
        Locale.setDefault(original);
        check(SmartSourceRules.key("", "song", 2000, "x").isEmpty());
        check(SmartSourceRules.key("artist", "song", 0, "x").isEmpty());
        check(SmartSourceRules.complete(100000, 200, 99900, 100, 1000));
        check(!SmartSourceRules.complete(100000, 30000, 99900, 100, 1000));
        check(!SmartSourceRules.complete(100000, 0, 30000, 100, 1000));
        check(!SmartSourceRules.complete(100000, 0, 99900, 10000, 1000));
        long[] timestamps = { 100, 200, 300 };
        check(SmartSourceRules.frameIndex(timestamps, 99) == -1);
        check(SmartSourceRules.frameIndex(timestamps, 200) == 1);
        check(SmartSourceRules.frameIndex(timestamps, 999) == 2);
        check(SmartSourceRules.frameIndex(timestamps, 100) == 0);
        check(SmartSourceRules.frameIndex(new long[0], 100) == -1);
        check(SmartSourceRules.readBounded(new java.io.ByteArrayInputStream(new byte[3]), 8).length == 3);
        check(SmartSourceRules.readBounded(new java.io.ByteArrayInputStream(new byte[8]), 8).length == 8);
        try {
            SmartSourceRules.readBounded(new java.io.ByteArrayInputStream(new byte[9]), 8);
            throw new AssertionError("Oversized body accepted");
        } catch (java.io.IOException expected) { }
        System.out.println("Smart Source identity, coverage, seek and response-bound tests passed (21 assertions).");
    }
}
