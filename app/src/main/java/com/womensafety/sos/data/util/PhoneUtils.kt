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
     * Validates if the phone number is a valid 10-digit mobile number (or 12-digit with 91 country code).
     */
    fun isValidPhoneNumber(phone: String): Boolean {
        val digits = phone.filter { it.isDigit() }
        return digits.length == 10 || (digits.startsWith("91") && digits.length == 12)
    }

    /**
     * Extracts exactly the 10-digit national number from an Indian phone input,
     * stripping leading "+91", "91", or "0" if present.
     * Prevents duplicate "+91" prefixing in UI text fields.
     */
    fun extract10DigitNumber(raw: String): String {
        var trimmed = raw.trim()
        if (trimmed.startsWith("+91")) {
            trimmed = trimmed.removePrefix("+91")
        } else if (trimmed.startsWith("+")) {
            trimmed = trimmed.removePrefix("+")
        }
        val digits = trimmed.filter { it.isDigit() }
        val nationalDigits = if (digits.length > 10 && digits.startsWith("91")) {
            digits.removePrefix("91")
        } else if (digits.length == 11 && digits.startsWith("0")) {
            digits.removePrefix("0")
        } else {
            digits
        }
        return nationalDigits.take(10)
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
