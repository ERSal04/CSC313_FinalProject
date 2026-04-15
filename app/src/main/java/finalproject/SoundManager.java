package finalproject;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;

import org.lwjgl.BufferUtils;
import org.lwjgl.openal.AL;
import org.lwjgl.openal.AL10;
import org.lwjgl.openal.ALC;
import org.lwjgl.openal.ALC10;
import org.lwjgl.openal.ALCCapabilities;

public class SoundManager {

    private long device;
    private long context;

    // Buffers — hold the raw audio data
    private int bufOcean;
    private int bufRumble;
    private int bufRain;
    private int bufImpact;

    // Sources — represent playing instances
    private int srcOcean;
    private int srcRumble;
    private int srcRain;
    private int srcImpact;

    private boolean impactPlayed = false;
    private float   impactTimer  = 0.0f;
    private static final float IMPACT_DURATION = 3.0f;

    public SoundManager() {
        device = ALC10.alcOpenDevice((ByteBuffer) null);
        if (device == 0)
            throw new RuntimeException("Failed to open audio device");

        ALCCapabilities deviceCaps = ALC.createCapabilities(device);
        context = ALC10.alcCreateContext(device, (IntBuffer) null);
        ALC10.alcMakeContextCurrent(context);
        AL.createCapabilities(deviceCaps);

        bufOcean  = loadWav("sounds/ocean_ambient.wav");
        bufRumble = loadWav("sounds/tsunami_rumble.wav");
        bufRain   = loadWav("sounds/rain_loop.wav");
        bufImpact = loadWav("sounds/wave_impact.wav");

        srcOcean  = createSource(bufOcean,  true,  0.4f);
        srcRumble = createSource(bufRumble, true,  0.0f);
        srcRain   = createSource(bufRain,   true,  0.0f);
        srcImpact = createSource(bufImpact, false, 1.0f);

        AL10.alSourcePlay(srcOcean);
    }

    private int loadWav(String resourcePath) {
        try {
            InputStream is = getClass().getClassLoader()
                .getResourceAsStream(resourcePath);
            if (is == null)
                throw new RuntimeException("Sound file not found: " + resourcePath);

            AudioInputStream ais = AudioSystem.getAudioInputStream(is);
            AudioFormat fmt      = ais.getFormat();

            byte[] bytes = ais.readAllBytes();
            ByteBuffer data = BufferUtils.createByteBuffer(bytes.length);
            data.put(bytes).flip();

            int channels = fmt.getChannels();
            int bits     = fmt.getSampleSizeInBits();
            int alFormat;
            if      (channels == 1 && bits == 8)  alFormat = AL10.AL_FORMAT_MONO8;
            else if (channels == 1 && bits == 16) alFormat = AL10.AL_FORMAT_MONO16;
            else if (channels == 2 && bits == 8)  alFormat = AL10.AL_FORMAT_STEREO8;
            else                                   alFormat = AL10.AL_FORMAT_STEREO16;

            int buffer = AL10.alGenBuffers();
            AL10.alBufferData(buffer, alFormat, data, (int) fmt.getSampleRate());
            return buffer;

        } catch (Exception e) {
            System.err.println("Failed to load sound: " + resourcePath
                + " — " + e.getMessage());
            return 0;
        }
    }

    private int createSource(int buffer, boolean loop, float gain) {
        int src = AL10.alGenSources();
        AL10.alSourcei(src, AL10.AL_BUFFER,  buffer);
        AL10.alSourcei(src, AL10.AL_LOOPING, loop ? AL10.AL_TRUE : AL10.AL_FALSE);
        AL10.alSourcef(src, AL10.AL_GAIN,    gain);
        AL10.alSourcef(src, AL10.AL_PITCH,   1.0f);
        return src;
    }

