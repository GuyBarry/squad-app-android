data class Game(
    val name: String,
    val platforms: List<String>,
    val imageResId: Int,
    val id: Int,
    val imageUrl: String? = null
)