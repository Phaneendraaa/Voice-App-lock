package com.phani.voicelock.voice

/** Which pair of on-device models to use. Bigger = more accurate, slower, bigger download. */
enum class ModelProfile(
    val label: String,
    val description: String,
    /** Whisper variant, e.g. "small.en" (name used by sherpa-onnx release archives). */
    val whisper: String,
    /** WeSpeaker speaker-embedding file in the sherpa-onnx release. */
    val speakerFile: String,
    val approxMb: Int,
) {
    BALANCED(
        label = "Balanced",
        description = "Whisper base.en + WeSpeaker ResNet221. Faster unlock (about 2 s), download about 300 MB.",
        whisper = "base.en",
        speakerFile = "wespeaker_en_voxceleb_resnet221_LM.onnx",
        approxMb = 300,
    ),
    MAX(
        label = "Max accuracy",
        description = "Whisper small.en + WeSpeaker ResNet293. Most accurate, slower unlock (about 4-6 s), download about 720 MB.",
        whisper = "small.en",
        speakerFile = "wespeaker_en_voxceleb_resnet293_LM.onnx",
        approxMb = 720,
    ),
}
