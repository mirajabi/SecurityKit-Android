package com.miaadrajabi.securitysample

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.view.Gravity
import android.view.ViewGroup

class TestResultsActivity : Activity() {
    
    private lateinit var resultsTextView: TextView
    private lateinit var scrollView: ScrollView
    private lateinit var clearButton: Button
    private lateinit var backButton: Button
    
    companion object {
        private var testResults: String = "No test results yet. Run tests from main screen."
        
        fun updateResults(results: String) {
            testResults = results
        }
        
        fun getResults(): String = testResults
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setupUI()
        displayResults()
    }
    
    private fun setupUI() {
        val mainLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(32, 32, 32, 32)
            setBackgroundColor(Color.WHITE)
        }
        
        // Title
        val titleTextView = TextView(this).apply {
            text = "🔍 Security Test Results"
            textSize = 24f
            setTextColor(Color.BLACK)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 32)
        }
        mainLayout.addView(titleTextView)
        
        // Results Section
        val resultsTitle = TextView(this).apply {
            text = "Test Results:"
            textSize = 18f
            setTextColor(Color.BLACK)
            setPadding(0, 0, 0, 16)
        }
        mainLayout.addView(resultsTitle)
        
        // Scrollable Results
        scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
            setBackgroundColor(Color.parseColor("#F5F5F5"))
        }
        
        resultsTextView = TextView(this).apply {
            text = testResults
            textSize = 14f
            setTextColor(Color.BLACK)
            setPadding(16, 16, 16, 16)
            movementMethod = ScrollingMovementMethod()
            setLineSpacing(4f, 1.2f)
        }
        
        scrollView.addView(resultsTextView)
        mainLayout.addView(scrollView)
        
        // Buttons Section
        val buttonsLayout = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            setPadding(0, 32, 0, 0)
        }
        
        backButton = Button(this).apply {
            text = "🔙 Back to Tests"
            setPadding(32, 16, 32, 16)
            setOnClickListener { 
                finish()
            }
        }
        buttonsLayout.addView(backButton)
        
        clearButton = Button(this).apply {
            text = "🗑️ Clear Results"
            setPadding(32, 16, 32, 16)
            setOnClickListener { 
                clearResults()
            }
        }
        buttonsLayout.addView(clearButton)
        
        mainLayout.addView(buttonsLayout)
        
        setContentView(mainLayout)
    }
    
    private fun displayResults() {
        resultsTextView.text = TestResultsActivity.getResults()
        
        // Auto-scroll to bottom
        scrollView.post {
            scrollView.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }
    
    private fun clearResults() {
        testResults = "Results cleared. Run tests from main screen to see new results."
        resultsTextView.text = testResults
        TestResultsActivity.updateResults(testResults)
    }
    
    override fun onResume() {
        super.onResume()
        // Update results when returning to this activity
        displayResults()
    }
}
