@Serializable
data class Book(
    val id: Int,
    val title: String,
    val author: String,
    val categoryId: Int,
    val cover: String,
    val description: String
)
