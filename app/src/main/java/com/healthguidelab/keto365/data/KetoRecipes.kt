package com.healthguidelab.keto365.data

import java.time.LocalDate

private val recipePool = listOf(
    "Huevos revueltos con aguacate y tocino",
    "Salmón al horno con mantequilla de ajo y espárragos",
    "Pollo cremoso con champiñones y espinaca",
    "Ensalada keto de atún con mayonesa y pepino",
    "Tacos keto con hojas de lechuga y carne molida",
    "Bowl de coliflor salteada con camarones",
    "Albóndigas en salsa de tomate sin azúcar con queso",
    "Pimientos rellenos de queso crema y pollo",
    "Lasaña keto con láminas de calabacín",
    "Omelette de queso cheddar y jamón serrano",
    "Chuletas de cerdo con puré de coliflor",
    "Ensalada César keto con pollo y parmesano"
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
