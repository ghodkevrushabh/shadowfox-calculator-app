package com.example.shadowfoxcalculator

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.shadowfoxcalculator.databinding.ActivityMainBinding
import net.objecthunter.exp4j.ExpressionBuilder
import java.text.DecimalFormat

class MainActivity : AppCompatActivity() {

    // 1. Set up View Binding
    private lateinit var binding: ActivityMainBinding

    // 2. For saving the theme preference
    private val PREFS_NAME = "ThemePrefs"
    private val THEME_KEY = "isDarkMode"

    // 3. For formatting numbers cleanly
    private val numberFormatter = DecimalFormat("#.##########")

    // 4. Reference to our expression EditText
    private lateinit var expressionText: EditText

    // 5. NEW: Memory variable
    private var memoryValue: Double = 0.0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Apply the saved theme BEFORE setting the content
        applySavedTheme()

        // Inflate the layout
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Get reference to the EditText
        expressionText = binding.expressionText
        expressionText.showSoftInputOnFocus = false // This hides the keyboard
        expressionText.setText("0")
        expressionText.setSelection(1) // Move cursor to the end
        binding.resultText.text = ""

        // Set up the Theme Toggle Button
        setupThemeToggle()

        // Set up all the OnClickListeners
        setupButtonListeners()

        // Add a TextWatcher to update the result in real-time
        expressionText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updateRealTimeResult()
            }
        })
    }

    private fun setupButtonListeners() {
        // --- Number Listeners ---
        val numberButtons = listOf(
            binding.button0, binding.button1, binding.button2, binding.button3,
            binding.button4, binding.button5, binding.button6, binding.button7,
            binding.button8, binding.button9, binding.buttonDot
        )
        numberButtons.forEach { it.setOnClickListener { onNumberClicked(it as Button) } }

        // --- Operator Listeners ---
        val operatorButtons = listOf(
            binding.buttonAdd, binding.buttonSubtract, binding.buttonMultiply,
            binding.buttonDivide, binding.buttonPercent
        )
        operatorButtons.forEach { it.setOnClickListener { onOperatorClicked(it as Button) } }

        // --- NEW: Scientific Listeners ---
        binding.buttonLog.setOnClickListener { onScientificFunctionClicked("log10(") }
        binding.buttonSqrt.setOnClickListener { onScientificFunctionClicked("sqrt(") }
        binding.buttonOpenParen.setOnClickListener { onScientificFunctionClicked("(") }
        binding.buttonCloseParen.setOnClickListener { onScientificFunctionClicked(")") }

        // --- NEW: Memory Listeners ---
        binding.buttonMPlus.setOnClickListener { onMemoryClicked("M+") }
        binding.buttonMMinus.setOnClickListener { onMemoryClicked("M-") }
        binding.buttonMr.setOnClickListener { onMemoryClicked("MR") }
        binding.buttonMc.setOnClickListener { onMemoryClicked("MC") }

        // --- Other Listeners ---
        binding.buttonBackspace.setOnClickListener { onBackspaceClicked() }
        binding.buttonClear.setOnClickListener { onClearClicked() }
        binding.buttonEquals.setOnClickListener { onEqualsClicked() }
    }

    // --- Theme Logic (Unchanged) ---

    private fun applySavedTheme() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val isDarkMode = sharedPref.getBoolean(THEME_KEY, false) // Default to light

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }

    private fun setupThemeToggle() {
        val sharedPref = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        var isDarkMode = sharedPref.getBoolean(THEME_KEY, false)

        updateThemeIcon(isDarkMode)

        binding.themeToggleButton.setOnClickListener {
            isDarkMode = !isDarkMode // Flip the value

            with(sharedPref.edit()) {
                putBoolean(THEME_KEY, isDarkMode)
                apply()
            }

            if (isDarkMode) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }
    }

    private fun updateThemeIcon(isDarkMode: Boolean) {
        if (isDarkMode) {
            binding.themeToggleButton.setIconResource(R.drawable.ic_light_mode)
        } else {
            binding.themeToggleButton.setIconResource(R.drawable.ic_dark_mode)
        }
    }

    // --- Calculator Logic Functions ---

    private fun insertAtCursor(text: String) {
        val start = expressionText.selectionStart
        val end = expressionText.selectionEnd

        if (expressionText.text.toString() == "0" && text != ".") {
            expressionText.setText(text)
            expressionText.setSelection(text.length)
        } else {
            expressionText.text.replace(start, end, text)
        }
    }

    private fun onNumberClicked(button: Button) {
        insertAtCursor(button.text.toString())
    }

    private fun onOperatorClicked(button: Button) {
        insertAtCursor(button.text.toString())
    }

    // --- NEW: Scientific & Memory Functions ---

    private fun onScientificFunctionClicked(func: String) {
        insertAtCursor(func)
    }

    private fun onMemoryClicked(type: String) {
        // First, get the current valid result on screen
        val currentResultString = evaluateExpression(expressionText.text.toString(), false)
        val currentResult = currentResultString.toDoubleOrNull() ?: 0.0

        when(type) {
            "M+" -> {
                memoryValue += currentResult
                Toast.makeText(this, "Added to Memory", Toast.LENGTH_SHORT).show()
            }
            "M-" -> {
                memoryValue -= currentResult
                Toast.makeText(this, "Subtracted from Memory", Toast.LENGTH_SHORT).show()
            }
            "MR" -> {
                insertAtCursor(numberFormatter.format(memoryValue))
            }
            "MC" -> {
                memoryValue = 0.0
                Toast.makeText(this, "Memory Cleared", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- Other Functions (Unchanged) ---

    private fun onClearClicked() {
        expressionText.setText("0")
        binding.resultText.text = ""
        expressionText.setSelection(1)
    }

    private fun onBackspaceClicked() {
        val start = expressionText.selectionStart
        val end = expressionText.selectionEnd

        if (start == 0 && end == 0) return

        if (start != end) {
            expressionText.text.delete(start, end)
        } else if (start > 0) {
            if (expressionText.text.toString() == "0" && start == 1) return

            expressionText.text.delete(start - 1, start)

            if(expressionText.text.isEmpty()) {
                expressionText.setText("0")
                expressionText.setSelection(1)
            }
        }
    }

    private fun onEqualsClicked() {
        val expressionString = expressionText.text.toString()
        val finalResult = evaluateExpression(expressionString, isFinalCalculation = true)

        if (finalResult.isNotEmpty()) {
            binding.expressionText.setText(finalResult)
            binding.resultText.text = expressionString
            binding.expressionText.setSelection(finalResult.length)
        }
    }

    private fun updateRealTimeResult() {
        val expressionString = expressionText.text.toString()
        if (expressionString.isEmpty() || expressionString == "0") {
            binding.resultText.text = ""
        } else {
            binding.resultText.text = evaluateExpression(expressionString, isFinalCalculation = false)
        }
    }

    private fun evaluateExpression(expressionString: String, isFinalCalculation: Boolean): String {
        if (expressionString.isEmpty()) return ""

        try {
            // 1. Sanitize the expression
            var evalString = expressionString
                .replace("×", "*")
                .replace("÷", "/")
                .replace("√", "sqrt")

            // 2. Handle the percentage logic
            val regex = Regex("([\\d.]+)\\s*([+\\-])\\s*([\\d.]+)%")
            evalString = regex.replace(evalString) {
                val num1 = it.groupValues[1].toDouble()
                val operator = it.groupValues[2]
                val percentVal = it.groupValues[3].toDouble()
                "($num1 $operator ($num1 * $percentVal * 0.01))"
            }
            evalString = evalString.replace("%", "*0.01")

            // 3. Build the expression
            val expression = ExpressionBuilder(evalString).build()

            // 4. Calculate
            val result = expression.evaluate()

            // 5. Check for math errors (like 8/0)
            if (result.isNaN() || result.isInfinite()) {
                return if (isFinalCalculation) "Error" else ""
            }

            // 6. Format and return
            return numberFormatter.format(result)

        } catch (e: Exception) {
            // Catches syntax errors (like "5+" or "5+*6")
            return if (isFinalCalculation) {
                "Error"
            } else {
                ""
            }
        }
    }
}