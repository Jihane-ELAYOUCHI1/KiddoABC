package com.example.kiddoabc.ui.letters

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.example.kiddoabc.MyApplication
import com.example.kiddoabc.R
import com.example.kiddoabc.data.models.AlphabetType
import com.example.kiddoabc.data.models.Letter
import com.example.kiddoabc.data.repository.LetterRepository
import com.example.kiddoabc.databinding.ActivityLettersBinding
import com.example.kiddoabc.ui.drawing.DrawingActivity
import com.example.kiddoabc.utils.SoundManager
import kotlinx.coroutines.launch

class LettersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLettersBinding
    private lateinit var adapter: LettersAdapter
    private lateinit var repository: LetterRepository
    private lateinit var soundManager: SoundManager

    private var alphabetType: AlphabetType = AlphabetType.ARABIC

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d("LettersActivity", "onCreate started")

            binding = ActivityLettersBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Récupérer le type d'alphabet depuis l'intent
            val alphabetTypeName = intent.getStringExtra(EXTRA_ALPHABET_TYPE)
            Log.d("LettersActivity", "Alphabet type received: $alphabetTypeName")

            alphabetType = try {
                AlphabetType.valueOf(alphabetTypeName ?: AlphabetType.ARABIC.name)
            } catch (e: Exception) {
                Log.e("LettersActivity", "Invalid alphabet type, using default", e)
                AlphabetType.ARABIC
            }

            // Initialiser le repository et le sound manager
            val database = (application as MyApplication).database
            repository = LetterRepository(database.progressDao())
            soundManager = SoundManager(this)

            setupToolbar()
            setupRecyclerView()
            loadLetters()

            Log.d("LettersActivity", "onCreate completed successfully")

        } catch (e: Exception) {
            Log.e("LettersActivity", "Error in onCreate", e)
            e.printStackTrace()
            showError("Erreur lors de l'initialisation: ${e.message}")
            finish()
        }
    }

    private fun setupToolbar() {
        try {
            setSupportActionBar(binding.toolbar)
            supportActionBar?.apply {
                setDisplayHomeAsUpEnabled(true)
                title = when (alphabetType) {
                    AlphabetType.ARABIC -> "Alphabet Arabe"
                    AlphabetType.FRENCH -> "Alphabet Français"
                }
            }
            Log.d("LettersActivity", "Toolbar setup complete")
        } catch (e: Exception) {
            Log.e("LettersActivity", "Error setting up toolbar", e)
            e.printStackTrace()
        }
    }

    private fun setupRecyclerView() {
        try {
            adapter = LettersAdapter(
                onLetterClick = { letter ->
                    Log.d("LettersActivity", "Letter clicked: ${letter.character}")
                    soundManager.playSound(letter.soundFileName)
                },
                onLetterLongClick = { letter ->
                    Log.d("LettersActivity", "Letter long clicked: ${letter.character}")
                    navigateToDrawing(letter)
                }
            )

            binding.recyclerView.apply {
                layoutManager = GridLayoutManager(this@LettersActivity, 3)
                adapter = this@LettersActivity.adapter
                setHasFixedSize(true)
            }

            Log.d("LettersActivity", "RecyclerView setup complete")
        } catch (e: Exception) {
            Log.e("LettersActivity", "Error setting up RecyclerView", e)
            e.printStackTrace()
            showError("Erreur lors de la configuration de la liste")
        }
    }

    private fun loadLetters() {
        lifecycleScope.launch {
            try {
                Log.d("LettersActivity", "Loading letters for type: $alphabetType")

                showLoading(true)

                val letters = repository.getLettersByType(this@LettersActivity, alphabetType)
                Log.d("LettersActivity", "Loaded ${letters.size} letters")

                if (letters.isEmpty()) {
                    Log.w("LettersActivity", "No letters found")
                    showEmptyState()
                } else {
                    adapter.submitList(letters)
                    showContent()
                }

            } catch (e: Exception) {
                Log.e("LettersActivity", "Error loading letters", e)
                e.printStackTrace()
                showError("Erreur lors du chargement: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun navigateToDrawing(letter: Letter) {
        try {
            Log.d("LettersActivity", "Navigating to DrawingActivity for letter: ${letter.character}")

            val intent = Intent(this, DrawingActivity::class.java).apply {
                putExtra(EXTRA_LETTER_ID, letter.id)
                putExtra(EXTRA_ALPHABET_TYPE, alphabetType.name)
            }

            startActivity(intent)

            Log.d("LettersActivity", "Navigation to DrawingActivity successful")
        } catch (e: Exception) {
            Log.e("LettersActivity", "Error navigating to DrawingActivity", e)
            e.printStackTrace()
            showError("Erreur lors de l'ouverture: ${e.message}")
        }
    }

    private fun showLoading(show: Boolean) {
        binding.progressBar.visibility = if (show) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (show) View.GONE else View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE
    }

    private fun showContent() {
        binding.progressBar.visibility = View.GONE
        binding.recyclerView.visibility = View.VISIBLE
        binding.tvEmptyState.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.progressBar.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.tvEmptyState.text = "Aucune lettre disponible"
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        Log.e("LettersActivity", message)
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onPause() {
        super.onPause()
        try {
            soundManager.stopAll()
        } catch (e: Exception) {
            Log.e("LettersActivity", "Error stopping sounds", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            soundManager.release()
        } catch (e: Exception) {
            Log.e("LettersActivity", "Error releasing sound manager", e)
        }
    }

    companion object {
        const val EXTRA_LETTER_ID = "extra_letter_id"
        const val EXTRA_ALPHABET_TYPE = "extra_alphabet_type"
    }
}