    public void update(boolean tsunamiActive, float rainIntensity,
                       float arrivalFactor, float tsunamiTime) {

        float master = 0.5f;

        // --- Ocean ambient ---
        if (tsunamiActive) {
            float currentOcean = AL10.alGetSourcef(srcOcean, AL10.AL_GAIN);
            float fadedGain    = lerp(currentOcean, 0.0f, 0.05f);
            AL10.alSourcef(srcOcean, AL10.AL_GAIN, fadedGain);
            if (fadedGain < 0.01f && isPlaying(srcOcean))
                AL10.alSourceStop(srcOcean);
        } else {
            if (!isPlaying(srcOcean))
                AL10.alSourcePlay(srcOcean);
            float currentOcean = AL10.alGetSourcef(srcOcean, AL10.AL_GAIN);
            float restoredGain = lerp(currentOcean, 0.4f * master, 0.02f);
            AL10.alSourcef(srcOcean, AL10.AL_GAIN, restoredGain);
        }

        // --- Tsunami rumble ---
        float rumbleTarget  = tsunamiActive ? 0.9f : 0.0f;
        float rumbleCurrent = AL10.alGetSourcef(srcRumble, AL10.AL_GAIN);
        float rumbleGain    = lerp(rumbleCurrent, rumbleTarget * master, 0.02f);
        AL10.alSourcef(srcRumble, AL10.AL_GAIN, rumbleGain);

        if (tsunamiActive && !isPlaying(srcRumble))
            AL10.alSourcePlay(srcRumble);
        else if (!tsunamiActive) {
            float g = AL10.alGetSourcef(srcRumble, AL10.AL_GAIN);
            AL10.alSourcef(srcRumble, AL10.AL_GAIN, g * 0.9f);
            if (g < 0.02f && isPlaying(srcRumble))
                AL10.alSourceStop(srcRumble);
        }

        // --- Rain ---
        float rainGain = rainIntensity * 0.15f * master;
        AL10.alSourcef(srcRain, AL10.AL_GAIN, rainGain);
        if (rainIntensity > 0.05f && !isPlaying(srcRain))
            AL10.alSourcePlay(srcRain);
        else if (rainIntensity < 0.01f && isPlaying(srcRain))
            AL10.alSourceStop(srcRain);

        // --- Wave impact ---
        if (tsunamiActive && arrivalFactor > 0.4f && !impactPlayed) {
            AL10.alSourcef(srcImpact, AL10.AL_GAIN, 0.4f * master);
            AL10.alSourcePlay(srcImpact);
            impactPlayed = true;
            impactTimer  = 0.0f;
        }

        // Fade out impact after IMPACT_DURATION seconds
        if (impactPlayed && isPlaying(srcImpact)) {
            impactTimer += 0.016f;
            if (impactTimer >= IMPACT_DURATION) {
                float g = AL10.alGetSourcef(srcImpact, AL10.AL_GAIN);
                AL10.alSourcef(srcImpact, AL10.AL_GAIN, g * 0.85f);
                if (g < 0.02f) AL10.alSourceStop(srcImpact);
            }
        }

        if (!tsunamiActive) {
            impactPlayed = false;
            impactTimer  = 0.0f;
        }
    }

    private boolean isPlaying(int source) {
        return AL10.alGetSourcei(source, AL10.AL_SOURCE_STATE)
               == AL10.AL_PLAYING;
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    public void cleanup() {
        AL10.alSourceStop(srcOcean);
        AL10.alSourceStop(srcRumble);
        AL10.alSourceStop(srcRain);
        AL10.alSourceStop(srcImpact);

        AL10.alDeleteSources(srcOcean);
        AL10.alDeleteSources(srcRumble);
        AL10.alDeleteSources(srcRain);
        AL10.alDeleteSources(srcImpact);

        AL10.alDeleteBuffers(bufOcean);
        AL10.alDeleteBuffers(bufRumble);
        AL10.alDeleteBuffers(bufRain);
        AL10.alDeleteBuffers(bufImpact);

        ALC10.alcDestroyContext(context);
        ALC10.alcCloseDevice(device);
    }
}