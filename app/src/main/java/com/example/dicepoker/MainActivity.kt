package com.example.dicepoker

import android.animation.ObjectAnimator
import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.animation.doOnEnd

class MainActivity : AppCompatActivity() {

    private val diceValues = IntArray(5) { 1 }
    private val savedDice = BooleanArray(5) { false }
    private var rollCount = 0
    private var currentRound = 1
    private var totalScore = 0
    private var phase = 1
    private var isRoundEnded = false

    private val numberScores = mutableMapOf<Int, Int?>()
    private val numberClosed = mutableMapOf<Int, Boolean>()
    private val comboScores = mutableMapOf<String, Int?>()
    private val comboClosed = mutableMapOf<String, Boolean>()

    private val PHASE1_ROUNDS = 3
    private val PHASE2_COMBOS = listOf("Пара", "Две пары", "3+2", "Малый стрит", "Большой стрит", "Каре", "Покер")
    private val MAX_ROLLS = 3

    private lateinit var tvRound: TextView
    private lateinit var tvRoll: TextView
    private lateinit var tvScore: TextView
    private lateinit var tvPhase: TextView
    private lateinit var tvHint: TextView
    private lateinit var btnRoll: Button
    private lateinit var diceContainer: LinearLayout
    private lateinit var numbersContainer: LinearLayout
    private lateinit var combosContainer: LinearLayout

