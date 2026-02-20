data class Game(
    val name: String,
    val rating: Double,
    val platforms: List<String>,
    val imageResId: Int,
    val id: Int,
    val imageUrl: String? = null
)