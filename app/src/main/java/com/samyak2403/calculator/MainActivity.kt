package com.samyak2403.calculator

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import com.google.android.gms.ads.AdView
import com.samyak2403.bannerad.Control
import com.samyak2403.calculator.databinding.ActivityMainBinding
import net.objecthunter.exp4j.Expression
import net.objecthunter.exp4j.ExpressionBuilder

class MainActivity : AppCompatActivity() {

    // View binding for accessing UI elements
    private lateinit var binding: ActivityMainBinding

    // Variables to track the state of the calculator
    private var lastNumeric = false
    private var stateError = false
    private var lastDot = false

    // Expression object for evaluating mathematical expressions
    private lateinit var expression: Expression

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Inflate the layout using view binding
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize Control class and load the banner ad
        val control = Control(this)
        val adUnitId = getString(R.string.admob_banner_id) // Fetch the AdMob banner ID from resources
        control.loadBannerAd(R.id.bannerAdContainer, adUnitId)
    }

    // Function to handle 'All Clear' button click
    fun onAllClearClick(view: View) {
        // Clear all text fields and reset the state
        binding.dataTv.text = ""
        binding.resultTv.text = ""
        stateError = false
        lastDot = false
        lastNumeric = false
        binding.resultTv.visibility = View.GONE
    }

    // Function to handle 'Equal' button click
    fun onEqualClick(view: View) {
        // Calculate and display the result
        onEqual()
        // Update the data text view with the result (dropping the '=' sign)
        binding.dataTv.text = binding.resultTv.text.toString().drop(1)
    }

    // Function to handle digit button clicks
    fun onDigitClick(view: View) {
        // If an error occurred, reset the text view
        if (stateError) {
            binding.dataTv.text = (view as Button).text
            stateError = false
        } else {
            // Append the clicked digit to the text view
            binding.dataTv.append((view as Button).text)
        }

        lastNumeric = true // A digit has been entered
        onEqual() // Calculate the intermediate result
    }

    // Function to handle operator button clicks
    fun onOperatorClick(view: View) {
        // Only append an operator if the last input was numeric and no error exists
        if (!stateError && lastNumeric) {
            binding.dataTv.append((view as Button).text)
            lastDot = false
            lastNumeric = false
            onEqual() // Calculate the intermediate result
        }
    }

    // Function to handle the 'Backspace' button click
    fun onBackClick(view: View) {
        val text = binding.dataTv.text.toString()
        // Remove the last character if the text is not empty
        if (text.isNotEmpty()) {
            binding.dataTv.text = text.dropLast(1)

            try {
                // Check the last character after deletion
                val lastChar = binding.dataTv.text.toString().lastOrNull()
                if (lastChar != null && lastChar.isDigit()) {
                    onEqual() // Calculate the intermediate result
                }
            } catch (e: Exception) {
                // Handle any exception if no character is left
                binding.resultTv.text = ""
                binding.resultTv.visibility = View.GONE
                Log.e("Last char error", e.toString())
            }
        }
    }

    // Function to handle the 'Clear' button click
    fun onClearClick(view: View) {
        // Clear the data text view
        binding.dataTv.text = ""
        lastNumeric = false
    }

    // Function to evaluate the expression and display the result
    private fun onEqual() {
        if (lastNumeric && !stateError) {
            var txt = binding.dataTv.text.toString()

            try {
                // Replace 'x' with '*' for multiplication
                txt = txt.replace("x", "*")

                // Build and evaluate the expression
                expression = ExpressionBuilder(txt).build()
                val result = expression.evaluate()

                // Display the result
                binding.resultTv.visibility = View.VISIBLE
                binding.resultTv.text = "=" + result.toString()
            } catch (ex: ArithmeticException) {
                // Handle any arithmetic errors during evaluation
                Log.e("Evaluate error", ex.toString())
                binding.resultTv.text = "Error"
                stateError = true
                lastNumeric = false
            } catch (ex: Exception) {
                // Handle other exceptions
                Log.e("Unknown error", ex.toString())
                binding.resultTv.text = "Error"
                stateError = true
                lastNumeric = false
            }
        }
    }
}
