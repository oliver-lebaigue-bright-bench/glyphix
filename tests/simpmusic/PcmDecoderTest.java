import com.maxrave.media3.audio.GlyphixPcmDecoder;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

public final class PcmDecoderTest {
    static void check(boolean condition, String name) {
        if (!condition) throw new AssertionError(name);
    }
    static ByteBuffer pcm(short... values) {
        ByteBuffer b = ByteBuffer.allocate(values.length * 2).order(ByteOrder.LITTLE_ENDIAN);
        for (short value : values) b.putShort(value);
        return b.flip();
    }
    public static void main(String[] args) {
        ByteBuffer mono = pcm((short)-32768, (short)0, (short)32767);
        check(Arrays.equals(GlyphixPcmDecoder.readMono(mono, 1, false, 8192),
                new short[]{-32768, 0, 32767}), "PCM16 signed limits");
        ByteBuffer stereo = pcm((short)32767, (short)32767, (short)-32768, (short)-32768, (short)100, (short)-100);
        check(Arrays.equals(GlyphixPcmDecoder.readMono(stereo, 2, false, 8192),
                new short[]{32767, -32768, 0}), "stereo average without overflow");
        ByteBuffer floats = ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN);
        for (float value : new float[]{2f,-2f,Float.NaN,Float.POSITIVE_INFINITY,.5f,-.5f}) floats.putFloat(value);
        floats.flip();
        check(Arrays.equals(GlyphixPcmDecoder.readMono(floats, 1, true, 8192),
                new short[]{32767,-32767,0,0,16384,-16383}), "float clipping and invalid samples");
        ByteBuffer bounded = pcm((short)1,(short)2,(short)3);
        check(GlyphixPcmDecoder.readMono(bounded,1,false,2).length == 2 && bounded.remaining() == 2,
                "bounded chunks retain the next frame");
        ByteBuffer incomplete = ByteBuffer.wrap(new byte[]{1,0,2});
        check(GlyphixPcmDecoder.readMono(incomplete,2,false,8192).length == 0 && incomplete.position() == 0,
                "incomplete frame never overreads");
        ByteBuffer original = pcm((short)42,(short)84);
        GlyphixPcmDecoder.readMono(original.asReadOnlyBuffer(),1,false,8192);
        check(original.position() == 0 && original.getShort() == 42, "tap leaves playback buffer untouched");
        try { GlyphixPcmDecoder.readMono(original,0,false,10); throw new AssertionError("invalid channels"); }
        catch (IllegalArgumentException expected) { }
        System.out.println("7 PCM decoder checks passed");
    }
}
