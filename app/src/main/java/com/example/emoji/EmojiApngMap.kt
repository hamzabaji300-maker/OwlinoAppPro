package com.example.emoji

object EmojiApngMap {

    // رابط الإيموجيات المتحركة (مستضاف على GitHub + jsDelivr CDN)
    private const val BASE_URL = "https://cdn.jsdelivr.net/gh/hanikanon/owlino-emojis@main/emoji/apng"

    private val emojiToAsset: Map<String, String> = mapOf(
        "\uD83D\uDE00" to "grinning_face_animated.png",                    // 😀
        "\uD83D\uDE03" to "grinning_face_with_big_eyes_animated.png",      // 😃
        "\uD83D\uDE04" to "grinning_face_with_smiling_eyes_animated.png",  // 😄
        "\uD83D\uDE01" to "beaming_face_with_smiling_eyes_animated.png",   // 😁
        "\uD83D\uDE06" to "grinning_squinting_face_animated.png",          // 😆
        "\uD83D\uDE05" to "grinning_face_with_sweat_animated.png",         // 😅
        "\uD83D\uDE02" to "face_with_tears_of_joy_animated.png",           // 😂
        "\uD83E\uDD23" to "rolling_on_the_floor_laughing_animated.png",    // 🤣
        "\uD83D\uDE42" to "slightly_smiling_face_animated.png",            // 🙂
        "\uD83D\uDE43" to "upside_down_face_animated.png",                 // 🙃
        "\uD83D\uDE09" to "winking_face_animated.png",                     // 😉
        "\uD83D\uDE0A" to "smiling_face_with_smiling_eyes_animated.png",   // 😊
        "\uD83D\uDE07" to "smiling_face_with_halo_animated.png",           // 😇
        "\uD83E\uDEE0" to "melting_face_animated.png",                     // 🫠
        // زيد هنا أي إيموجي جديد تحطو في الباكت — سطر وحد كافي، بلا ما تضيف شيء في التطبيق نفسه
    )

    fun assetFor(emoji: String): String? = emojiToAsset[emoji]

    // الرابط الكامل اللي يتحمل منه الإيموجي (Glide يتكفل بالتحميل + الكاش)
    fun remoteUrlFor(emoji: String): String? =
        assetFor(emoji)?.let { "$BASE_URL/$it" }

    fun isSupported(emoji: String): Boolean = emojiToAsset.containsKey(emoji)
    fun supportedEmojis(): Set<String> = emojiToAsset.keys
}
