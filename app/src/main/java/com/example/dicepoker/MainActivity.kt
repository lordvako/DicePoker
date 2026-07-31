package lordvako.appname

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
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

    data class ScoreCell(
        val id: String,
        val name: String,
        val description: String,
        val isUpper: Boolean,
        var score: Int? = null
    )

    private val scoreCells = mutableListOf<ScoreCell>()
    private val cellViews = mutableMapOf<String, View>()
    private val diceViews = mutableListOf<DiceView>()
    private val diceValues = IntArray(5) { 1 }
    private val savedDice = BooleanArray(5) { false }

    private var rollCount = 0
    private var currentRound = 1
    private val maxRounds = 15
    private val maxRolls = 3
    private var isGameOver = false

    private lateinit var tvRound: TextView
    private lateinit var tvRoll: TextView
    private lateinit var tvTotalScore: TextView
    private lateinit var tvUpperScore: TextView
    private lateinit var tvBonus: TextView
    private lateinit var tvHint: TextView
    private lateinit var btnRoll: Button
    private lateinit var diceContainer: LinearLayout
    private lateinit var scoresContainer: LinearLayout
    private lateinit var btnHelp: ImageButton
    private lateinit var btnRestart: ImageButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initScoreCells()
        initViews()
        setupDiceClicks()
        setupButtons()
        buildScoresTable()
        updateUI()
    }

    private fun initScoreCells() {
        scoreCells.clear()
        scoreCells.add(ScoreCell("ones", "Единицы", "Сумма всех единиц", true))
        scoreCells.add(ScoreCell("twos", "Двойки", "Сумма всех двоек", true))
        scoreCells.add(ScoreCell("threes", "Тройки", "Сумма всех троек", true))
        scoreCells.add(ScoreCell("fours", "Четвёрки", "Сумма всех четвёрок", true))
        scoreCells.add(ScoreCell("fives", "Пятёрки", "Сумма всех пятёрок", true))
        scoreCells.add(ScoreCell("sixes", "Шестёрки", "Сумма всех шестёрок", true))
        scoreCells.add(ScoreCell("pair", "Одна пара", "Сумма пары", false))
        scoreCells.add(ScoreCell("two_pairs", "Две пары", "Сумма двух пар", false))
        scoreCells.add(ScoreCell("three_kind", "Тройка", "Сумма трёх одинаковых", false))
        scoreCells.add(ScoreCell("four_kind", "Каре", "Сумма четырёх одинаковых", false))
        scoreCells.add(ScoreCell("small_straight", "Малый стрит", "1-2-3-4-5 = 15 очков", false))
        scoreCells.add(ScoreCell("large_straight", "Большой стрит", "2-3-4-5-6 = 20 очков", false))
        scoreCells.add(ScoreCell("full_house", "Фулл хаус", "3+2 = сумма всех кубиков", false))
        scoreCells.add(ScoreCell("chance", "Шанс", "Сумма всех кубиков", false))
        scoreCells.add(ScoreCell("yatzy", "Покер (Ятцы)", "5 одинаковых = 50 очков", false))
    }

    private fun initViews() {
        tvRound = findViewById(R.id.tvRound)
        tvRoll = findViewById(R.id.tvRoll)
        tvTotalScore = findViewById(R.id.tvTotalScore)
        tvUpperScore = findViewById(R.id.tvUpperScore)
        tvBonus = findViewById(R.id.tvBonus)
        tvHint = findViewById(R.id.tvHint)
        btnRoll = findViewById(R.id.btnRoll)
        diceContainer = findViewById(R.id.diceContainer)
        scoresContainer = findViewById(R.id.scoresContainer)
        btnHelp = findViewById(R.id.btnHelp)
        btnRestart = findViewById(R.id.btnRestart)

        diceContainer.removeAllViews()
        for (i in 0 until 5) {
            val dice = DiceView(this).apply {
                layoutParams = LinearLayout.LayoutParams(100, 100).apply {
                    marginEnd = 14
                }
                diceValue = diceValues[i]
            }
            diceViews.add(dice)
            diceContainer.addView(dice)
        }
    }

    private fun buildScoresTable() {
        scoresContainer.removeAllViews()
        cellViews.clear()

        // Upper section header
        val upperHeader = TextView(this).apply {
            text = "ВЕРХНЯЯ СЕКЦИЯ"
            textSize = 14f
            setTextColor(Color.parseColor("#00E5FF"))
            setPadding(16, 16, 16, 8)
        }
        scoresContainer.addView(upperHeader)

        for (cell in scoreCells.filter { it.isUpper }) {
            addScoreRow(cell)
        }

        // Divider
        val divider = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 2
            ).apply { setMargins(16, 8, 16, 8) }
            setBackgroundColor(Color.parseColor("#2A2A5A"))
        }
        scoresContainer.addView(divider)

        // Lower section header
        val lowerHeader = TextView(this).apply {
            text = "НИЖНЯЯ СЕКЦИЯ"
            textSize = 14f
            setTextColor(Color.parseColor("#00E5FF"))
            setPadding(16, 8, 16, 8)
        }
        scoresContainer.addView(lowerHeader)

        for (cell in scoreCells.filter { !it.isUpper }) {
            addScoreRow(cell)
        }
    }

    private fun addScoreRow(cell: ScoreCell) {
        val row = layoutInflater.inflate(R.layout.item_score, scoresContainer, false)
        val title = row.findViewById<TextView>(R.id.tvScoreTitle)
        val desc = row.findViewById<TextView>(R.id.tvScoreDesc)
        val value = row.findViewById<TextView>(R.id.tvScoreValue)
        val action = row.findViewById<Button>(R.id.btnScoreAction)
        val card = row.findViewById<CardView>(R.id.cardScore)

        title.text = cell.name
        desc.text = cell.description
        value.text = ""
        action.visibility = View.GONE

        cellViews[cell.id] = row
        scoresContainer.addView(row)
    }

    private fun setupDiceClicks() {
        for (i in 0 until 5) {
            diceViews[i].setOnClickListener {
                if (rollCount == 0 || isGameOver) return@setOnClickListener
                savedDice[i] = !savedDice[i]
                diceViews[i].isDiceSelected = savedDice[i]
                diceViews[i].animateSelect()
            }
        }
    }

    private fun setupButtons() {
        btnRoll.setOnClickListener {
            if (isGameOver) return@setOnClickListener
            if (rollCount >= maxRolls) {
                // Should not happen, but just in case
                return@setOnClickListener
            }
            rollDice()
        }
        btnHelp.setOnClickListener { showHelp() }
        btnRestart.setOnClickListener { confirmRestart() }
    }

    private fun rollDice() {
        for (i in 0 until 5) {
            if (!savedDice[i]) {
                diceValues[i] = (1..6).random()
            }
        }
        rollCount++

        for (dice in diceViews) {
            dice.animateRoll()
        }

        // Delay update to match animation
        diceContainer.postDelayed({
            updateDiceUI()
            updateScoresTable()

            if (rollCount >= maxRolls) {
                tvHint.text = "Последний бросок! Выбери ячейку для записи очков."
                btnRoll.isEnabled = false
                btnRoll.alpha = 0.5f
            } else {
                tvHint.text = "Бросок ${rollCount}/${maxRolls}. Тапни кубик, чтобы сохранить."
            }
            updateUI()
        }, 300)
    }

    private fun updateDiceUI() {
        for (i in 0 until 5) {
            diceViews[i].diceValue = diceValues[i]
            diceViews[i].isDiceSelected = savedDice[i]
            diceViews[i].isDiceLocked = rollCount >= maxRolls
        }
    }

    private fun calculatePossibleScore(cellId: String): Int? {
        val sorted = diceValues.sorted()
        val counts = diceValues.groupBy { it }.mapValues { it.value.size }

        return when (cellId) {
            "ones" -> diceValues.count { it == 1 } * 1
            "twos" -> diceValues.count { it == 2 } * 2
            "threes" -> diceValues.count { it == 3 } * 3
            "fours" -> diceValues.count { it == 4 } * 4
            "fives" -> diceValues.count { it == 5 } * 5
            "sixes" -> diceValues.count { it == 6 } * 6
            "pair" -> {
                counts.filter { it.value >= 2 }.keys.maxOrNull()?.let { it * 2 }
            }
            "two_pairs" -> {
                val pairs = counts.filter { it.value >= 2 }.keys.sortedDescending()
                if (pairs.size >= 2) pairs[0] * 2 + pairs[1] * 2 else null
            }
            "three_kind" -> {
                counts.filter { it.value >= 3 }.keys.maxOrNull()?.let { it * 3 }
            }
            "four_kind" -> {
                counts.filter { it.value >= 4 }.keys.maxOrNull()?.let { it * 4 }
            }
            "small_straight" -> {
                if (sorted.toSet().containsAll(setOf(1, 2, 3, 4, 5))) 15 else null
            }
            "large_straight" -> {
                if (sorted.toSet().containsAll(setOf(2, 3, 4, 5, 6))) 20 else null
            }
            "full_house" -> {
                val hasThree = counts.any { it.value >= 3 }
                val hasTwo = counts.any { it.value >= 2 }
                val isYatzy = counts.any { it.value == 5 }
                if ((hasThree && hasTwo && !isYatzy) || isYatzy) diceValues.sum() else null
            }
            "chance" -> diceValues.sum()
            "yatzy" -> {
                if (counts.any { it.value == 5 }) 50 else null
            }
            else -> null
        }
    }

    private fun updateScoresTable() {
        for (cell in scoreCells) {
            val row = cellViews[cell.id] ?: continue
            val title = row.findViewById<TextView>(R.id.tvScoreTitle)
            val desc = row.findViewById<TextView>(R.id.tvScoreDesc)
            val value = row.findViewById<TextView>(R.id.tvScoreValue)
            val action = row.findViewById<Button>(R.id.btnScoreAction)
            val card = row.findViewById<CardView>(R.id.cardScore)

            title.text = cell.name
            desc.text = cell.description

            when {
                cell.score != null -> {
                    val s = cell.score!!
                    value.text = if (s > 0) "+$s" else "✕"
                    value.setTextColor(if (s > 0) Color.parseColor("#FFD700") else Color.parseColor("#FF6B6B"))
                    card.setCardBackgroundColor(if (s > 0) Color.parseColor("#1B3A2F") else Color.parseColor("#3A1B1B"))
                    action.visibility = View.GONE
                }
                rollCount > 0 && !isGameOver -> {
                    val possible = calculatePossibleScore(cell.id)
                    if (possible != null && possible > 0) {
                        value.text = "+$possible"
                        value.setTextColor(Color.parseColor("#FFD700"))
                        action.text = "ЗАПИСАТЬ"
                        action.setTextColor(Color.parseColor("#00C853"))
                        action.setOnClickListener {
                            recordScore(cell.id, possible)
                        }
                    } else {
                        value.text = "0"
                        value.setTextColor(Color.parseColor("#888888"))
                        action.text = "ЗАЧЁРКНУТЬ"
                        action.setTextColor(Color.parseColor("#FF6B6B"))
                        action.setOnClickListener {
                            recordScore(cell.id, 0)
                        }
                    }
                    action.visibility = View.VISIBLE
                    card.setCardBackgroundColor(Color.parseColor("#1A1A3E"))
                }
                else -> {
                    value.text = ""
                    action.visibility = View.GONE
                    card.setCardBackgroundColor(Color.parseColor("#1A1A3E"))
                }
            }
        }
    }

    private fun recordScore(cellId: String, score: Int) {
        val cell = scoreCells.find { it.id == cellId } ?: return
        cell.score = score

        // Animate the card
        val row = cellViews[cellId]
        row?.let {
            val colorAnim = ValueAnimator.ofArgb(
                Color.parseColor("#1A1A3E"),
                if (score > 0) Color.parseColor("#1B3A2F") else Color.parseColor("#3A1B1B")
            )
            colorAnim.duration = 400
            colorAnim.addUpdateListener { animator ->
                it.findViewById<CardView>(R.id.cardScore).setCardBackgroundColor(animator.animatedValue as Int)
            }
            colorAnim.start()
        }

        updateScoresTable()

        currentRound++
        rollCount = 0
        savedDice.fill(false)
        btnRoll.isEnabled = true
        btnRoll.alpha = 1f

        if (currentRound > maxRounds) {
            isGameOver = true
            showEndGame()
        } else {
            btnRoll.text = "БРОСИТЬ КУБИКИ"
            tvHint.text = "Раунд $currentRound. Нажми БРОСИТЬ КУБИКИ"
            updateDiceUI()
            updateUI()
        }
    }

    private fun getUpperScore(): Int {
        return scoreCells.filter { it.isUpper && it.score != null }.sumOf { it.score!! }
    }

    private fun getLowerScore(): Int {
        return scoreCells.filter { !it.isUpper && it.score != null }.sumOf { it.score!! }
    }

    private fun getBonus(): Int {
        return if (getUpperScore() >= 63) 50 else 0
    }

    private fun getTotalScore(): Int {
        return getUpperScore() + getLowerScore() + getBonus()
    }

    private fun updateUI() {
        tvRound.text = "Раунд: ${currentRound}/${maxRounds}"
        tvRoll.text = "Бросок: ${rollCount}/${maxRolls}"
        tvTotalScore.text = "Итого: ${getTotalScore()}"
        tvUpperScore.text = "Верх: ${getUpperScore()}/63"
        tvBonus.text = "Бонус: ${getBonus()}"
    }

    private fun showEndGame() {
        val upper = getUpperScore()
        val lower = getLowerScore()
        val bonus = getBonus()
        val total = getTotalScore()

        val message = buildString {
            appendLine("Верхняя секция: $upper")
            appendLine("Нижняя секция: $lower")
            appendLine("Бонус: $bonus")
            appendLine("")
            appendLine("ИТОГОВЫЙ СЧЁТ: $total")
        }

        AlertDialog.Builder(this)
            .setTitle("🎉 Игра окончена!")
            .setMessage(message)
            .setPositiveButton("СЫГРАТЬ ЕЩЁ") { _, _ -> restartGame() }
            .setNegativeButton("ВЫЙТИ") { _, _ -> finish() }
            .setCancelable(false)
            .show()
    }

    private fun confirmRestart() {
        AlertDialog.Builder(this)
            .setTitle("Начать заново?")
            .setMessage("Текущий прогресс будет потерян.")
            .setPositiveButton("ДА") { _, _ -> restartGame() }
            .setNegativeButton("ОТМЕНА", null)
            .show()
    }

    private fun restartGame() {
        currentRound = 1
        rollCount = 0
        isGameOver = false
        savedDice.fill(false)
        diceValues.fill(1)
        scoreCells.forEach { it.score = null }
        btnRoll.isEnabled = true
        btnRoll.alpha = 1f
        btnRoll.text = "БРОСИТЬ КУБИКИ"
        updateDiceUI()
        buildScoresTable()
        updateUI()
        tvHint.text = "Раунд 1. Нажми БРОСИТЬ КУБИКИ"
    }

    private fun showHelp() {
        val rules = """
            🎲 ПРАВИЛА ИГРЫ "ПОКЕР НА КОСТЯХ" (YATZY)

            Цель: набрать максимум очков за 15 раундов.

            В каждом раунде:
            1. Брось 5 кубиков (до 3 бросков)
            2. Тапни кубик, чтобы сохранить его
            3. Перебрасывай остальные
            4. Запиши результат в ЛЮБУЮ свободную ячейку

            ВЕРХНЯЯ СЕКЦИЯ (1-6):
            Сумма кубиков с нужной цифрой.
            Если сумма ≥ 63 → БОНУС +50!

            НИЖНЯЯ СЕКЦИЯ:
            • Одна пара — сумма пары
            • Две пары — сумма двух пар
            • Тройка — сумма трёх одинаковых
            • Каре — сумма четырёх одинаковых
            • Малый стрит (1-2-3-4-5) — 15 очков
            • Большой стрит (2-3-4-5-6) — 20 очков
            • Фулл хаус (3+2) — сумма всех кубиков
            • Шанс — сумма всех кубиков
            • Покер (5 одинаковых) — 50 очков

            Не подходит? Зачеркни (0 очков).
        """.trimIndent()

        AlertDialog.Builder(this)
            .setTitle("📖 Правила игры")
            .setMessage(rules)
            .setPositiveButton("ПОНЯТНО", null)
            .show()
    }
}
