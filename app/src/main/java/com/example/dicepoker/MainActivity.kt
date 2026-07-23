package com.example.dicepoker

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.BounceInterpolator
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    // Game State
    private var diceValues = intArrayOf(1, 1, 1, 1, 1)
    private var diceLocked = booleanArrayOf(false, false, false, false, false)
    private var diceSelected = booleanArrayOf(false, false, false, false, false)
    private var rollCount = 0
    private var round = 1
    private var totalScore = 0
    private var isPhase1 = true
    private var isSaloRound = false
    private var gameOver = false

    // Scores for numbers (1-6)
    private var numberScores = mutableMapOf<Int, Int?>(
        1 to null, 2 to null, 3 to null, 4 to null, 5 to null, 6 to null
    )

    // Scores for combinations
    private var combinationScores = mutableMapOf<String, Int?>(
        "pair" to null,
        "twoPairs" to null,
        "threeTwo" to null,
        "smallStraight" to null,
        "bigStraight" to null,
        "fourOfAKind" to null,
        "poker" to null
    )

    // UI Elements
    private lateinit var diceFrames: Array<FrameLayout>
    private lateinit var diceImages: Array<ImageView>
    private lateinit var btnRoll: Button
    private lateinit var tvRound: TextView
    private lateinit var tvRollCount: TextView
    private lateinit var tvTotalScore: TextView
    private lateinit var tvPhase: TextView
    private lateinit var tvMessage: TextView
    private lateinit var numbersContainer: LinearLayout
    private lateinit var combinationsContainer: LinearLayout
    private lateinit var btnNewGame: Button

    private val diceDrawables = mapOf(
        1 to R.drawable.dice_1,
        2 to R.drawable.dice_2,
        3 to R.drawable.dice_3,
        4 to R.drawable.dice_4,
        5 to R.drawable.dice_5,
        6 to R.drawable.dice_6
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        setupDiceClickListeners()
        setupRollButton()
        updateUI()
    }

    private fun initViews() {
        diceFrames = arrayOf(
            findViewById(R.id.dice1),
            findViewById(R.id.dice2),
            findViewById(R.id.dice3),
            findViewById(R.id.dice4),
            findViewById(R.id.dice5)
        )
        diceImages = arrayOf(
            findViewById(R.id.ivDice1),
            findViewById(R.id.ivDice2),
            findViewById(R.id.ivDice3),
            findViewById(R.id.ivDice4),
            findViewById(R.id.ivDice5)
        )
        btnRoll = findViewById(R.id.btnRoll)
        tvRound = findViewById(R.id.tvRound)
        tvRollCount = findViewById(R.id.tvRollCount)
        tvTotalScore = findViewById(R.id.tvTotalScore)
        tvPhase = findViewById(R.id.tvPhase)
        tvMessage = findViewById(R.id.tvMessage)
        numbersContainer = findViewById(R.id.numbersContainer)
        combinationsContainer = findViewById(R.id.combinationsContainer)
        btnNewGame = findViewById(R.id.btnNewGame)

        btnNewGame.setOnClickListener { startNewGame() }
    }

    private fun setupDiceClickListeners() {
        for (i in 0..4) {
            diceFrames[i].setOnClickListener {
                if (rollCount == 0 || gameOver) return@setOnClickListener

                if (!diceLocked[i]) {
                    diceSelected[i] = !diceSelected[i]
                    updateDiceVisuals()

                    val status = if (diceSelected[i]) "сохранён" else "будет переброшен"
                    tvMessage.text = "Кубик ${diceValues[i]} $status"
                }
            }
        }
    }

    private fun setupRollButton() {
        btnRoll.setOnClickListener {
            if (gameOver) return@setOnClickListener

            if (rollCount >= 3) {
                tvMessage.text = "Выбери, что записать!"
                return@setOnClickListener
            }

            rollDice()
        }
    }

    private fun rollDice() {
        rollCount++

        // 3D анимация для всех кубиков
        for (i in 0..4) {
            if (!diceSelected[i] && !diceLocked[i]) {
                animateDiceRoll3D(i)
            }
        }

        Handler(Looper.getMainLooper()).postDelayed({
            for (i in 0..4) {
                if (!diceSelected[i] && !diceLocked[i]) {
                    diceValues[i] = (1..6).random()
                }
            }

            for (i in 0..4) {
                if (diceSelected[i]) {
                    diceLocked[i] = true
                    diceSelected[i] = false
                }
            }

            updateDiceVisuals()
            updateUI()
            checkAvailableScores()

            if (rollCount >= 3) {
                tvMessage.text = "Выбери категорию для записи очков!"
                btnRoll.isEnabled = false
                btnRoll.alpha = 0.5f
            } else {
                tvMessage.text = "Бросок $rollCount/3. Выбери кубики для сохранения."
            }
        }, 800)
    }

    private fun animateDiceRoll3D(index: Int) {
        val frame = diceFrames[index]

        // Комплексная 3D анимация
        val rotateX = ObjectAnimator.ofFloat(frame, "rotationX", 0f, 360f)
        val rotateY = ObjectAnimator.ofFloat(frame, "rotationY", 0f, 720f)
        val scaleX = ObjectAnimator.ofFloat(frame, "scaleX", 1f, 1.3f, 1f)
        val scaleY = ObjectAnimator.ofFloat(frame, "scaleY", 1f, 1.3f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(rotateX, rotateY, scaleX, scaleY)
        animatorSet.duration = 700
        animatorSet.interpolator = BounceInterpolator()
        animatorSet.start()
    }

    private fun updateDiceVisuals() {
        for (i in 0..4) {
            diceImages[i].setImageResource(diceDrawables[diceValues[i]] ?: R.drawable.dice_1)

            when {
                diceLocked[i] -> {
                    diceFrames[i].setBackgroundResource(R.drawable.bg_dice_locked)
                    diceFrames[i].alpha = 0.8f
                }
                diceSelected[i] -> {
                    diceFrames[i].setBackgroundResource(R.drawable.bg_dice_selected)
                    diceFrames[i].alpha = 1.0f
                }
                else -> {
                    diceFrames[i].setBackgroundResource(R.drawable.bg_dice)
                    diceFrames[i].alpha = 1.0f
                }
            }
        }
    }

    private fun updateUI() {
        tvRound.text = "Раунд: $round/13"
        tvRollCount.text = "Бросок: $rollCount/3"
        tvTotalScore.text = "Очки: $totalScore"

        tvPhase.text = when {
            isSaloRound -> "🥓 САЛО - Финальный бросок!"
            isPhase1 -> "🔢 Фаза 1: Собери цифры (ход $round/6)"
            else -> "🃏 Фаза 2: Собери комбинации"
        }

        renderScoreTable()
    }

    private fun renderScoreTable() {
        numbersContainer.removeAllViews()
        combinationsContainer.removeAllViews()

        // Numbers section
        for (num in 1..6) {
            val item = layoutInflater.inflate(R.layout.item_score, numbersContainer, false)
            val card = item.findViewById<CardView>(R.id.cardScore)
            val name = item.findViewById<TextView>(R.id.tvScoreName)
            val value = item.findViewById<TextView>(R.id.tvScoreValue)
            val action = item.findViewById<TextView>(R.id.tvScoreAction)

            name.text = "$num${when(num) {
                1 -> " (единицы)"
                2 -> " (двойки)"
                3 -> " (тройки)"
                4 -> " (четвёрки)"
                5 -> " (пятёрки)"
                else -> " (шестёрки)"
            }}"

            val scoreValue = numberScores[num]
            when {
                scoreValue != null -> {
                    value.text = if (scoreValue >= 0) "+$scoreValue" else "$scoreValue"
                    if (scoreValue >= 0) {
                        value.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
                        card.setBackgroundResource(R.drawable.bg_score_filled)
                    } else {
                        value.setTextColor(ContextCompat.getColor(this, R.color.accent_red))
                        card.setBackgroundResource(R.drawable.bg_score_cross)
                    }
                    action.visibility = View.GONE
                }
                rollCount > 0 && !gameOver && isPhase1 -> {
                    val possibleScore = calculateNumberScore(num)
                    val canRecord = canRecordNumber(num)

                    if (canRecord) {
                        value.text = possibleScore.toString()
                        value.setTextColor(ContextCompat.getColor(this, R.color.accent_gold))
                        action.visibility = View.VISIBLE
                        card.setOnClickListener { recordNumberScore(num, possibleScore) }
                    } else {
                        value.text = "-"
                        value.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                        action.visibility = View.GONE
                    }
                }
                else -> {
                    value.text = "-"
                    value.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                    action.visibility = View.GONE
                }
            }

            numbersContainer.addView(item)
        }

        // Combinations section (показываем всегда, но активны только во 2-й фазе)
        val combos = listOf(
            "pair" to "Пара (+10 + сумма)",
            "twoPairs" to "Две пары (+20 + сумма)",
            "threeTwo" to "3+2 (+30 + сумма)",
            "smallStraight" to "Малый стрит 1-5 (+40)",
            "bigStraight" to "Большой стрит 2-6 (+60)",
            "fourOfAKind" to "Каре (+80 + сумма ×2 если с 1-го раза)",
            "poker" to "Покер (+100 + сумма ×2 если с 1-го раза)"
        )

        for ((key, label) in combos) {
            val item = layoutInflater.inflate(R.layout.item_score, combinationsContainer, false)
            val card = item.findViewById<CardView>(R.id.cardScore)
            val name = item.findViewById<TextView>(R.id.tvScoreName)
            val value = item.findViewById<TextView>(R.id.tvScoreValue)
            val action = item.findViewById<TextView>(R.id.tvScoreAction)

            name.text = label

            val scoreValue = combinationScores[key]
            when {
                scoreValue != null -> {
                    value.text = if (scoreValue >= 0) "+$scoreValue" else "$scoreValue"
                    if (scoreValue >= 0) {
                        value.setTextColor(ContextCompat.getColor(this, R.color.accent_green))
                        card.setBackgroundResource(R.drawable.bg_score_filled)
                    } else {
                        value.setTextColor(ContextCompat.getColor(this, R.color.accent_red))
                        card.setBackgroundResource(R.drawable.bg_score_cross)
                    }
                    action.visibility = View.GONE
                }
                rollCount > 0 && !gameOver && !isPhase1 -> {
                    val possibleScore = calculateCombinationScore(key)
                    if (possibleScore != null) {
                        value.text = "+$possibleScore"
                        value.setTextColor(ContextCompat.getColor(this, R.color.accent_gold))
                        action.visibility = View.VISIBLE
                        card.setOnClickListener { recordCombinationScore(key, possibleScore) }
                    } else {
                        value.text = "0"
                        value.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                        action.visibility = View.GONE
                    }
                }
                else -> {
                    value.text = "-"
                    value.setTextColor(ContextCompat.getColor(this, R.color.text_secondary))
                    action.visibility = View.GONE
                }
            }

            combinationsContainer.addView(item)
        }
    }

    // Проверяем, можно ли записать эту цифру (только если собрали 3+ или это последний бросок)
    private fun canRecordNumber(number: Int): Boolean {
        val count = diceValues.count { it == number }
        return count >= 3 || rollCount >= 3
    }

    private fun calculateNumberScore(number: Int): Int {
        val count = diceValues.count { it == number }
        return if (count >= 3) {
            count * number
        } else {
            -number
        }
    }

    private fun calculateCombinationScore(key: String): Int? {
        val sorted = diceValues.sorted()
        val counts = diceValues.groupBy { it }.mapValues { it.value.size }

        return when (key) {
            "pair" -> {
                val pairs = counts.filter { it.value >= 2 }
                if (pairs.isNotEmpty()) {
                    val pairValue = pairs.keys.maxOrNull() ?: 0
                    10 + pairValue * 2
                } else null
            }
            "twoPairs" -> {
                val pairs = counts.filter { it.value >= 2 }.keys.sortedDescending()
                if (pairs.size >= 2) {
                    20 + pairs[0] * 2 + pairs[1] * 2
                } else null
            }
            "threeTwo" -> {
                val hasThree = counts.any { it.value >= 3 }
                val hasTwo = counts.any { it.value >= 2 }
                if (hasThree && hasTwo && counts.size == 2) {
                    30 + diceValues.fold(0) { acc, v -> acc + v }
                } else null
            }
            "smallStraight" -> {
                if (sorted.toSet() == setOf(1, 2, 3, 4, 5)) 40 else null
            }
            "bigStraight" -> {
                if (sorted.toSet() == setOf(2, 3, 4, 5, 6)) 60 else null
            }
            "fourOfAKind" -> {
                val four = counts.entries.find { it.value >= 4 }
                if (four != null) {
                    val base = 80 + four.key * 4
                    if (rollCount == 1) base * 2 else base
                } else null
            }
            "poker" -> {
                if (counts.any { it.value == 5 }) {
                    val value = diceValues[0]
                    val base = 100 + value * 5
                    if (rollCount == 1) base * 2 else base
                } else null
            }
            else -> null
        }
    }

    private fun recordNumberScore(number: Int, score: Int) {
        numberScores[number] = score
        totalScore += score
        nextRound()
    }

    private fun recordCombinationScore(key: String, score: Int) {
        combinationScores[key] = score
        totalScore += score
        nextRound()
    }

    private fun checkAvailableScores() {
        if (rollCount >= 3) {
            val hasAvailable = if (isPhase1) {
                (1..6).any { numberScores[it] == null && canRecordNumber(it) && calculateNumberScore(it) != null }
            } else {
                combinationScores.keys.any { combinationScores[it] == null && calculateCombinationScore(it) != null }
            }

            if (!hasAvailable && !isSaloRound) {
                tvMessage.text = "Нет доступных комбинаций! Выбери категорию для зачёркивания."
                enableCrossOutMode()
            }
        }
    }

    private fun enableCrossOutMode() {
        if (isPhase1) {
            for (num in 1..6) {
                if (numberScores[num] == null) {
                    val index = num - 1
                    if (index < numbersContainer.childCount) {
                        val item = numbersContainer.getChildAt(index)
                        val card = item.findViewById<CardView>(R.id.cardScore)
                        val action = item.findViewById<TextView>(R.id.tvScoreAction)
                        action.text = "ЗАЧЁРКНУТЬ (-$num)"
                        action.setTextColor(ContextCompat.getColor(this, R.color.accent_red))
                        action.visibility = View.VISIBLE
                        card.setOnClickListener {
                            numberScores[num] = -num
                            totalScore -= num
                            nextRound()
                        }
                    }
                }
            }
        } else {
            val comboKeys = listOf("pair", "twoPairs", "threeTwo", "smallStraight", "bigStraight", "fourOfAKind", "poker")
            val comboNames = mapOf(
                "pair" to 10, "twoPairs" to 20, "threeTwo" to 30,
                "smallStraight" to 40, "bigStraight" to 60, "fourOfAKind" to 80, "poker" to 100
            )
            for (key in comboKeys) {
                if (combinationScores[key] == null) {
                    val index = comboKeys.indexOf(key)
                    if (index < combinationsContainer.childCount) {
                        val item = combinationsContainer.getChildAt(index)
                        val card = item.findViewById<CardView>(R.id.cardScore)
                        val action = item.findViewById<TextView>(R.id.tvScoreAction)
                        val penalty = comboNames[key] ?: 10
                        action.text = "ЗАЧЁРКНУТЬ (-$penalty)"
                        action.setTextColor(ContextCompat.getColor(this, R.color.accent_red))
                        action.visibility = View.VISIBLE
                        card.setOnClickListener {
                            combinationScores[key] = -penalty
                            totalScore -= penalty
                            nextRound()
                        }
                    }
                }
            }
        }
    }

    private fun nextRound() {
        if (isSaloRound) {
            endGame()
            return
        }

        val phase1Complete = numberScores.values.all { it != null }
        val phase2Complete = combinationScores.values.all { it != null }

        if (phase1Complete && isPhase1) {
            val numbersSum = numberScores.values.filterNotNull().fold(0) { acc, v -> acc + v }
            if (numbersSum > 0) {
                totalScore += 100
                Toast.makeText(this, "🎉 Бонус +100 за положительный счёт цифр!", Toast.LENGTH_LONG).show()
            }
            isPhase1 = false
            Toast.makeText(this, "Фаза 2: Комбинации!", Toast.LENGTH_SHORT).show()
        }

        if (phase2Complete && !isPhase1) {
            isSaloRound = true
            Toast.makeText(this, "🥓 САЛО! Финальный бросок!", Toast.LENGTH_LONG).show()
        }

        round++
        rollCount = 0
        diceLocked.fill(false)
        diceSelected.fill(false)
        diceValues.fill(1)
        updateDiceVisuals()

        btnRoll.isEnabled = true
        btnRoll.alpha = 1.0f
        btnRoll.text = if (isSaloRound) "🥓 БРОСИТЬ САЛО!" else "🎲 БРОСИТЬ КУБИКИ"

        if (isSaloRound) {
            tvMessage.text = "Финальный бросок! Любая комбинация = очки!"
        } else {
            tvMessage.text = "Новый раунд! Бросай кубики!"
        }

        updateUI()
    }

    private fun endGame() {
        gameOver = true
        val saloScore = calculateSaloScore()
        totalScore += saloScore

        btnRoll.isEnabled = false
        btnRoll.alpha = 0.5f
        btnNewGame.visibility = View.VISIBLE

        tvMessage.text = "Игра окончена! Финальный счёт: $totalScore"

        AlertDialog.Builder(this)
            .setTitle("🎉 Игра окончена!")
            .setMessage("Ваш финальный счёт: $totalScore\n\nСало: +$saloScore\n\nОтличная игра!")
            .setPositiveButton("Новая игра") { _, _ -> startNewGame() }
            .setCancelable(false)
            .show()

        updateUI()
    }

    private fun calculateSaloScore(): Int {
        val sorted = diceValues.sorted()
        val counts = diceValues.groupBy { it }.mapValues { it.value.size }
        val diceSum = diceValues.fold(0) { acc, value -> acc + value }

        return when {
            counts.any { it.value == 5 } -> 100 + diceSum
            counts.any { it.value >= 4 } -> 80 + diceSum
            sorted.toSet() == setOf(2, 3, 4, 5, 6) -> 60 + diceSum
            sorted.toSet() == setOf(1, 2, 3, 4, 5) -> 40 + diceSum
            counts.filter { it.value >= 2 }.size >= 2 -> 20 + diceSum
            counts.any { it.value >= 3 } && counts.any { it.value >= 2 } -> 30 + diceSum
            counts.any { it.value >= 2 } -> 10 + diceSum
            else -> diceSum
        }
    }

    private fun startNewGame() {
        diceValues = intArrayOf(1, 1, 1, 1, 1)
        diceLocked.fill(false)
        diceSelected.fill(false)
        rollCount = 0
        round = 1
        totalScore = 0
        isPhase1 = true
        isSaloRound = false
        gameOver = false

        numberScores = mutableMapOf(
            1 to null, 2 to null, 3 to null, 4 to null, 5 to null, 6 to null
        )
        combinationScores = mutableMapOf(
            "pair" to null, "twoPairs" to null, "threeTwo" to null,
            "smallStraight" to null, "bigStraight" to null,
            "fourOfAKind" to null, "poker" to null
        )

        btnRoll.isEnabled = true
        btnRoll.alpha = 1.0f
        btnRoll.text = "🎲 БРОСИТЬ КУБИКИ"
        btnNewGame.visibility = View.GONE

        updateDiceVisuals()
        updateUI()

        tvMessage.text = "Новая игра! Нажми 'БРОСИТЬ КУБИКИ'!"
    }
}
