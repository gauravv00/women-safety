package com.womensafety.sos.data.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.ContactsContract
import android.util.Log
import androidx.core.content.ContextCompat

sealed class ContactImportResult {
    data class Success(val name: String, val phoneNumber: String) : ContactImportResult()
    data class Duplicate(val name: String, val phoneNumber: String) : ContactImportResult()
    object NoPhoneNumber : ContactImportResult()
    data class InvalidPhoneNumber(val rawNumber: String) : ContactImportResult()
    object PermissionRequired : ContactImportResult()
    data class Failure(val message: String) : ContactImportResult()
}

object ContactImportHelper {

    private const val TAG = "ContactImportHelper"

    /**
     * Creates an Intent to pick directly from Phone numbers.
     * This prompts the user to select a phone number (not just a contact profile),
     * and grants temporary read permission to the specific phone record on standard Android.
     */
    fun createPhonePickerIntent(): Intent {
        return Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
    }

    /**
     * Checks whether READ_CONTACTS permission is granted.
     */
    fun hasPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Safely resolves a picked Contact or Phone URI into a clean name and phone number.
     * Handles:
     * 1. Direct Phone URI (vnd.android.cursor.item/phone_v2)
     * 2. General Contact URI (vnd.android.cursor.item/contact or lookup URI)
     * 3. OEMs with non-standard content providers
     * 4. Missing permissions / SecurityExceptions
     * 5. Empty or invalid phone numbers
     * 6. Duplicate contact checks against existing list
     */
    fun resolveContact(
        context: Context,
        uri: Uri,
        existingPhoneNumbers: List<String> = emptyList()
    ): ContactImportResult {
        return try {
            var resolvedName: String? = null
            var resolvedPhone: String? = null

            // Step 1: Attempt to query the URI directly for Phone data
            // This works when uri is CommonDataKinds.Phone.CONTENT_URI or a specific data row
            val phoneProjection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )

            try {
                context.contentResolver.query(uri, phoneProjection, null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                        val phoneIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                        if (nameIdx != -1) resolvedName = cursor.getString(nameIdx)
                        if (phoneIdx != -1) resolvedPhone = cursor.getString(phoneIdx)
                    }
                }
            } catch (se: SecurityException) {
                Log.w(TAG, "Direct Phone query SecurityException: ${se.localizedMessage}")
            } catch (e: Exception) {
                Log.w(TAG, "Direct Phone query failed: ${e.localizedMessage}")
            }

            // Step 2: If phone not resolved, it may be a Contacts table URI (e.g. ActivityResultContracts.PickContact)
            if (resolvedPhone.isNullOrBlank()) {
                val contactProjection = arrayOf(
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER
                )

                var contactId: String? = null
                var hasPhoneFlag = 0

                try {
                    context.contentResolver.query(uri, contactProjection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val idIdx = cursor.getColumnIndex(ContactsContract.Contacts._ID)
                            val nameIdx = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME)
                            val hasPhoneIdx = cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER)

                            if (idIdx != -1) contactId = cursor.getString(idIdx)
                            if (nameIdx != -1 && resolvedName.isNullOrBlank()) resolvedName = cursor.getString(nameIdx)
                            if (hasPhoneIdx != -1) hasPhoneFlag = cursor.getInt(hasPhoneIdx)
                        }
                    }
                } catch (se: SecurityException) {
                    Log.w(TAG, "Contacts query SecurityException: ${se.localizedMessage}")
                    return ContactImportResult.PermissionRequired
                } catch (e: Exception) {
                    Log.w(TAG, "Contacts query failed: ${e.localizedMessage}")
                }

                if (hasPhoneFlag > 0 && !contactId.isNullOrBlank()) {
                    if (!hasPermission(context)) {
                        return ContactImportResult.PermissionRequired
                    }

                    try {
                        context.contentResolver.query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME),
                            "${ContactsContract.CommonDataKinds.Phone.CONTACT_ID} = ?",
                            arrayOf(contactId),
                            null
                        )?.use { pCursor ->
                            if (pCursor.moveToFirst()) {
                                val pIndex = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                                val nIndex = pCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                                if (pIndex != -1) resolvedPhone = pCursor.getString(pIndex)
                                if (nIndex != -1 && resolvedName.isNullOrBlank()) resolvedName = pCursor.getString(nIndex)
                            }
                        }
                    } catch (se: SecurityException) {
                        Log.e(TAG, "Phone table query SecurityException: ${se.localizedMessage}")
                        return ContactImportResult.PermissionRequired
                    } catch (e: Exception) {
                        Log.e(TAG, "Phone table query failed: ${e.localizedMessage}")
                    }
                }
            }

            // Step 3: Validate resolved data
            if (resolvedPhone.isNullOrBlank()) {
                return ContactImportResult.NoPhoneNumber
            }

            val sanitizedNumber = PhoneUtils.sanitizePhoneNumber(resolvedPhone!!)
            if (!PhoneUtils.isValidPhoneNumber(sanitizedNumber)) {
                return ContactImportResult.InvalidPhoneNumber(resolvedPhone!!)
            }

            val finalName = resolvedName?.trim()?.ifBlank { "Contact" } ?: "Contact"

            // Step 4: Check duplicate
            val isDuplicate = existingPhoneNumbers.any { existing ->
                PhoneUtils.sanitizePhoneNumber(existing) == sanitizedNumber
            }
            if (isDuplicate) {
                return ContactImportResult.Duplicate(finalName, sanitizedNumber)
            }

            ContactImportResult.Success(finalName, sanitizedNumber)
        } catch (se: SecurityException) {
            Log.e(TAG, "SecurityException while importing contact", se)
            ContactImportResult.PermissionRequired
        } catch (t: Throwable) {
            Log.e(TAG, "Unexpected error resolving contact", t)
            ContactImportResult.Failure(t.localizedMessage ?: "Unknown error occurred")
        }
    }
}
