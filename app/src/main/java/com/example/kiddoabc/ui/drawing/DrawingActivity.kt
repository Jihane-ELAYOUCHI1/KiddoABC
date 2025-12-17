package com.example.kiddoabc.ui.drawing

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.BounceInterpolator
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.kiddoabc.MyApplication
import com.example.kiddoabc.R
import com.example.kiddoabc.data.models.AlphabetType
import com.example.kiddoabc.data.models.Letter
import com.example.kiddoabc.data.repository.LetterRepository
import com.example.kiddoabc.databinding.ActivityDrawingBinding
import com.example.kiddoabc.ui.letters.LettersActivity
import com.example.kiddoabc.utils.SoundManager
import kotlinx.coroutines.launch

class DrawingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrawingBinding
    private lateinit var repository: LetterRepository
    private lateinit var soundManager: SoundManager

    private var currentLetter: Letter? = null
    private var alphabetType: AlphabetType = AlphabetType.ARABIC
    private var allLetters: List<Letter> = emptyList()
    private var currentIndex: Int = 0

    private val letterScores = mutableMapOf<String, Float>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val database = (application as MyApplication).database
        repository = LetterRepository(database.progressDao())
        soundManager = SoundManager(this)

        val letterId = intent.getStringExtra(LettersActivity.EXTRA_LETTER_ID)
        val alphabetTypeName = intent.getStringExtra(LettersActivity.EXTRA_ALPHABET_TYPE)
            ?: AlphabetType.ARABIC.name
        alphabetType = AlphabetType.valueOf(alphabetTypeName)

        setupToolbar()
        setupDrawingView()
        setupButtons()
        setupBackPressHandler()

        loadLetter(letterId)
    }

    private fun setupBackPressHandler() {
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (letterScores.isNotEmpty()) {
                    showExitConfirmationDialog()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun showExitConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Veux-tu quitter ?")
            .setMessage("Tu as déjà tracé des lettres. Es-tu sûr de vouloir quitter ?")
            .setPositiveButton("Oui") { _, _ ->
                finish()
            }
            .setNegativeButton("Non", null)
            .show()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "Trace la lettre"
        }
    }

    private fun setupDrawingView() {
        // Callback quand l'utilisateur termine de tracer
        binding.drawingView.onDrawingComplete = { accuracy ->
            currentLetter?.let { letter ->
                letterScores[letter.id] = accuracy
                saveProgress(letter.id, accuracy)
                showAccuracyFeedback(accuracy)
                soundManager.playEncouragementSound(accuracy)
            }
        }

        // Callback quand un chemin est tracé (pour activer/désactiver le bouton Retour)
        binding.drawingView.onPathDrawn = {
            updateUndoButton()
        }
    }

    private fun setupButtons() {
        // Bouton Effacer tout - Design enfantin avec emoji
        binding.btnClear.apply {
            text = "🗑️ Tout effacer"
            setOnClickListener {
                animateButton(it)
                binding.drawingView.clear()
                binding.tvScore.visibility = View.GONE
                updateUndoButton()
            }
        }

        // Bouton Retour/Annuler - NOUVELLE FONCTIONNALITÉ
        binding.btnUndo.apply {
            text = "↩️ Annuler"
            isEnabled = false
            setOnClickListener {
                animateButton(it)
                if (binding.drawingView.undo()) {
                    Toast.makeText(this@DrawingActivity, "Dernière action annulée", Toast.LENGTH_SHORT).show()
                }
                updateUndoButton()
            }
        }

        // Bouton Toggle Guide
        binding.btnToggleGuide.apply {
            text = "👁️ Guide"
            setOnClickListener {
                animateButton(it)
                toggleGuideVisibility()
            }
        }

        // Bouton Répéter le son
        binding.btnRepeatSound.apply {
            text = "🔊 Son"
            setOnClickListener {
                animateButton(it)
                currentLetter?.let { letter ->
                    soundManager.playSound(letter.soundFileName)
                }
            }
        }

        // Bouton Précédent
        binding.btnPrevious.apply {
            text = "⬅️ Avant"
            setOnClickListener {
                animateButton(it)
                previousLetter()
            }
        }

        // Bouton Suivant
        binding.btnNext.apply {
            text = "Après ➡️"
            setOnClickListener {
                animateButton(it)
                nextLetter()
            }
        }

        // Bouton Statistiques
        binding.btnStats.apply {
            text = "📊 Mes scores"
            setOnClickListener {
                animateButton(it)
                showStatistics()
            }
        }
    }

    /**
     * Met à jour l'état du bouton Annuler
     */
    private fun updateUndoButton() {
        binding.btnUndo.isEnabled = binding.drawingView.canUndo()
        binding.btnUndo.alpha = if (binding.drawingView.canUndo()) 1.0f else 0.5f
    }

    /**
     * Animation de clic pour les boutons
     */
    private fun animateButton(view: View) {
        val scaleDown = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 0.9f),
                ObjectAnimator.ofFloat(view, "scaleY", 0.9f)
            )
            duration = 100
        }

        val scaleUp = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(view, "scaleX", 1f),
                ObjectAnimator.ofFloat(view, "scaleY", 1f)
            )
            duration = 100
            interpolator = BounceInterpolator()
        }

        AnimatorSet().apply {
            play(scaleUp).after(scaleDown)
            start()
        }
    }

    private fun loadLetter(letterId: String?) {
        lifecycleScope.launch {
            try {
                allLetters = repository.getLettersByType(this@DrawingActivity, alphabetType)

                if (allLetters.isEmpty()) {
                    showError("Aucune lettre trouvée")
                    return@launch
                }

                currentIndex = if (letterId != null) {
                    allLetters.indexOfFirst { it.id == letterId }.coerceAtLeast(0)
                } else {
                    0
                }

                displayLetter(currentIndex)
            } catch (e: Exception) {
                showError("Erreur: ${e.message}")
            }
        }
    }

    private fun displayLetter(index: Int) {
        if (index !in allLetters.indices) return

        currentIndex = index
        currentLetter = allLetters[currentIndex]
        val letter = currentLetter ?: return

        // Animation de la lettre
        animateLetterDisplay()

        // Mettre à jour l'affichage
        binding.tvCurrentLetter.text = letter.character
        binding.tvLetterProgress.text = "${currentIndex + 1} / ${allLetters.size}"

        // Dessiner la lettre guide
        binding.drawingView.drawGuideLetter(letter.character)

        // Effacer le tracé précédent
        binding.drawingView.clear()
        binding.tvScore.visibility = View.GONE
        updateUndoButton()

        // Mettre à jour les boutons
        binding.btnPrevious.isEnabled = currentIndex > 0
        binding.btnPrevious.alpha = if (currentIndex > 0) 1.0f else 0.5f

        binding.btnNext.text = if (currentIndex < allLetters.size - 1) {
            "Après ➡️"
        } else {
            "Terminer 🎉"
        }

        // Afficher le score précédent si disponible
        letterScores[letter.id]?.let { score ->
            binding.tvPreviousScore.text = "Score: ${score.toInt()}% ⭐"
            binding.tvPreviousScore.visibility = View.VISIBLE
        } ?: run {
            binding.tvPreviousScore.visibility = View.GONE
        }

        // Jouer le son de la lettre
        soundManager.playSound(letter.soundFileName)
    }

    /**
     * Animation lors de l'affichage d'une nouvelle lettre
     */
    private fun animateLetterDisplay() {
        val fadeOut = ObjectAnimator.ofFloat(binding.tvCurrentLetter, "alpha", 1f, 0f).apply {
            duration = 150
        }

        val scale = AnimatorSet().apply {
            playTogether(
                ObjectAnimator.ofFloat(binding.tvCurrentLetter, "scaleX", 0.5f, 1.2f, 1f),
                ObjectAnimator.ofFloat(binding.tvCurrentLetter, "scaleY", 0.5f, 1.2f, 1f)
            )
            duration = 500
            interpolator = BounceInterpolator()
        }

        val fadeIn = ObjectAnimator.ofFloat(binding.tvCurrentLetter, "alpha", 0f, 1f).apply {
            duration = 200
        }

        AnimatorSet().apply {
            play(scale).with(fadeIn).after(fadeOut)
            start()
        }
    }

    private fun nextLetter() {
        if (currentIndex < allLetters.size - 1) {
            displayLetter(currentIndex + 1)
        } else {
            showFinalSummary()
        }
    }

    private fun previousLetter() {
        if (currentIndex > 0) {
            displayLetter(currentIndex - 1)
        }
    }

    private fun saveProgress(letterId: String, accuracy: Float) {
        lifecycleScope.launch {
            try {
                val currentProgress = repository.getProgressByLetter(letterId)
                val attempts = (currentProgress?.attempts ?: 0) + 1
                val isCompleted = accuracy >= 70f

                repository.saveProgress(
                    letterId = letterId,
                    alphabetType = alphabetType,
                    isCompleted = isCompleted,
                    attempts = attempts,
                    successRate = accuracy
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun showAccuracyFeedback(accuracy: Float) {
        val score = accuracy.toInt()

        // Afficher le score avec animation
        binding.tvScore.apply {
            text = "✨ Précision: $score% ✨"
            visibility = View.VISIBLE

            // Animation du score
            alpha = 0f
            scaleX = 0.5f
            scaleY = 0.5f

            animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(500)
                .setInterpolator(BounceInterpolator())
                .start()
        }

        // Changer la couleur selon le score
        val color = when {
            score >= 80 -> Color.parseColor("#4CAF50") // Vert
            score >= 60 -> Color.parseColor("#FF9800") // Orange
            else -> Color.parseColor("#F44336") // Rouge
        }
        binding.tvScore.setTextColor(color)

        // Afficher un message encourageant
        val message = when {
            score >= 90 -> "Excellent ! 🌟✨"
            score >= 80 -> "Très bien ! 👍🎉"
            score >= 70 -> "Bien ! 😊👏"
            score >= 60 -> "Pas mal ! 🙂"
            else -> "Continue à t'entraîner ! 💪"
        }

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun toggleGuideVisibility() {
        val currentAlpha = binding.drawingView.alpha
        if (currentAlpha == 1f) {
            // Réduire la transparence du guide
            binding.drawingView.setGuideAlpha(40)
            binding.btnToggleGuide.text = "👁️ Plus visible"
        } else {
            // Augmenter la transparence du guide
            binding.drawingView.setGuideAlpha(120)
            binding.btnToggleGuide.text = "👁️ Moins visible"
        }
        binding.drawingView.invalidate()
    }

    private fun showStatistics() {
        val stats = StringBuilder()
        stats.append("🎯 Tes résultats 🎯\n\n")

        var totalScore = 0f
        var completedLetters = 0

        for (letter in allLetters) {
            letterScores[letter.id]?.let { score ->
                val emoji = when {
                    score >= 90 -> "⭐⭐⭐"
                    score >= 80 -> "⭐⭐"
                    score >= 70 -> "⭐"
                    else -> "📝"
                }
                stats.append("$emoji ${letter.character}: ${score.toInt()}%\n")
                totalScore += score
                completedLetters++
            }
        }

        if (completedLetters > 0) {
            val average = totalScore / completedLetters
            stats.append("\n")
            stats.append("📊 Moyenne: ${average.toInt()}%\n")
            stats.append("✅ Lettres tracées: $completedLetters / ${allLetters.size}")
        } else {
            stats.append("Aucune lettre tracée pour le moment.\nCommence à dessiner ! 🎨")
        }

        AlertDialog.Builder(this)
            .setTitle("📈 Statistiques")
            .setMessage(stats.toString())
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showFinalSummary() {
        val completedCount = letterScores.size
        val totalScore = letterScores.values.sum()
        val average = if (completedCount > 0) totalScore / completedCount else 0f

        val emoji = when {
            average >= 90 -> "🏆🌟"
            average >= 80 -> "🎉👏"
            average >= 70 -> "😊👍"
            else -> "💪📝"
        }

        val message = "$emoji\n\nTu as tracé $completedCount lettre(s) sur ${allLetters.size}.\n\n" +
                "Score moyen: ${average.toInt()}%\n\nContinue comme ça ! 🚀"

        AlertDialog.Builder(this)
            .setTitle("Félicitations ! 🎊")
            .setMessage(message)
            .setPositiveButton("🔄 Recommencer") { _, _ ->
                letterScores.clear()
                displayLetter(0)
            }
            .setNegativeButton("✅ Terminer") { _, _ ->
                finish()
            }
            .show()
    }

    private fun showError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    override fun onPause() {
        super.onPause()
        soundManager.stopAll()
    }

    override fun onDestroy() {
        super.onDestroy()
        soundManager.release()
    }
}