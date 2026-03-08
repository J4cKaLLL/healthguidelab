package com.healthguidelab.keto365.data

import java.time.LocalDate

private val recipePool = listOf(
    "Scrambled eggs with avocado and bacon",
    "Baked salmon with garlic butter and asparagus",
    "Creamy chicken with mushrooms and spinach",
    "Keto tuna salad with mayo and cucumber",
    "Keto tacos with lettuce wraps and ground beef",
    "Sauteed cauliflower bowl with shrimp",
    "Meatballs in sugar-free tomato sauce with cheese",
    "Bell peppers stuffed with cream cheese and chicken",
    "Keto lasagna with zucchini slices",
    "Cheddar omelette with serrano ham",
    "Pork chops with cauliflower mash",
    "Keto Caesar salad with chicken and parmesan"
)

data class DailyRecipe(
    val dayOfYear: Int,
    val date: LocalDate,
    val title: String
)

fun recipeFor(date: LocalDate): DailyRecipe {
    val day = date.dayOfYear
    val title = recipePool[(day - 1) % recipePool.size]
    return DailyRecipe(
        dayOfYear = day,
        date = date,
        title = title
    )
}
