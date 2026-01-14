package com.example.billlens.domain.expenses

enum class ExpenseCategory(val displayName: String, val keywords: List<String>) {
    GROCERIES(
        "Groceries", listOf(
            "supermercato", "market", "lidl", "coop", "conad", "carrefour", "esselunga",
            "grocery", "alimentari", "spesa", "food", "penny", "despar", "aldi"
        )
    ),
    DINING(
        "Dining & Drinks", listOf(
            "ristorante", "restaurant", "pizzeria", "trattoria", "bar", "caffè", "coffee",
            "pub", "osteria", "dinner", "lunch", "breakfast", "steakhouse", "bistrot"
        )
    ),
    TRANSPORT(
        "Transport", listOf(
            "benzina", "fuel", "carburante", "eni", "q8", "shell", "treno", "train",
            "bus", "autobus", "parcheggio", "parking", "taxi", "uber", "garage"
        )
    ),
    SHOPPING(
        "Shopping", listOf(
            "abbigliamento", "clothing", "clothes", "zara", "h&m", "amazon", "mediaworld",
            "unieuro", "elettronica", "electronics", "vestiti", "fashion", "mall", "decathlon"
        )
    ),
    HEALTH(
        "Health & Medical", listOf(
            "farmacia", "pharmacy", "medico", "doctor", "dentista", "dentist", "clinica",
            "clinic", "ospedale", "hospital", "salute", "drugstore"
        )
    ),
    HOME(
        "Home & Utilities", listOf(
            "affitto", "rent", "ikea", "leroy merlin", "ferramenta", "hardware", "electricity",
            "luce", "gas", "acqua", "water", "internet", "mobili", "furniture"
        )
    ),
    ENTERTAINMENT(
        "Entertainment", listOf(
            "cinema", "teatro", "theater", "palestra", "gym", "sport", "museo", "museum",
            "viaggi", "travel", "hotel", "vacation", "concert", "concerto"
        )
    ),
    PERSONAL_CARE(
        "Personal Care", listOf(
            "barbiere", "barber", "parrucchiere", "haircut", "salon", "estetica", "beauty",
            "wellness", "spa", "cosmetici", "makeup"
        )
    ),
    EDUCATION(
        "Education", listOf(
            "libri", "books", "università", "university", "scuola", "school", "corso",
            "course", "cancelleria", "stationery"
        )
    ),
    OTHER("Miscellaneous", emptyList());

    companion object {
        /**
         * Analyzes text and suggests the most probable category.
         */
        fun categorize(fullText: String): ExpenseCategory {
            val lowerText = fullText.lowercase()
            return entries.find { category ->
                category.keywords.any { keyword -> lowerText.contains(keyword) }
            } ?: OTHER
        }
    }
}