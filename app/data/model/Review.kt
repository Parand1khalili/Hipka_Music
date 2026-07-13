@Serializable
data class Review(
    val bookId: Int,
    val user: String,
    val text: String
)