    private val diceViews = mutableListOf<TextView>()
    private val numberRows = mutableMapOf<Int, View>()
    private val comboRows = mutableMapOf<String, View>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initViews()
        initGameData()
        setupDiceClicks()
        setupButtons()
        updateUI()
    }

    private fun initViews() {
        tvRound = findViewById(R.id.tvRound)
        tvRoll = findViewById(R.id.tvRoll)
        tvScore = findViewById(R.id.tvScore)
        tvPhase = findViewById(R.id.tvPhase)
        tvHint = findViewById(R.id.tvHint)
        btnRoll = findViewById(R.id.btnRoll)
        diceContainer = findViewById(R.id.diceContainer)
        numbersContainer = findViewById(R.id.numbersContainer)
        combosContainer = findViewById(R.id.combosContainer)

        diceContainer.removeAllViews()
        for (i in 0 until 5) {
            val tv = TextView(this).apply {
                text = "1"
                textSize = 32f
                setTextColor(Color.WHITE)
                gravity = android.view.Gravity.CENTER
                setBackgroundResource(R.drawable.bg_dice_modern)
                layoutParams = LinearLayout.LayoutParams(100, 100).apply { marginEnd = 16 }
                tag = i
            }
            diceViews.add(tv)
            diceContainer.addView(tv)
        }
    }

    private fun initGameData() {
        for (i in 1..6) {
            numberScores[i] = null
            numberClosed[i] = false
        }
        PHASE2_COMBOS.forEach {
            comboScores[it] = null
            comboClosed[it] = false
        }
    }

    private fun setupDiceClicks() {
        for (i in 0 until 5) {
            diceViews[i].setOnClickListener {
                if (rollCount == 0 || rollCount >= MAX_ROLLS || isRoundEnded) return@setOnClickListener
                savedDice[i] = !savedDice[i]
                updateDiceAppearance(i)
            }
        }
    }

    private fun setupButtons() {
        btnRoll.setOnClickListener {
            if (isRoundEnded) {
                startNextRound()
                return@setOnClickListener
            }
            if (rollCount >= MAX_ROLLS) return@setOnClickListener
            rollDice()
        }
        findViewById<ImageButton>(R.id.btnHelp).setOnClickListener { showHelp() }
        findViewById<ImageButton>(R.id.btnRestart).setOnClickListener { confirmRestart() }
    }

    private fun rollDice() {
        for (i in 0 until 5) {
            if (!savedDice[i]) {
                diceValues[i] = (1..6).random()
            }
        }
        rollCount++
        animateDice()
        updateDiceUI()

        if (rollCount >= MAX_ROLLS) {
            endRound()
        } else {
            tvHint.text = "Бросок ${rollCount}/${MAX_ROLLS}. Выбери кубики для сохранения (тап)."
            updateNumbersTable(showActions = false)
        }
    }

    private fun animateDice() {
        for (v in diceViews) {
            if (v.tag == null) continue
            val idx = v.tag as Int
            if (savedDice[idx]) continue
            ObjectAnimator.ofFloat(v, "rotationY", 0f, 360f).apply {
                duration = 400
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
            ObjectAnimator.ofFloat(v, "scaleX", 1f, 1.3f, 1f).apply {
                duration = 300
                start()
            }
            ObjectAnimator.ofFloat(v, "scaleY", 1f, 1.3f, 1f).apply {
                duration = 300
                start()
            }
        }
    }

    private fun updateDiceUI() {
        for (i in 0 until 5) {
            diceViews[i].text = diceValues[i].toString()
            updateDiceAppearance(i)
        }
        tvRoll.text = "Бросок: ${rollCount}/${MAX_ROLLS}"
    }

    private fun updateDiceAppearance(i: Int) {
        val tv = diceViews[i]
        when {
            savedDice[i] -> tv.setBackgroundResource(R.drawable.bg_dice_selected_modern)
            rollCount >= MAX_ROLLS || isRoundEnded -> tv.setBackgroundResource(R.drawable.bg_dice_locked_modern)
            else -> tv.setBackgroundResource(R.drawable.bg_dice_modern)
        }
    }

    private fun endRound() {
        isRoundEnded = true
        tvHint.text = "Раунд завершён! Выбери, что записать, или зачеркни."
        btnRoll.text = "СЛЕДУЮЩИЙ РАУНД"
        updateNumbersTable(showActions = true)
        updateCombosTable(showActions = true)
    }

    private fun startNextRound() {
        // Сохраняем бонусы для уже закрытых цифр
        if (phase == 1) {
            for (num in 1..6) {
                if (numberClosed[num] == true) {
                    val bonus = diceValues.count { it == num } * num
                    if (bonus > 0) {
                        numberScores[num] = (numberScores[num] ?: 0) + bonus
                        totalScore += bonus
                    }
                }
            }
        }

        currentRound++
        rollCount = 0
        isRoundEnded = false
        savedDice.fill(false)
        diceValues.fill(1)
        updateDiceUI()

        // Проверка перехода фаз
        if (phase == 1 && currentRound > PHASE1_ROUNDS) {
            // Автозачёркивание оставшихся цифр
            for (num in 1..6) {
                if (numberClosed[num] != true) {
                    numberScores[num] = 0
                    numberClosed[num] = true
                }
            }
            phase = 2
            currentRound = 1
            Toast.makeText(this, "Фаза 2: Комбинации!", Toast.LENGTH_LONG).show()
        } else if (phase == 2 && currentRound > PHASE2_COMBOS.size) {
            phase = 3
            currentRound = 1
            Toast.makeText(this, "Финал: САЛО!", Toast.LENGTH_LONG).show()
        } else if (phase == 3) {
            showEndGameDialog()
            return
        }

        btnRoll.text = "БРОСИТЬ КУБИКИ"
        updateUI()
    }

    private fun updateUI() {
        val totalRounds = PHASE1_ROUNDS + PHASE2_COMBOS.size + 1
        val displayRound = when (phase) {
            1 -> currentRound
            2 -> PHASE1_ROUNDS + currentRound
            else -> PHASE1_ROUNDS + PHASE2_COMBOS.size + 1
        }
        tvRound.text = "Раунд: ${displayRound}/${totalRounds}"
        tvRoll.text = "Бросок: ${rollCount}/${MAX_ROLLS}"
        tvScore.text = "Очки: ${totalScore}"
        tvPhase.text = when (phase) {
            1 -> "Фаза 1: Собери цифры (ход ${currentRound}/${PHASE1_ROUNDS})"
            2 -> "Фаза 2: Комбинации (${currentRound}/${PHASE2_COMBOS.size})"
            else -> "Финал: САЛО!"
        }
        tvHint.text = when {
            phase == 3 -> "Брось кубики 1 раз! Любая комбинация = очки"
            rollCount == 0 -> "Нажми БРОСИТЬ КУБИКИ"
            else -> "Бросок ${rollCount}/${MAX_ROLLS}. Выбери кубики для сохранения"
        }

        updateNumbersTable(showActions = isRoundEnded && phase == 1)
        combosContainer.visibility = if (phase >= 2) View.VISIBLE else View.GONE
        if (phase >= 2) updateCombosTable(showActions = isRoundEnded && phase == 2)
    }

    private fun updateNumbersTable(showActions: Boolean) {
        numbersContainer.removeAllViews()
        for (num in 1..6) {
            val row = layoutInflater.inflate(R.layout.item_score, numbersContainer, false)
            val title = row.findViewById<TextView>(R.id.tvScoreTitle)
            val value = row.findViewById<TextView>(R.id.tvScoreValue)
            val action = row.findViewById<Button>(R.id.btnScoreAction)
            val card = row.findViewById<CardView>(R.id.cardScore)

            title.text = "${num} (${getNumberName(num)})"

            when {
                numberClosed[num] == true -> {
                    val score = numberScores[num] ?: 0
                    if (score > 0) {
                        value.text = "+${score}"
                        value.setTextColor(Color.parseColor("#FFD700"))
                        card.setCardBackgroundColor(Color.parseColor("#1B3A2F"))
                    } else {
                        value.text = "✕"
                        value.setTextColor(Color.parseColor("#FF6B6B"))
                        card.setCardBackgroundColor(Color.parseColor("#3A1B1B"))
                    }
                    action.visibility = View.GONE
                }
                showActions -> {
                    val count = diceValues.count { it == num }
                    val possibleScore = if (count >= 3) count * num else null
                    if (possibleScore != null) {
                        value.text = "+${possibleScore}"
                        value.setTextColor(Color.parseColor("#FFD700"))
                        action.text = "ЗАПИСАТЬ"
                        action.setOnClickListener {
                            numberScores[num] = possibleScore
                            numberClosed[num] = true
                            totalScore += possibleScore
                            updateUI()
                        }
                    } else {
                        value.text = "—"
                        value.setTextColor(Color.parseColor("#888888"))
                        action.text = "ЗАЧЁРКНУТЬ"
                        action.setOnClickListener {
                            numberScores[num] = 0
                            numberClosed[num] = true
                            updateUI()
                        }
                    }
                    action.visibility = View.VISIBLE
                }
                else -> {
                    value.text = ""
                    action.visibility = View.GONE
                }
            }
            numbersContainer.addView(row)
        }
    }

    private fun updateCombosTable(showActions: Boolean) {
        combosContainer.removeAllViews()
        for (combo in PHASE2_COMBOS) {
            val row = layoutInflater.inflate(R.layout.item_score, combosContainer, false)
            val title = row.findViewById<TextView>(R.id.tvScoreTitle)
            val value = row.findViewById<TextView>(R.id.tvScoreValue)
            val action = row.findViewById<Button>(R.id.btnScoreAction)
            val card = row.findViewById<CardView>(R.id.cardScore)

            title.text = combo

            when {
                comboClosed[combo] == true -> {
                    val score = comboScores[combo] ?: 0
                    if (score > 0) {
                        value.text = "+${score}"
                        value.setTextColor(Color.parseColor("#FFD700"))
                        card.setCardBackgroundColor(Color.parseColor("#1B3A2F"))
                    } else {
                        value.text = "✕"
                        value.setTextColor(Color.parseColor("#FF6B6B"))
                        card.setCardBackgroundColor(Color.parseColor("#3A1B1B"))
                    }
                    action.visibility = View.GONE
                }
                showActions -> {
                    val possibleScore = calculateComboScore(combo)
                    if (possibleScore != null && possibleScore > 0) {
                        value.text = "+${possibleScore}"
                        value.setTextColor(Color.parseColor("#FFD700"))
                        action.text = "ЗАПИСАТЬ"
                        action.setOnClickListener {
                            comboScores[combo] = possibleScore
                            comboClosed[combo] = true
                            totalScore += possibleScore
                            updateUI()
                        }
                    } else {
                        value.text = "—"
                        value.setTextColor(Color.parseColor("#888888"))
                        action.text = "ЗАЧЁРКНУТЬ"
                        action.setOnClickListener {
                            comboScores[combo] = 0
                            comboClosed[combo] = true
                            updateUI()
                        }
                    }
                    action.visibility = View.VISIBLE
                }
                else -> {
                    value.text = ""
                    action.visibility = View.GONE
                }
            }
            combosContainer.addView(row)
        }
    }

    private fun calculateComboScore(combo: String): Int? {
        val sorted = diceValues.sorted()
        val counts = diceValues.groupBy { it }.mapValues { it.value.size }
        return when (combo) {
            "Пара" -> {
                val pair = counts.filter { it.value >= 2 }.keys.maxOrNull()
                pair?.let { it * 2 }
            }
            "Две пары" -> {
                val pairs = counts.filter { it.value >= 2 }.keys.sortedDescending()
                if (pairs.size >= 2) pairs[0] * 2 + pairs[1] * 2 else null
            }
            "3+2" -> {
                val three = counts.filter { it.value >= 3 }.keys.maxOrNull()
                val two = counts.filter { it.value >= 2 && it.key != three }.keys.maxOrNull()
                if (three != null && two != null) three * 3 + two * 2 else null
            }
            "Малый стрит" -> {
                val unique = sorted.distinct()
                if (unique.containsAll(listOf(1,2,3,4,5)) || unique.containsAll(listOf(2,3,4,5,6))) 15 else null
            }
            "Большой стрит" -> {
                if (sorted == listOf(1,2,3,4,5) || sorted == listOf(2,3,4,5,6)) 25 else null
            }
            "Каре" -> {
                val four = counts.filter { it.value >= 4 }.keys.maxOrNull()
                four?.let { it * 4 }
            }
            "Покер" -> {
                if (counts.any { it.value == 5 }) 50 else null
            }
            else -> null
        }
    }

    private fun getNumberName(n: Int): String = when (n) {
        1 -> "единицы"
        2 -> "двойки"
        3 -> "тройки"
        4 -> "четвёрки"
        5 -> "пятёрки"
        6 -> "шестёрки"
        else -> ""
    }

    private fun showEndGameDialog() {
        AlertDialog.Builder(this)
            .setTitle("Игра окончена!")
            .setMessage("Ваш финальный счёт: ${totalScore}\n\nХотите сыграть ещё раз?")
            .setPositiveButton("ДА") { _, _ -> restartGame() }
            .setNegativeButton("ВЫХОД") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun confirmRestart() {
        AlertDialog.Builder(this)
            .setTitle("Начать заново?")
            .setMessage("Текущий прогресс будет сброшен.")
            .setPositiveButton("ДА") { _, _ -> restartGame() }
            .setNegativeButton("ОТМЕНА", null)
            .show()
    }

    private fun restartGame() {
        currentRound = 1
        totalScore = 0
        phase = 1
        rollCount = 0
        isRoundEnded = false
        savedDice.fill(false)
        diceValues.fill(1)
        initGameData()
        btnRoll.text = "БРОСИТЬ КУБИКИ"
        updateDiceUI()
        updateUI()
    }

    private fun showHelp() {
        AlertDialog.Builder(this)
            .setTitle("Правила игры Покер Кубик")
            .setMessage(
                "ФАЗА 1 — ЦИФРЫ (3 хода):\n" +
                "• У тебя 3 хода. В каждом — до 3 бросков.\n" +
                "• Выбирай кубики тапом, остальные перебрасывай.\n" +
                "• После 3-го броска запиши собранную цифру (3+ кубиков) или зачеркни.\n" +
                "• Зачёркивание = крестик, 0 очков (без минуса!).\n" +
                "• Если закрытая цифра выпадает снова — бонус +номинал!\n\n" +
                "ФАЗА 2 — КОМБИНАЦИИ (7 ходов):\n" +
                "• Пара, Две пары, 3+2, Малый стрит, Большой стрит, Каре, Покер.\n" +
                "• Не собрал — зачеркни (без штрафа).\n\n" +
                "ФИНАЛ — САЛО:\n" +
                "• 1 бросок, любая комбинация = очки."
            )
            .setPositiveButton("ПОНЯТНО", null)
            .show()
    }
}
