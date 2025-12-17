package com.example.kiddoabc.ui.home

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.kiddoabc.databinding.ActivityHomeBinding
import com.example.kiddoabc.data.models.AlphabetType
import com.example.kiddoabc.ui.letters.LettersActivity

class HomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityHomeBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Bouton alphabet arabe
        binding.cardArabic.setOnClickListener {
            navigateToLetters(AlphabetType.ARABIC)
        }

        // Bouton alphabet français
        binding.cardFrench.setOnClickListener {
            navigateToLetters(AlphabetType.FRENCH)
        }
    }

    private fun navigateToLetters(alphabetType: AlphabetType) {
        val intent = Intent(this, LettersActivity::class.java).apply {
            putExtra(EXTRA_ALPHABET_TYPE, alphabetType.name)
        }
        startActivity(intent)
    }

    companion object {
        const val EXTRA_ALPHABET_TYPE = "extra_alphabet_type"
    }
}