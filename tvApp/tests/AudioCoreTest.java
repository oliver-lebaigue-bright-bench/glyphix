import com.glyphix.tv.FlashLimiter;
import com.glyphix.tv.SignalAnalyzer;
import java.util.Arrays;

public final class AudioCoreTest {
    private static void check(boolean passed, String message) {
        if (!passed) throw new AssertionError(message);
    }
    public static void main(String[] args) {
        FlashLimiter limiter = new FlashLimiter();
        check(limiter.allow(0), "First pulse allowed");
        check(!limiter.allow(499), "Default prevents more than two pulses per second");
        check(limiter.allow(500), "Pulse allowed after 500 ms");
        limiter.setRate(100);
        check(!limiter.allow(999), "Requested rate is capped at two per second");
        check(limiter.allow(1000), "Cap interval elapsed");
        limiter.setRate(0.5f);
        check(!limiter.allow(2999), "Slower configured rate retained");
        check(limiter.allow(3000), "Slower rate elapsed");
        SignalAnalyzer analyzer = new SignalAnalyzer();
        short[] pcm = new short[1024];
        SignalAnalyzer.Frame silence = analyzer.analyze(pcm, pcm.length, 1, 0);
        check(silence.level == 0 && !silence.beat, "Silence stays black");
        Arrays.fill(pcm, (short) 12000);
        SignalAnalyzer.Frame transientFrame = analyzer.analyze(pcm, pcm.length, 1, 1000);
        check(transientFrame.level > 0 && transientFrame.beat, "Sound triggers level and beat");
        check(!analyzer.analyze(pcm, pcm.length, 1, 1200).beat, "Beat refractory time applied");
        Arrays.fill(pcm, (short)0);
        SignalAnalyzer.Frame release = null;
        for (int i = 0; i < 100; i++) release = analyzer.analyze(pcm, pcm.length, 1, 2000 + i * 24);
        check(release != null && release.level == 0, "Envelope decays to black after sound stops");
        check(analyzer.analyze(pcm, 0, 1, 5000).level == 0, "Empty input handled");
        System.out.println("TV audio and flash limit checks passed");
    }
}
