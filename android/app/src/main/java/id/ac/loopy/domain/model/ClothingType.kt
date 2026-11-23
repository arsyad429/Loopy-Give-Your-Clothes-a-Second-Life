package id.ac.loopy.domain.model

/** User-selected clothing type. Send backendValue, rather than the translated label, to the API. */
enum class ClothingType(val label: String, val backendValue: String) {
    TSHIRT("Kaos", "TSHIRT"),
    SHIRT("Kemeja", "SHIRT"),
    POLO("Polo Shirt", "POLO"),
    BLOUSE("Blus", "BLOUSE"),
    TANK_TOP("Tank Top", "TANK_TOP"),
    SWEATER("Sweater", "SWEATER"),
    HOODIE("Hoodie", "HOODIE"),
    JACKET("Jaket", "JACKET"),
    CARDIGAN("Cardigan", "CARDIGAN"),
    LONG_PANTS("Celana Panjang", "LONG_PANTS"),
    SHORTS("Celana Pendek", "SHORTS"),
    JEANS("Celana Jeans", "JEANS"),
    JOGGER("Celana Training / Jogger", "JOGGER"),
    SKIRT("Rok", "SKIRT"),
    DRESS("Dress / Gaun", "DRESS"),
    GAMIS("Gamis", "GAMIS"),
    TUNIC("Tunik", "TUNIC"),
    SPORTSWEAR("Pakaian Olahraga", "SPORTSWEAR"),
    SLEEPWEAR("Pakaian Tidur", "SLEEPWEAR"),
    OTHER("Lainnya", "OTHER"),
}
