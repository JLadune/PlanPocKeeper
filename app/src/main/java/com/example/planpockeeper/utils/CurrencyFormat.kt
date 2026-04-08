package com.example.planpockeeper.utils

object CurrencyFormatter {

    data class CurrencyInfo(
        val code: String,
        val symbol: String,
        val label: String
    )

    val currencies = listOf(
        CurrencyInfo("EUR", "€", "Euro"),
        CurrencyInfo("USD", "$", "Dollar américain"),
        CurrencyInfo("GBP", "£", "Livre sterling"),
        CurrencyInfo("CAD", "$", "Dollar canadien"),
        CurrencyInfo("JPY", "¥", "Yen japonais"),
        CurrencyInfo("MAD", "dh", "Dirham marocain")
    )

    fun getSymbol(code: String): String =
        currencies.firstOrNull { it.code == code }?.symbol ?: "€"

    fun format(amount: Double, currencyCode: String): String {
        val symbol = getSymbol(currencyCode)
        return when (currencyCode) {
            "JPY" -> "${amount.toInt()}$symbol"
            else -> "${"%.2f".format(amount)}$symbol"
        }
    }
}