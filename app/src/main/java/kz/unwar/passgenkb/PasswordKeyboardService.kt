package kz.unwar.passgenkb

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.inputmethodservice.InputMethodService
import android.os.Build
import android.os.PersistableBundle
import android.view.View
import android.view.inputmethod.ExtractedTextRequest
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.CheckBox
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import java.security.SecureRandom

class PasswordKeyboardService : InputMethodService() {

    private val secureRandom = SecureRandom()
    private val charLower = "abcdefghijklmnopqrstuvwxyz"
    private val charUpper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
    private val charDigits = "0123456789"
    private val charSymbols = "!@#$%^&*()-_=+[]{}|;:,.<>?"
    private val minLength = 4

    override fun onCreateInputView(): View {
        val view = layoutInflater.inflate(R.layout.keyboard_view, null)
        val storageContext = createDeviceProtectedStorageContext()
        val prefs = storageContext.getSharedPreferences("passgen_prefs", Context.MODE_PRIVATE)

        val tvLengthLabel = view.findViewById<TextView>(R.id.tvLengthLabel)
        val sbLength = view.findViewById<SeekBar>(R.id.sbLength)
        val cbLower = view.findViewById<CheckBox>(R.id.cbLower)
        val cbUpper = view.findViewById<CheckBox>(R.id.cbUpper)
        val cbDigits = view.findViewById<CheckBox>(R.id.cbDigits)
        val cbSymbols = view.findViewById<CheckBox>(R.id.cbSymbols)
        val btnGenerate = view.findViewById<Button>(R.id.btnGenerate)
        val btnGenerateAndCopy = view.findViewById<Button>(R.id.btnGenerateAndCopy)
        val btnBackspace = view.findViewById<Button>(R.id.btnBackspace)
        val btnSwitchIme = view.findViewById<Button>(R.id.btnSwitchIme)

        val savedLength = prefs.getInt("length", 16).coerceAtLeast(minLength)
        sbLength.progress = savedLength
        tvLengthLabel.text = getString(R.string.kb_length, savedLength)

        cbLower.isChecked = prefs.getBoolean("lower", true)
        cbUpper.isChecked = prefs.getBoolean("upper", true)
        cbDigits.isChecked = prefs.getBoolean("digits", true)
        cbSymbols.isChecked = prefs.getBoolean("symbols", true)

        sbLength.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val actualLength = progress.coerceAtLeast(minLength)
                tvLengthLabel.text = getString(R.string.kb_length, actualLength)
                prefs.edit().putInt("length", actualLength).apply()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        val checkListener = View.OnClickListener {
            prefs.edit()
                .putBoolean("lower", cbLower.isChecked)
                .putBoolean("upper", cbUpper.isChecked)
                .putBoolean("digits", cbDigits.isChecked)
                .putBoolean("symbols", cbSymbols.isChecked)
                .apply()
        }
        cbLower.setOnClickListener(checkListener)
        cbUpper.setOnClickListener(checkListener)
        cbDigits.setOnClickListener(checkListener)
        cbSymbols.setOnClickListener(checkListener)

        btnGenerate.setOnClickListener {
            val password = generatePassword(
                length = sbLength.progress.coerceAtLeast(minLength),
                useLower = cbLower.isChecked,
                useUpper = cbUpper.isChecked,
                useDigits = cbDigits.isChecked,
                useSymbols = cbSymbols.isChecked
            )
            if (password != null) {
                currentInputConnection?.commitText(password, 1)
            } else {
                Toast.makeText(this, getString(R.string.toast_no_selection), Toast.LENGTH_SHORT).show()
            }
        }

        btnGenerateAndCopy.setOnClickListener {
            val password = generatePassword(
                length = sbLength.progress.coerceAtLeast(minLength),
                useLower = cbLower.isChecked,
                useUpper = cbUpper.isChecked,
                useDigits = cbDigits.isChecked,
                useSymbols = cbSymbols.isChecked
            )
            if (password != null) {
                currentInputConnection?.commitText(password, 1)
                copyToClipboardSecurely(password)
                Toast.makeText(this, getString(R.string.toast_copied), Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, getString(R.string.toast_no_selection), Toast.LENGTH_SHORT).show()
            }
        }

        btnBackspace.setOnClickListener {
            currentInputConnection?.deleteSurroundingText(1, 0)
        }
        btnBackspace.setOnLongClickListener {
            val ic = currentInputConnection ?: return@setOnLongClickListener true
            val extracted = ic.getExtractedText(ExtractedTextRequest(), 0)
            if (extracted?.text != null) {
                ic.setSelection(0, extracted.text.length)
                ic.commitText("", 1)
            } else {
                ic.deleteSurroundingText(100, 100)
            }
            true
        }

        btnSwitchIme.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val switched = switchToPreviousInputMethod()
                if (!switched) showImePicker()
            } else {
                showImePicker()
            }
        }
        btnSwitchIme.setOnLongClickListener {
            showImePicker()
            true
        }

        return view
    }

    private fun showImePicker() {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.showInputMethodPicker()
    }

    private fun generatePassword(
        length: Int,
        useLower: Boolean,
        useUpper: Boolean,
        useDigits: Boolean,
        useSymbols: Boolean
    ): String? {
        val activePools = mutableListOf<String>()
        if (useLower) activePools.add(charLower)
        if (useUpper) activePools.add(charUpper)
        if (useDigits) activePools.add(charDigits)
        if (useSymbols) activePools.add(charSymbols)

        if (activePools.isEmpty()) return null

        val combinedPool = activePools.joinToString("")
        val passwordChars = CharArray(length)

        try {
            for (i in activePools.indices) {
                if (i < length) {
                    val pool = activePools[i]
                    passwordChars[i] = pool[secureRandom.nextInt(pool.length)]
                }
            }

            for (i in activePools.size until length) {
                passwordChars[i] = combinedPool[secureRandom.nextInt(combinedPool.length)]
            }

            for (i in passwordChars.indices.reversed()) {
                val j = secureRandom.nextInt(i + 1)
                val temp = passwordChars[i]
                passwordChars[i] = passwordChars[j]
                passwordChars[j] = temp
            }

            return String(passwordChars)
        } finally {
            // Затираем буфер символов в ОЗУ сразу после генерации
            passwordChars.fill('\u0000')
        }
    }

    private fun copyToClipboardSecurely(text: String) {
        val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText("Generated Password", text)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }
        clipboard.setPrimaryClip(clip)
    }
}