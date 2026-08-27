#include <jni.h>
#include <string>
#include <android/log.h>
#include <cmath>
#include <cstdlib>

#define LOG_TAG "AudioEngine-JNI"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

static bool g_engineRunning = false;
static int g_currentMidiNote = -1;
static float g_detectedFrequency = 0.0f;

extern "C" {

JNIEXPORT void JNICALL
Java_com_pocketbansuri_AudioEngine_startEngine(JNIEnv *env, jobject thiz) {
    LOGI("Audio Engine Started (Stub)");
    g_engineRunning = true;
    g_detectedFrequency = 440.0f; // Mock reference frequency (A4)
}

JNIEXPORT void JNICALL
Java_com_pocketbansuri_AudioEngine_stopEngine(JNIEnv *env, jobject thiz) {
    LOGI("Audio Engine Stopped (Stub)");
    g_engineRunning = false;
    g_detectedFrequency = 0.0f;
}

JNIEXPORT void JNICALL
Java_com_pocketbansuri_AudioEngine_playReferenceNote(JNIEnv *env, jobject thiz, jint midi_note) {
    LOGI("Playing Reference Note: %d (Stub)", midi_note);
    g_currentMidiNote = midi_note;
}

JNIEXPORT void JNICALL
Java_com_pocketbansuri_AudioEngine_stopReferenceNote(JNIEnv *env, jobject thiz) {
    LOGI("Stopping Reference Note (Stub)");
    g_currentMidiNote = -1;
}

JNIEXPORT jfloat JNICALL
Java_com_pocketbansuri_AudioEngine_getDetectedFrequency(JNIEnv *env, jobject thiz) {
    if (!g_engineRunning) {
        return 0.0f;
    }
    
    // If a reference note is playing, return its approximate frequency with some random deviation
    if (g_currentMidiNote != -1) {
        // Frequency = 440 * 2^((midi - 69)/12)
        float target = 440.0f * std::pow(2.0f, (g_currentMidiNote - 69) / 12.0f);
        // Add a small fluctuation (-1.0Hz to +1.0Hz) to simulate live mic pitch detection
        float fluctuation = (((float)std::rand() / (float)RAND_MAX) * 2.0f) - 1.0f;
        g_detectedFrequency = target + fluctuation;
    } else {
        // Return fluctuating pitch around base frequency (e.g. 440Hz for A4) or a random rest frequency
        float fluctuation = (((float)std::rand() / (float)RAND_MAX) * 4.0f) - 2.0f;
        g_detectedFrequency = 440.0f + fluctuation;
    }
    return g_detectedFrequency;
}

}
