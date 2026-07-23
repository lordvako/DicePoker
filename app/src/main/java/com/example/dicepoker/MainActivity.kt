package com.example.dicepoker

import android.animation.ObjectAnimator
import android.graphics.Color
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

    private var diceValues = intArrayOf(1, 1, 1, 1, 1)
    private var diceLocked = booleanArrayOf(false, false, false, false, false)
    private var diceSelected = booleanArrayOf(false, false, false, false, false)
    private var rollCount = 0
    private var round = 1
    private var totalScore = 0
    private var isPhase1 = true
    private var isSaloRound = false
    private var gameOver = false

    private var numberScores = mutableMapOf<Int, Int?>(1 to null, 2 to null, 3 to null, 4 to null, 5 to null, 6 to null)

    private var combinationScores = mutableMapOf<String, Int?>(
        "pair" to null,
        "twoPairs" to null,
        "threeTwo" to null,
        "smallStraight" to null,
        "bigStraight" to null,
        "fourOfAKind" to null,
        "poker" to null
    )

    private lateinit var diceFrames: Array<FrameLayout>
    private lateinit var diceImages: Array<ImageView>
    private lateinit var btnRoll: Button
    private lateinit var btnRestart: Button
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
        setupRestartButton()
        updateUI()
    }

    private fun initViews() {
        diceFrames = arrayOf(
            findViewById(R.id.dice1), findViewById(R.id.dice2),
            findViewById(R.id.dice3), findViewById(R.id.dice4), findViewById(R.id.dice5)
        )
        diceImages = arrayOf(
            findViewById(R.id.ivDice1), findViewById(R.id.ivDice2),
            findViewById(R.id.ivDice3), findViewById(R.id.ivDice4), findViewById(R.id.ivDice5)
        )
        btnRoll = findViewById(R.id.btnRoll)
        btnRestart = findViewById(R.id.btnRestart)
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

    private fun setupRestartButton() {
        btnRestart.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("Новая игра")
                .setMessage("Точно хотите начать заново?")
                .setPositiveButton("Да") { _, _ -> startNewGame() }
                .setNegativeButton("Отмена", null)
                .show()
        }
    }

    private fun setupDiceClickListeners() {
        for (i in 0..4) {
            diceFrames[i].setOnClickListener {
                if (rollCount == 0 || gameOver || isSaloRound) return@setOnClickListener
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
            if (isSaloRound) { rollSalo(); return@setOnClickListener }
            if (rollCount >= 3) { tvMessage.text = "Выбери, что записать!"; return@setOnClickListener }
            rollDice()
        }
    }

    private fun rollDice() {
        rollCount++
        for (i in 0..4) {
            if (!diceSelected[i] && !diceLocked[i]) animateDiceRoll(i)
        }
        Handler(Looper.getMainLooper()).postDelayed({
            for (i in 0..4) {
                if (!diceSelected[i] && !diceLocked[i]) diceValues[i] = (1..6).random()
            }
            for (i in 0..4) {
                if (diceSelected[i]) { diceLocked[i] = true; diceSelected[i] = false }
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
        }, 600)
    }

    private fun rollSalo() {
        rollCount++
        for (i in 0..4) animateDiceRoll(i)
        Handler(Looper.getMainLooper()).postDelayed({
            for (i in 0..4) diceValues[i] = (1..6).random()
            updateDiceVisuals()
            updateUI()
            val saloResult = calculateSaloDetailed()
            val saloScore = saloResult.first
            val comboName = saloResult.second
            totalScore += saloScore
            AlertDialog.Builder(this)
                .setTitle("САЛО!")
                .setMessage("Выпало: ${diceValues.joinToString(", ")}\n\nКомбинация: $comboName\nОчки за Сало: +$saloScore")
                .setPositiveButton("Отлично!") { _, _ -> showEndGameDialog() }
                .setCancelable(false)
                .show()
            btnRoll.isEnabled = false
            btnRoll.alpha = 0.5f
            gameOver = true
            btnNewGame.visibility = View.VISIBLE
        }, 600)
    }

    private fun showEndGameDialog() {
        AlertDialog.Builder(this)
            .setTitle("Игра окончена!")
            .setMessage("Ваш финальный счёт: $totalScore\n\nХотите сыграть ещё раз?")
            .setPositiveButton("Да, ещё раз!") { _, _ -> startNewGame() }
            .setNegativeButton("Выйти") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun animateDiceRoll(index: Int) {
        val frame = diceFrames[index]
        ObjectAnimator.ofFloat(frame, "rotationX", 0f, 360f).apply {
            duration = 500; interpolator = BounceInterpolator(); start()
        }
        ObjectAnimator.ofFloat(frame, "rotationY", 0f, 360f).apply {
            duration = 500; interpolator = BounceInterpolator(); start()
        }
    }

    private fun updateDiceVisuals() {
        for (i in 0..4) {
            diceImages[i].setImageResource(diceDrawables[diceValues[i]] ?: R.drawable.dice_1)
            when {
                diceLocked[i] -> { diceFrames[i].setBackgroundResource(R.drawable.bg_dice_locked); diceFrames[i].alpha = 0.7f }
                diceSelected[i] -> { diceFrames[i].setBackgroundResource(R.drawable.bg_dice_selected); diceFrames[i].alpha = 1.0f }
                else -> { diceFrames[i].setBackgroundResource(R.drawable.bg_dice); diceFrames[i].alpha = 1.0f }
            }
        }
    }

    private fun updateUI() {
        tvRound.text = "Раунд: $round/13"
        tvRollCount.text = "Бросок: $rollCount/3"
        tvTotalScore.text = "Очки: $totalScore"
        tvPhase.text = when {
            isSaloRound -> "САЛО - Финальный бросок!"
            isPhase1 -> "Фаза 1: Собери цифры"
            else -> "Фаза 2: Собери комбинации"
        }
        renderScoreTable()
    }

    private fun renderScoreTable() {
        numbersContainer.removeAllViews()
        combinationsContainer.removeAllViews()

        val greenColor = ContextCompat.getColor(this, R.color.accent_green)
        val redColor = ContextCompat.getColor(this, R.color.accent_red)
        val goldColor = ContextCompat.getColor(this, R.color.accent_gold)
        val grayColor = ContextCompat.getColor(this, R.color.text_secondary)
        val crossBg = Color.parseColor("#3A1B1B")
        val filledBg = Color.parseColor("#1B3A2F")

        for (num in 1..6) {
            val item = layoutInflater.inflate(R.layout.item_score, numbersContainer, false)
            val card = item.findViewById<CardView>(R.id.cardScore)
            val name = item.findViewById<TextView>(R.id.tvScoreName)
            val value = item.findViewById<TextView>(R.id.tvScoreValue)
            val action = item.findViewById<TextView>(R.id.tvScoreAction)

            name.text = "$num${when(num) { 1->" (единицы)"; 2->" (двойки)"; 3->" (тройки)"; 4->" (четвёрки)"; 5->" (пятёрки)"; else->" (шестёрки)" }}"

            val currentScore = numberScores[num]
            when {
                currentScore != null -> {
                    if (currentScore == -1) {
                        value.text = "✕"
                        value.setTextColor(redColor)
                        card.setCardBackgroundColor(crossBg)
                    } else {
                        value.text = "+$currentScore"
                        value.setTextColor(greenColor)
                        card.setCardBackgroundColor(filledBg)
                    }
                    action.visibility = View.GONE
                }
                rollCount > 0 && !gameOver && isPhase1 -> {
                    val possibleScore = calculateNumberScore(num)
                    value.text = if (possibleScore >= 0) "+$possibleScore" else "-$num"
                    value.setTextColor(goldColor)
                    action.visibility = View.VISIBLE
                    action.text = "ЗАПИСАТЬ"
                    card.setOnClickListener { recordNumberScore(num, possibleScore) }
                }
                else -> {
                    value.text = "-"
                    value.setTextColor(grayColor)
                    action.visibility = View.GONE
                }
            }
            numbersContainer.addView(item)
        }

        val combos = listOf(
            "pair" to "Пара (+10 + сумма)",
            "twoPairs" to "Две пары (+20 + сумма)",
            "threeTwo" to "3+2 (+30 + сумма)",
            "smallStraight" to "Малый стрит 1-5 (+40)",
            "bigStraight" to "Большой стрит 2-6 (+60)",
            "fourOfAKind" to "Каре (+80 + сумма x2 если с 1-го)",
            "poker" to "Покер (+100 + сумма x2 если с 1-го)"
        )

        for ((key, label) in combos) {
            val item = layoutInflater.inflate(R.layout.item_score, combinationsContainer, false)
            val card = item.findViewById<CardView>(R.id.cardScore)
            val name = item.findViewById<TextView>(R.id.tvScoreName)
            val value = item.findViewById<TextView>(R.id.tvScoreValue)
            val action = item.findViewById<TextView>(R.id.tvScoreAction)

            name.text = label
            val currentScore = combinationScores[key]

            when {
                currentScore != null -> {
                    if (currentScore == -1) {
                        value.text = "✕"
                        value.setTextColor(redColor)
                        card.setCardBackgroundColor(crossBg)
                    } else {
                        value.text = "+$currentScore"
                        value.setTextColor(greenColor)
                        card.setCardBackgroundColor(filledBg)
                    }
                    action.visibility = View.GONE
                }
                rollCount > 0 && !gameOver && !isPhase1 -> {
                    val possibleScore = calculateCombinationScore(key)
                    value.text = if (possibleScore != null) "+$possibleScore" else "0"
                    val textColor = if (possibleScore != null && possibleScore > 0) goldColor else grayColor
                    value.setTextColor(textColor)
                    action.visibility = if (possibleScore != null) View.VISIBLE else View.GONE
                    action.text = "ЗАПИСАТЬ"
                    if (possibleScore != null) {
                        card.setOnClickListener { recordCombinationScore(key, possibleScore) }
                    }
                }
                else -> {
                    value.text = "-"
                    value.setTextColor(grayColor)
                    action.visibility = View.GONE
                }
            }
            combinationsContainer.addView(item)
        }
    }

    private fun calculateNumberScore(number: Int): Int {
        val count = diceValues.count { it == number }
        return if (count >= 3) count * number else -number
    }

    private fun calculateCombinationScore(key: String): Int? {
        val sorted = diceValues.toList().sorted()
        val counts = diceValues.groupBy { it }.mapValues { it.value.size }

        return when (key) {
            "pair" -> {
                val pairs = counts.filter { it.value >= 2 }
                if (pairs.isNotEmpty()) {
                    val maxPair = pairs.keys.max()
                    10 + maxPair * 2
                } else null
            }
            "twoPairs" -> {
                val pairKeys = counts.filter { it.value >= 2 }.keys.toList()
                if (pairKeys.size >= 2) {
                    val sortedPairs = pairKeys.sortedDescending()
                    20 + sortedPairs[0] * 2 + sortedPairs[1] * 2
                } else null
            }
            "threeTwo" -> {
                val hasThree = counts.any { it.value >= 3 }
                val hasTwo = counts.any { it.value >= 2 }
                if (hasThree && hasTwo && counts.size == 2) 30 + diceValues.sum() else null
            }
            "smallStraight" -> if (sorted.toSet() == setOf(1, 2, 3, 4, 5)) 40 else null
            "bigStraight" -> if (sorted.toSet() == setOf(2, 3, 4, 5, 6)) 60 else null
            "fourOfAKind" -> {
                val four = counts.entries.find { it.value >= 4 }
                if (four != null) {
                    val base = 80 + four.key * 4
                    if (rollCount == 1) base * 2 else base
                } else null
            }
            "poker" -> {
                if (counts.any { it.value == 5 }) {
                    val v = diceValues[0]
                    val base = 100 + v * 5
                    if (rollCount == 1) base * 2 else base
                } else null
            }
            else -> null
        }
    }

    private fun recordNumberScore(number: Int, score: Int) {
        if (score < 0) {
            numberScores[number] = -1
        } else {
            numberScores[number] = score
            totalScore += score
        }
        nextRound()
    }

    private fun recordCombinationScore(key: String, score: Int) {
        combinationScores[key] = score
        totalScore += score
        nextRound()
    }

    private fun checkAvailableScores() {
        if (rollCount >= 3 && !isSaloRound) {
            val hasAvailable = if (isPhase1) {
                (1..6).any { numberScores[it] == null }
            } else {
                combinationScores.keys.any { combinationScores[it] == null }
            }
            if (!hasAvailable) {
                tvMessage.text = "Нет доступных категорий!"
            } else {
                enableCrossOutMode()
            }
        }
    }

    private fun enableCrossOutMode() {
        val redColor = ContextCompat.getColor(this, R.color.accent_red)
        if (isPhase1) {
            for (num in 1..6) {
                if (numberScores[num] == null) {
                    val index = num - 1
                    if (index < numbersContainer.childCount) {
                        val item = numbersContainer.getChildAt(index)
                        val card = item.findViewById<CardView>(R.id.cardScore)
                        val action = item.findViewById<TextView>(R.id.tvScoreAction)
                        action.text = "ЗАЧЁРКНУТЬ"
                        action.setTextColor(redColor)
                        action.visibility = View.VISIBLE
                        card.setOnClickListener {
                            numberScores[num] = -1
                            nextRound()
                        }
                    }
                }
            }
        } else {
            val comboKeys = listOf("pair", "twoPairs", "threeTwo", "smallStraight", "bigStraight", "fourOfAKind", "poker")
            for (key in comboKeys) {
                if (combinationScores[key] == null) {
                    val index = comboKeys.indexOf(key)
                    if (index < combinationsContainer.childCount) {
                        val item = combinationsContainer.getChildAt(index)
                        val card = item.findViewById<CardView>(R.id.cardScore)
                        val action = item.findViewById<TextView>(R.id.tvScoreAction)
                        action.text = "ЗАЧЁРКНУТЬ"
                        action.setTextColor(redColor)
                        action.visibility = View.VISIBLE
                        card.setOnClickListener {
                            combinationScores[key] = -1
                            nextRound()
                        }
                    }
                }
            }
        }
    }

    private fun nextRound() {
        if (isSaloRound) return

        val phase1Complete = numberScores.values.all { it != null }
        val phase2Complete = combinationScores.values.all { it != null }

        if (phase1Complete && isPhase1) {
            val numbersSum = numberScores.values.filterNotNull().filter { it > 0 }.sum()
            if (numbersSum > 0) {
                totalScore += 100
                Toast.makeText(this, "Бонус +100 за положительный счёт цифр!", Toast.LENGTH_LONG).show()
            }
            isPhase1 = false
            Toast.makeText(this, "Фаза 2: Комбинации!", Toast.LENGTH_SHORT).show()
        }

        if (phase2Complete && !isPhase1) {
            isSaloRound = true
            Toast.makeText(this, "САЛО! Финальный бросок!", Toast.LENGTH_LONG).show()
        }

        round++
        rollCount = 0
        diceLocked.fill(false)
        diceSelected.fill(false)
        diceValues.fill(1)
        updateDiceVisuals()
        btnRoll.isEnabled = true
        btnRoll.alpha = 1.0f
        btnRoll.text = if (isSaloRound) "БРОСИТЬ САЛО!" else "БРОСИТЬ КУБИКИ"
        tvMessage.text = if (isSaloRound) "Финальный бросок! Любая комбинация = очки!" else "Новый раунд! Бросай кубики!"
        updateUI()
    }

    private fun calculateSaloDetailed(): Pair<Int, String> {
        val sorted = diceValues.toList().sorted()
        val counts = diceValues.groupBy { it }.mapValues { it.value.size }
        val diceSum = diceValues.sum()

        return when {
            counts.any { it.value == 5 } -> Pair(100 + diceSum, "ПОКЕР!")
            counts.any { it.value >= 4 } -> Pair(80 + diceSum, "КАРЕ!")
            sorted.toSet() == setOf(2, 3, 4, 5, 6) -> Pair(60 + diceSum, "Большой стрит!")
            sorted.toSet() == setOf(1, 2, 3, 4, 5) -> Pair(40 + diceSum, "Малый стрит!")
            counts.filter { it.value >= 2 }.size >= 2 -> Pair(20 + diceSum, "Две пары!")
            counts.any { it.value >= 3 } && counts.any { it.value >= 2 } -> Pair(30 + diceSum, "3+2 (Фулл хаус!)")
            counts.any { it.value >= 2 } -> Pair(10 + diceSum, "Пара!")
            else -> Pair(diceSum, "Сумма кубиков")
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
        numberScores = mutableMapOf(1 to null, 2 to null, 3 to null, 4 to null, 5 to null, 6 to null)
        combinationScores = mutableMapOf(
            "pair" to null, "twoPairs" to null, "threeTwo" to null,
            "smallStraight" to null, "bigStraight" to null,
            "fourOfAKind" to null, "poker" to null
        )
        btnRoll.isEnabled = true
        btnRoll.alpha = 1.0f
        btnRoll.text = "БРОСИТЬ КУБИКИ"
        btnNewGame.visibility = View.GONE
        updateDiceVisuals()
        updateUI()
        tvMessage.text = "Новая игра! Нажми БРОСИТЬ КУБИКИ!"
    }
}