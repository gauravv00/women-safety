package com.womensafety.sos.data.util

object PhoneUtils {
    /**
     * Sanitizes phone numbers by stripping whitespace, hyphens, brackets,
     * and preserving optional leading '+' for country codes.
     */
    fun sanitizePhoneNumber(raw: String): String {
        val trimmed = raw.trim()
        val hasLeadingPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.filter { it.isDigit() }
        return if (hasLeadingPlus) "+$digitsOnly" else digitsOnly
    }

    /**
     * Validates if the phone number has a reasonable length (typically 7 to 15 digits).
     */
    fun isValidPhoneNumber(phone: String): Boolean {
        val digits = phone.filter { it.isDigit() }
        return digits.length in 7..15
    }
}
