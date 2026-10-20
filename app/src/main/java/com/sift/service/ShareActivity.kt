package com.sift.service

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import com.sift.data.Db
import com.sift.data.Snippet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// The whole capture surface: any app's Share sheet -> Sift. Persist, toast
// the contract, done. Two taps, zero decisions.
class ShareActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val text = intent?.takeIf { it.action == Intent.ACTION_SEND }
            ?.getStringExtra(Intent.EXTRA_TEXT)?.trim()
        if (text.isNullOrEmpty()) {
            Toast.makeText(this, "Nothing text to keep.", Toast.LENGTH_SHORT).show()
            finish()
            return
        }
        val app = applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            val dao = Db.get(app).dao()
            val alreadyKept = dao.countByText(text) > 0
            if (!alreadyKept) {
                dao.insert(Snippet(text = text, createdAt = System.currentTimeMillis()))
            }
            runOnUiThread {
                Toast.makeText(
                    app,
                    if (alreadyKept) "Already kept." else "Kept. Filed tonight.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
        finish()
    }
}
