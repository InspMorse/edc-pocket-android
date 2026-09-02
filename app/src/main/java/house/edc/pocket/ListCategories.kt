package house.edc.pocket

enum class GroceryAisle(val label: String, private val keywords: List<String>) {
    PRODUCE("Produce", listOf("apple", "banana", "veg", "salad", "fruit", "tomato", "onion", "potato")),
    DAIRY("Dairy", listOf("milk", "cheese", "yogurt", "butter", "cream", "eggs")),
    BAKERY("Bakery", listOf("bread", "bagel", "roll", "croissant", "cake")),
    MEAT("Meat & fish", listOf("chicken", "beef", "pork", "fish", "salmon", "bacon", "sausage")),
    FROZEN("Frozen", listOf("frozen", "ice cream", "pizza")),
    DRINKS("Drinks", listOf("juice", "water", "wine", "beer", "coffee", "tea", "cola")),
    HOUSEHOLD("Household", listOf("soap", "detergent", "toilet", "kitchen roll", "bin bag")),
    OTHER("Other", emptyList()),
    ;

    companion object {
        fun infer(text: String, override: String = ""): GroceryAisle {
            if (override.isNotBlank()) {
                entries.find { it.label.equals(override, ignoreCase = true) || it.name.equals(override, ignoreCase = true) }
                    ?.let { return it }
            }
            val lower = text.lowercase()
            return entries.firstOrNull { aisle ->
                aisle != OTHER && aisle.keywords.any { lower.contains(it) }
            } ?: OTHER
        }
    }
}

internal fun groupTodosByAisle(todos: List<TodoItem>): List<Pair<GroceryAisle, List<TodoItem>>> {
    val grouped = todos.groupBy { GroceryAisle.infer(it.text, it.category) }
    return GroceryAisle.entries.mapNotNull { aisle ->
        grouped[aisle]?.takeIf { it.isNotEmpty() }?.let { aisle to it }
    }
}
