package client.service;

import javax.sound.sampled.*;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

/** Ghi âm (xuất WAV) và phát lại tin nhắn thoại. */
public class AudioService {
    private final AudioFormat fmt = new AudioFormat(16000, 16, 1, true, false);
    private volatile boolean recording;
    private TargetDataLine line;
    private ByteArrayOutputStream raw;
    private Thread thread;
    private long startTime;
    private long lastDuration;

    public boolean isRecording() { return recording; }
    public long getLastDuration() { return lastDuration; }

    public void startRecording() throws Exception {
        line = (TargetDataLine) AudioSystem.getLine(new DataLine.Info(TargetDataLine.class, fmt));
        line.open(fmt);
        line.start();
        raw = new ByteArrayOutputStream();
        recording = true;
        startTime = System.currentTimeMillis();
        thread = new Thread(() -> {
            byte[] buf = new byte[4096];
            while (recording) {
                int n = line.read(buf, 0, buf.length);
                if (n > 0) raw.write(buf, 0, n);
            }
        });
        thread.start();
    }

    /** @return dữ liệu WAV, hoặc null nếu bản ghi quá ngắn. Ném lỗi nếu micro không thu được tiếng. */
    public byte[] stopRecording() throws Exception {
        recording = false;
        line.stop();
        line.close();
        thread.join(500);
        lastDuration = Math.max(1, (System.currentTimeMillis() - startTime) / 1000);
        byte[] pcm = raw.toByteArray();
        if (pcm.length < 3200) return null;

        if (peak(pcm) < 200) {
            throw new Exception("Micro không thu được âm thanh (im lặng).\n"
                    + "Hãy kiểm tra: micro mặc định của Windows, và Settings > Privacy > Microphone "
                    + "đã cho phép ứng dụng truy cập micro.");
        }

        ByteArrayOutputStream wav = new ByteArrayOutputStream();
        AudioSystem.write(new AudioInputStream(new ByteArrayInputStream(pcm), fmt,
                pcm.length / fmt.getFrameSize()), AudioFileFormat.Type.WAVE, wav);
        return wav.toByteArray();
    }

    /** Biên độ lớn nhất của dữ liệu PCM 16-bit little-endian. */
    private static int peak(byte[] pcm) {
        int max = 0;
        for (int i = 0; i + 1 < pcm.length; i += 2) {
            int s = (short) ((pcm[i + 1] << 8) | (pcm[i] & 0xFF));
            max = Math.max(max, Math.abs(s));
        }
        return max;
    }

    /** Phát WAV bằng SourceDataLine trên một luồng riêng (ổn định hơn Clip). */
    public static void play(byte[] wav) throws Exception {
        AudioInputStream ais = AudioSystem.getAudioInputStream(new ByteArrayInputStream(wav));
        AudioFormat f = ais.getFormat();
        SourceDataLine out = AudioSystem.getSourceDataLine(f);
        out.open(f);
        out.start();
        new Thread(() -> {
            byte[] buf = new byte[4096];
            try {
                int n;
                while ((n = ais.read(buf, 0, buf.length)) > 0) out.write(buf, 0, n);
                out.drain();
            } catch (Exception ignored) {
            } finally {
                out.close();
            }
        }, "voice-player").start();
    }
}