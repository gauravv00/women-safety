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
     * Validates if the phone number has a reasonable length (7 to 15 digits according to E.164).
     */
    fun isValidPhoneNumber(phone: String): Boolean {
        val sanitized = sanitizePhoneNumber(phone)
        val digits = sanitized.filter { it.isDigit() }
        return digits.length in 7..15
    }

    /**
     * Formats phone number for display (e.g. +91 98765 43210 or 98765 43210).
     */
    fun formatForDisplay(raw: String): String {
        val sanitized = sanitizePhoneNumber(raw)
        return if (sanitized.startsWith("+") && sanitized.length > 3) {
            val countryCode = sanitized.substring(0, 3)
            val rest = sanitized.substring(3)
            if (rest.length > 5) {
                "$countryCode ${rest.substring(0, 5)} ${rest.substring(5)}"
            } else {
                "$countryCode $rest"
            }
        } else if (sanitized.length == 10) {
            "${sanitized.substring(0, 5)} ${sanitized.substring(5)}"
        } else {
            sanitized
        }
    }

    /**
     * Normalizes pairing codes. Accepts either "SOS-XXXXXX", "XXXXXX", with or without hyphens or lowercase.
     * Returns standard "SOS-XXXXXX" format.
     */
    fun normalizePairingCode(raw: String): String {
        val cleaned = raw.trim().uppercase().replace(" ", "").replace("-", "")
        val codePart = if (cleaned.startsWith("SOS")) {
            cleaned.removePrefix("SOS")
        } else {
            cleaned
        }
        return if (codePart.isNotBlank()) "SOS-$codePart" else ""
    }

    /**
     * Validates if a pairing code adheres to the standard SOS-XXXXXX format (6 alphanumeric characters).
     */
    fun isValidPairingCode(code: String): Boolean {
        val normalized = normalizePairingCode(code)
        val regex = Regex("^SOS-[A-Z0-9]{6}$")
        return regex.matches(normalized)
    }
}
