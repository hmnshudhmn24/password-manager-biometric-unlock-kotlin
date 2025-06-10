package com.example.passwordmanager

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import java.util.concurrent.Executor

class MainActivity : AppCompatActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo
    private lateinit var encryptedPrefs: EncryptedSharedPreferences

    private lateinit var vaultLayout: LinearLayout
    private lateinit var loginLayout: LinearLayout
    private lateinit var saveButton: Button
    private lateinit var websiteInput: EditText
    private lateinit var passwordInput: EditText
    private lateinit var vaultDisplay: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        loginLayout = findViewById(R.id.loginLayout)
        vaultLayout = findViewById(R.id.vaultLayout)
        saveButton = findViewById(R.id.saveButton)
        websiteInput = findViewById(R.id.websiteInput)
        passwordInput = findViewById(R.id.passwordInput)
        vaultDisplay = findViewById(R.id.vaultDisplay)

        executor = ContextCompat.getMainExecutor(this)
        encryptedPrefs = EncryptedSharedPreferences.create(
            "vault",
            MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC),
            this,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    runOnUiThread {
                        loginLayout.visibility = View.GONE
                        vaultLayout.visibility = View.VISIBLE
                        loadVault()
                    }
                }

                override fun onAuthenticationFailed() {
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Secure Vault Login")
            .setSubtitle("Authenticate with your biometrics")
            .setNegativeButtonText("Cancel")
            .build()

        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate() == BiometricManager.BIOMETRIC_SUCCESS) {
            biometricPrompt.authenticate(promptInfo)
        } else {
            Toast.makeText(this, "Biometric authentication not available", Toast.LENGTH_LONG).show()
        }

        saveButton.setOnClickListener {
            val website = websiteInput.text.toString()
            val password = passwordInput.text.toString()
            if (website.isNotBlank() && password.isNotBlank()) {
                encryptedPrefs.edit().putString(website, password).apply()
                Toast.makeText(this, "Password Saved!", Toast.LENGTH_SHORT).show()
                websiteInput.text.clear()
                passwordInput.text.clear()
                loadVault()
            }
        }
    }

    private fun loadVault() {
        val allEntries = encryptedPrefs.all
        val builder = StringBuilder()
        for ((key, value) in allEntries) {
            builder.append("🔐 $key: $value\n")
        }
        vaultDisplay.text = builder.toString()
    }
}
