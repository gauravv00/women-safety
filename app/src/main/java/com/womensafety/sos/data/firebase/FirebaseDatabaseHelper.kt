package com.womensafety.sos.data.firebase

import android.util.Log
import com.google.firebase.database.FirebaseDatabase

object FirebaseDatabaseHelper {
    // Exact regional database URL from google-services.json (asia-southeast1)
    const val RTDB_URL = "https://wosafety-17b7e-default-rtdb.asia-southeast1.firebasedatabase.app"

    fun getInstance(): FirebaseDatabase {
        return try {
            FirebaseDatabase.getInstance(RTDB_URL)
        } catch (e: Exception) {
            Log.w("FirebaseDatabaseHelper", "Failed to connect to regional URL $RTDB_URL, falling back: ${e.localizedMessage}")
            FirebaseDatabase.getInstance()
        }
    }
}

