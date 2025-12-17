package com.example.kiddoabc.ui.drawing

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.OvershootInterpolator
import kotlin.math.pow
import kotlin.math.sqrt

class DrawingView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // Paint pour le tracé de l'utilisateur avec gradient coloré
    private var userPaint: Paint = Paint()
    private var currentPath: Path = Path()

    // Liste des chemins tracés (pour gérer l'annulation)
    private val drawnPaths = mutableListOf<DrawnPath>()

    // Paint pour la lettre guide
    private var guidePaint: Paint = Paint()
    private var guidePointsPaint: Paint = Paint()
    private var guidePath: Path = Path()

    // Paint pour afficher la lettre réelle (texte)
    private var letterTextPaint: Paint = Paint()
    private var showRealLetter: Boolean = false
    private var currentLetterText: String = ""

    // Canvas et bitmap pour enregistrer les tracés
    private var drawCanvas: Canvas? = null
    private var canvasBitmap: Bitmap? = null
    private var canvasPaint: Paint = Paint(Paint.DITHER_FLAG)

    // Points pour le calcul de précision
    private val allUserPoints = mutableListOf<PointF>()
    private val currentPathPoints = mutableListOf<PointF>()
    private val guidePoints = mutableListOf<PointF>()

    // Paramètres de dessin
    private var brushSize: Float = 26f
    private var guideColor: Int = Color.parseColor("#E0E0E0")

    // Animation
    private var celebrationAnimator: ValueAnimator? = null
    private var celebrationScale = 1f

    // Lettre actuelle
    private var currentLetter: String = "A"

    // Callback
    var onDrawingComplete: ((Float) -> Unit)? = null
    var onPathDrawn: (() -> Unit)? = null

    // Classe pour stocker un chemin avec sa peinture
    private data class DrawnPath(val path: Path, val paint: Paint)

    init {
        setupDrawing()
    }

    private fun setupDrawing() {
        // Configuration du paint pour le tracé utilisateur avec gradient
        userPaint.apply {
            style = Paint.Style.STROKE
            strokeWidth = brushSize
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
            isAntiAlias = true
        }

        // Configuration du paint pour la lettre guide
        guidePaint.apply {
            color = guideColor
            isAntiAlias = true
            strokeWidth = brushSize * 1.8f
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            alpha = 120
        }

        // Configuration du paint pour les points de guidage
        guidePointsPaint.apply {
            color = Color.parseColor("#FF9800") // Orange
            style = Paint.Style.FILL
            isAntiAlias = true
            alpha = 150
        }

        // Configuration du paint pour afficher la lettre réelle (texte)
        letterTextPaint.apply {
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            color = Color.parseColor("#E0E0E0") // Gris clair transparent
            alpha = 80
            style = Paint.Style.FILL
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        canvasBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        drawCanvas = Canvas(canvasBitmap!!)

        // Mettre à jour le gradient avec les nouvelles dimensions
        updateGradientPaint()

        // Redessiner la lettre guide
        drawGuideLetter(currentLetter)
    }

    private fun updateGradientPaint() {
        if (width > 0 && height > 0) {
            val gradient = LinearGradient(
                0f, 0f, width.toFloat(), height.toFloat(),
                intArrayOf(
                    Color.parseColor("#F48FB1"), // rose
                    Color.parseColor("#90CAF9"), // bleu
                    Color.parseColor("#FFF59D")  // jaune
                ),
                null,
                Shader.TileMode.CLAMP
            )
            userPaint.shader = gradient
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Appliquer l'animation de célébration
        canvas.save()
        canvas.scale(celebrationScale, celebrationScale, width / 2f, height / 2f)

        // Si c'est une lettre arabe, afficher la vraie lettre en arrière-plan
        if (showRealLetter && currentLetterText.isNotEmpty()) {
            drawRealLetter(canvas)
        } else {
            // Pour les lettres latines, dessiner le guide tracé
            canvas.drawPath(guidePath, guidePaint)
            drawGuidePoints(canvas)
        }

        // Dessiner tous les chemins précédemment tracés
        for (drawnPath in drawnPaths) {
            canvas.drawPath(drawnPath.path, drawnPath.paint)
        }

        // Dessiner le chemin actuel
        canvas.drawPath(currentPath, userPaint)

        canvas.restore()
    }

    /**
     * Affiche la vraie lettre arabe en arrière-plan (semi-transparent)
     */
    private fun drawRealLetter(canvas: Canvas) {
        // Calculer la taille de la lettre pour qu'elle remplisse bien la vue
        val textSize = minOf(width, height) * 0.7f
        letterTextPaint.textSize = textSize

        // Calculer la position verticale (centrer le texte)
        val textBounds = android.graphics.Rect()
        letterTextPaint.getTextBounds(currentLetterText, 0, currentLetterText.length, textBounds)
        val textHeight = textBounds.height()
        val y = height / 2f + textHeight / 2f

        // Dessiner la lettre centrée
        canvas.drawText(currentLetterText, width / 2f, y, letterTextPaint)
    }

    private fun drawGuidePoints(canvas: Canvas) {
        // Dessiner des points le long du guide pour aider l'enfant
        val pathMeasure = PathMeasure(guidePath, false)
        val length = pathMeasure.length
        val step = 30f // Un point tous les 30 pixels
        val coords = FloatArray(2)

        var distance = 0f
        var pointIndex = 0

        while (distance < length) {
            pathMeasure.getPosTan(distance, coords, null)

            // Alterner entre points normaux et points plus gros
            val radius = if (pointIndex % 3 == 0) 8f else 5f
            canvas.drawCircle(coords[0], coords[1], radius, guidePointsPaint)

            distance += step
            pointIndex++
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val touchX = event.x
        val touchY = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                currentPath.reset()
                currentPath.moveTo(touchX, touchY)
                currentPathPoints.clear()
                currentPathPoints.add(PointF(touchX, touchY))
                allUserPoints.add(PointF(touchX, touchY))
            }
            MotionEvent.ACTION_MOVE -> {
                currentPath.lineTo(touchX, touchY)
                currentPathPoints.add(PointF(touchX, touchY))
                allUserPoints.add(PointF(touchX, touchY))
            }
            MotionEvent.ACTION_UP -> {
                // Sauvegarder le chemin tracé
                val pathCopy = Path(currentPath)
                val paintCopy = Paint(userPaint)
                drawnPaths.add(DrawnPath(pathCopy, paintCopy))

                // Réinitialiser le chemin actuel
                currentPath.reset()
                currentPathPoints.clear()

                // Notifier qu'un chemin a été tracé
                onPathDrawn?.invoke()

                // Calculer et notifier la précision
                val accuracy = calculateAccuracy()
                if (accuracy > 0) {
                    onDrawingComplete?.invoke(accuracy)

                    // Lancer l'animation de célébration si bon score
                    if (accuracy >= 70) {
                        startCelebrationAnimation()
                    }
                }
            }
            else -> return false
        }

        invalidate()
        return true
    }

    private fun startCelebrationAnimation() {
        celebrationAnimator?.cancel()

        celebrationAnimator = ValueAnimator.ofFloat(1f, 1.15f, 1f).apply {
            duration = 500
            interpolator = OvershootInterpolator()
            addUpdateListener { animator ->
                celebrationScale = animator.animatedValue as Float
                invalidate()
            }
            start()
        }
    }

    fun drawGuideLetter(letter: String) {
        currentLetter = letter
        currentLetterText = letter
        guidePath.reset()
        guidePoints.clear()

        val centerX = width / 2f
        val centerY = height / 2f
        val letterSize = minOf(width, height) * 0.6f

        // Vérifier si c'est une lettre arabe
        showRealLetter = isArabicLetter(letter)

        if (!showRealLetter) {
            // Pour les lettres latines, créer le path pour la lettre
            when (letter.uppercase()) {
                "A" -> drawLetterA(guidePath, centerX, centerY, letterSize)
                "B" -> drawLetterB(guidePath, centerX, centerY, letterSize)
                "C" -> drawLetterC(guidePath, centerX, centerY, letterSize)
                "D" -> drawLetterD(guidePath, centerX, centerY, letterSize)
                "E" -> drawLetterE(guidePath, centerX, centerY, letterSize)
                "F" -> drawLetterF(guidePath, centerX, centerY, letterSize)
                "G" -> drawLetterG(guidePath, centerX, centerY, letterSize)
                "H" -> drawLetterH(guidePath, centerX, centerY, letterSize)
                "I" -> drawLetterI(guidePath, centerX, centerY, letterSize)
                "J" -> drawLetterJ(guidePath, centerX, centerY, letterSize)
                "K" -> drawLetterK(guidePath, centerX, centerY, letterSize)
                "L" -> drawLetterL(guidePath, centerX, centerY, letterSize)
                "M" -> drawLetterM(guidePath, centerX, centerY, letterSize)
                "N" -> drawLetterN(guidePath, centerX, centerY, letterSize)
                "O" -> drawLetterO(guidePath, centerX, centerY, letterSize)
                "P" -> drawLetterP(guidePath, centerX, centerY, letterSize)
                "Q" -> drawLetterQ(guidePath, centerX, centerY, letterSize)
                "R" -> drawLetterR(guidePath, centerX, centerY, letterSize)
                "S" -> drawLetterS(guidePath, centerX, centerY, letterSize)
                "T" -> drawLetterT(guidePath, centerX, centerY, letterSize)
                "U" -> drawLetterU(guidePath, centerX, centerY, letterSize)
                "V" -> drawLetterV(guidePath, centerX, centerY, letterSize)
                "W" -> drawLetterW(guidePath, centerX, centerY, letterSize)
                "X" -> drawLetterX(guidePath, centerX, centerY, letterSize)
                "Y" -> drawLetterY(guidePath, centerX, centerY, letterSize)
                "Z" -> drawLetterZ(guidePath, centerX, centerY, letterSize)
            }

            extractPathPoints(guidePath, guidePoints)
        }

        invalidate()
    }

    /**
     * Vérifie si une lettre est arabe
     */
    private fun isArabicLetter(letter: String): Boolean {
        if (letter.isEmpty()) return false
        val code = letter[0].code
        // Plage Unicode pour les lettres arabes : 0x0600 - 0x06FF
        return code in 0x0600..0x06FF
    }

    private fun calculateAccuracy(): Float {
        // Pour les lettres arabes, utiliser une méthode simplifiée
        // car on ne peut pas comparer avec un path
        if (showRealLetter) {
            // Calculer simplement basé sur la quantité de tracé
            if (allUserPoints.isEmpty()) return 0f

            // Plus l'enfant trace, meilleur est le score (jusqu'à un certain point)
            val expectedPoints = 200 // Nombre de points attendus pour une lettre complète
            val actualPoints = allUserPoints.size

            // Score basé sur la couverture
            val coverageScore = (actualPoints.toFloat() / expectedPoints * 100).coerceIn(0f, 100f)

            // Bonus si l'enfant a bien tracé (pas trop de points)
            val efficiency = if (actualPoints > expectedPoints * 2) {
                50f // Pénalité si trop de tracé
            } else {
                100f
            }

            return ((coverageScore + efficiency) / 2).coerceIn(0f, 100f)
        }

        // Pour les lettres latines, utiliser la méthode originale
        if (allUserPoints.isEmpty() || guidePoints.isEmpty()) return 0f

        var totalDistance = 0f
        var matchedPoints = 0
        val threshold = 60f

        for (userPoint in allUserPoints) {
            var minDistance = Float.MAX_VALUE

            for (guidePoint in guidePoints) {
                val distance = distanceBetween(userPoint, guidePoint)
                if (distance < minDistance) {
                    minDistance = distance
                }
            }

            if (minDistance < threshold) {
                matchedPoints++
                totalDistance += minDistance
            }
        }

        val coverageScore = (matchedPoints.toFloat() / guidePoints.size) * 100
        val precisionScore = if (matchedPoints > 0) {
            100 - (totalDistance / matchedPoints).coerceIn(0f, 100f)
        } else 0f

        return ((coverageScore + precisionScore) / 2).coerceIn(0f, 100f)
    }

    private fun distanceBetween(p1: PointF, p2: PointF): Float {
        return sqrt((p1.x - p2.x).pow(2) + (p1.y - p2.y).pow(2))
    }

    private fun extractPathPoints(path: Path, points: MutableList<PointF>) {
        points.clear()
        val pathMeasure = PathMeasure(path, false)
        val length = pathMeasure.length
        val step = 5f

        var distance = 0f
        val coords = FloatArray(2)

        while (distance < length) {
            pathMeasure.getPosTan(distance, coords, null)
            points.add(PointF(coords[0], coords[1]))
            distance += step
        }
    }

    /**
     * Efface tout le dessin
     */
    fun clear() {
        currentPath.reset()
        drawnPaths.clear()
        allUserPoints.clear()
        currentPathPoints.clear()
        invalidate()
    }

    /**
     * Annule le dernier tracé (fonction Retour)
     */
    fun undo(): Boolean {
        if (drawnPaths.isNotEmpty()) {
            drawnPaths.removeAt(drawnPaths.size - 1)

            // Recalculer tous les points utilisateur
            allUserPoints.clear()
            // Note: Cette implémentation simple efface tous les points
            // Pour une version plus précise, il faudrait stocker les points par chemin

            invalidate()
            return true
        }
        return false
    }

    /**
     * Vérifie s'il y a des tracés à annuler
     */
    fun canUndo(): Boolean = drawnPaths.isNotEmpty()

    fun reset() {
        clear()
        guidePath.reset()
        guidePoints.clear()
        invalidate()
    }

    fun setBrushSize(newSize: Float) {
        brushSize = newSize
        userPaint.strokeWidth = brushSize
    }

    fun setGuideColor(newColor: Int) {
        guideColor = newColor
        guidePaint.color = guideColor
        letterTextPaint.color = newColor
        letterTextPaint.alpha = 80
    }

    /**
     * Modifie la transparence de la lettre guide
     */
    fun setGuideAlpha(alpha: Int) {
        guidePaint.alpha = alpha
        letterTextPaint.alpha = alpha
        invalidate()
    }

    // Méthodes pour dessiner les lettres latines (A-Z) - GARDÉES POUR L'ALPHABET LATIN
    private fun drawLetterA(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2
        val middle = cy

        path.moveTo(cx, top)
        path.lineTo(left, bottom)
        path.moveTo(cx, top)
        path.lineTo(right, bottom)
        path.moveTo(left + size / 6, middle)
        path.lineTo(right - size / 6, middle)
    }

    private fun drawLetterB(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(left, top)
        path.lineTo(left, bottom)
        path.moveTo(left, top)
        path.cubicTo(right, top, right, cy, left, cy)
        path.moveTo(left, cy)
        path.cubicTo(right, cy, right, bottom, left, bottom)
    }

    private fun drawLetterC(path: Path, cx: Float, cy: Float, size: Float) {
        val rect = RectF(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2)
        path.addArc(rect, 45f, 270f)
    }

    private fun drawLetterD(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(left, top)
        path.lineTo(left, bottom)
        path.moveTo(left, top)
        path.cubicTo(cx + size / 2, top, cx + size / 2, bottom, left, bottom)
    }

    private fun drawLetterE(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(right, top)
        path.lineTo(left, top)
        path.lineTo(left, bottom)
        path.lineTo(right, bottom)
        path.moveTo(left, cy)
        path.lineTo(right - size / 6, cy)
    }

    private fun drawLetterF(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(left, bottom)
        path.lineTo(left, top)
        path.lineTo(right, top)
        path.moveTo(left, cy)
        path.lineTo(right - size / 6, cy)
    }

    private fun drawLetterG(path: Path, cx: Float, cy: Float, size: Float) {
        val rect = RectF(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2)
        path.addArc(rect, 45f, 270f)
        path.lineTo(cx + size / 2, cy)
        path.lineTo(cx, cy)
    }

    private fun drawLetterH(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(left, top)
        path.lineTo(left, bottom)
        path.moveTo(right, top)
        path.lineTo(right, bottom)
        path.moveTo(left, cy)
        path.lineTo(right, cy)
    }

    private fun drawLetterI(path: Path, cx: Float, cy: Float, size: Float) {
        val top = cy - size / 2
        val bottom = cy + size / 2
        val width = size / 3

        path.moveTo(cx - width, top)
        path.lineTo(cx + width, top)
        path.moveTo(cx, top)
        path.lineTo(cx, bottom)
        path.moveTo(cx - width, bottom)
        path.lineTo(cx + width, bottom)
    }

    private fun drawLetterJ(path: Path, cx: Float, cy: Float, size: Float) {
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(right, top)
        path.lineTo(right, bottom - size / 4)
        path.cubicTo(right, bottom, cx - size / 3, bottom, cx - size / 3, bottom - size / 4)
    }

    private fun drawLetterK(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(left, top)
        path.lineTo(left, bottom)
        path.moveTo(right, top)
        path.lineTo(left, cy)
        path.lineTo(right, bottom)
    }

    private fun drawLetterL(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(left, top)
        path.lineTo(left, bottom)
        path.lineTo(right, bottom)
    }

    private fun drawLetterM(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 2
        val right = cx + size / 2
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(left, bottom)
        path.lineTo(left, top)
        path.lineTo(cx, cy)
        path.lineTo(right, top)
        path.lineTo(right, bottom)
    }

    private fun drawLetterN(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(left, bottom)
        path.lineTo(left, top)
        path.lineTo(right, bottom)
        path.lineTo(right, top)
    }

    private fun drawLetterO(path: Path, cx: Float, cy: Float, size: Float) {
        val rect = RectF(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2)
        path.addOval(rect, Path.Direction.CW)
    }

    private fun drawLetterP(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(left, bottom)
        path.lineTo(left, top)
        path.cubicTo(right, top, right, cy, left, cy)
    }

    private fun drawLetterQ(path: Path, cx: Float, cy: Float, size: Float) {
        val rect = RectF(cx - size / 2, cy - size / 2, cx + size / 2, cy + size / 2)
        path.addOval(rect, Path.Direction.CW)
        path.moveTo(cx, cy + size / 4)
        path.lineTo(cx + size / 2, cy + size / 2)
    }

    private fun drawLetterR(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(left, bottom)
        path.lineTo(left, top)
        path.cubicTo(right, top, right, cy, left, cy)
        path.moveTo(left, cy)
        path.lineTo(right, bottom)
    }

    private fun drawLetterS(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(right, top + size / 6)
        path.cubicTo(right, top, left, top, left, top + size / 6)
        path.cubicTo(left, cy, right, cy, right, bottom - size / 6)
        path.cubicTo(right, bottom, left, bottom, left, bottom - size / 6)
    }

    private fun drawLetterT(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(left, top)
        path.lineTo(right, top)
        path.moveTo(cx, top)
        path.lineTo(cx, bottom)
    }

    private fun drawLetterU(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(left, top)
        path.lineTo(left, bottom - size / 4)
        path.cubicTo(left, bottom, right, bottom, right, bottom - size / 4)
        path.lineTo(right, top)
    }

    private fun drawLetterV(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(left, top)
        path.lineTo(cx, bottom)
        path.lineTo(right, top)
    }

    private fun drawLetterW(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 2
        val right = cx + size / 2
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(left, top)
        path.lineTo(cx - size / 4, bottom)
        path.lineTo(cx, cy)
        path.lineTo(cx + size / 4, bottom)
        path.lineTo(right, top)
    }

    private fun drawLetterX(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(left, top)
        path.lineTo(right, bottom)
        path.moveTo(right, top)
        path.lineTo(left, bottom)
    }

    private fun drawLetterY(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(left, top)
        path.lineTo(cx, cy)
        path.lineTo(right, top)
        path.moveTo(cx, cy)
        path.lineTo(cx, bottom)
    }

    private fun drawLetterZ(path: Path, cx: Float, cy: Float, size: Float) {
        val left = cx - size / 3
        val right = cx + size / 3
        val top = cy - size / 2
        val bottom = cy + size / 2

        path.moveTo(left, top)
        path.lineTo(right, top)
        path.lineTo(left, bottom)
        path.lineTo(right, bottom)
    }
}