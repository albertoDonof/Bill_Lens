package com.example.billlens.utils

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Locale

/**
 * Un oggetto singleton per gestire la formattazione di valori BigDecimal in stringhe di valuta.
 *
 * Utilizza NumberFormat per garantire una formattazione corretta basata sulla Locale,
 * gestendo simboli di valuta, separatori decimali e delle migliaia.
 */
object CurrencyFormatter {

    // Creiamo un'istanza del formattatore una sola volta e la riutilizziamo.
    // Usare Locale.getDefault() adatta la formattazione alle impostazioni del telefono dell'utente.
    // Per forzare lo stile italiano, usa Locale.ITALY.
    private val formatter: NumberFormat = NumberFormat.getCurrencyInstance(Locale.getDefault())

    /**
     * Formatta un BigDecimal in una stringa di valuta.
     *
     * @param amount Il valore BigDecimal da formattare.
     * @return Una stringa formattata secondo le convenzioni della valuta locale (es. "1.234,56 €").
     */
    fun formatBigToString(amount: BigDecimal): String {
        return formatter.format(amount)
    }

    /**
     * Formatta un Double in una stringa di valuta.
     * Utile per valori che non sono ancora stati convertiti in BigDecimal.
     *
     * @param amount Il valore Double da formattare.
     * @return Una stringa formattata.
     */
    fun formatDoubleToString(amount: Double): String {
        return formatter.format(amount)
    }
}