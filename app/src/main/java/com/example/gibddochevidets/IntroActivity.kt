package com.example.gibddochevidets

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

class IntroActivity : Activity() {

    private lateinit var root: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        createScreen()
    }

    private fun createScreen() {

        // ========================================================
        // ROOT
        // ========================================================

        root = LinearLayout(this)

        root.orientation =
            LinearLayout.VERTICAL

        root.setBackgroundColor(
            Color.rgb(
                245,
                248,
                251
            )
        )

        root.setOnApplyWindowInsetsListener { view, insets ->

            val systemBars =
                insets.getInsets(
                    WindowInsets.Type.systemBars()
                )

            view.setPadding(
                dp(20),
                systemBars.top + dp(20),
                dp(20),
                systemBars.bottom + dp(20)
            )

            insets
        }

        // ========================================================
        // SCROLL
        // ========================================================

        val scrollView =
            ScrollView(this)

        scrollView.isFillViewport =
            true

        scrollView.overScrollMode =
            View.OVER_SCROLL_NEVER

        // ========================================================
        // CONTENT
        // ========================================================

        val content =
            LinearLayout(this)

        content.orientation =
            LinearLayout.VERTICAL

        content.gravity =
            Gravity.CENTER_HORIZONTAL

        // ========================================================
        // TITLE
        // ========================================================

        val title =
            TextView(this)

        title.text =
            "Трезвый дозор - 44"

        title.textSize =
            25f

        title.setTextColor(
            Color.rgb(
                25,
                40,
                55
            )
        )

        title.typeface =
            Typeface.create(
                "sans",
                Typeface.BOLD
            )

        title.gravity =
            Gravity.CENTER

        title.setLineSpacing(
            0f,
            1.05f
        )

        val titleParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        titleParams.bottomMargin =
            dp(24)

        content.addView(
            title,
            titleParams
        )

        // ========================================================
        // INFORMATION CARD
        // ========================================================

        val card =
            LinearLayout(this)

        card.orientation =
            LinearLayout.VERTICAL

        card.setPadding(
            dp(22),
            dp(22),
            dp(22),
            dp(22)
        )

        val cardBackground =
            GradientDrawable()

        cardBackground.setColor(
            Color.WHITE
        )

        cardBackground.cornerRadius =
            dp(24).toFloat()

        cardBackground.setStroke(
            dp(1),
            Color.rgb(
                228,
                234,
                240
            )
        )

        card.background =
            cardBackground

        // ========================================================
        // CARD TITLE
        // ========================================================

        val cardTitle =
            TextView(this)

        cardTitle.text =
            "Уважаемые участники дорожного движения!"

        cardTitle.textSize =
            20f

        cardTitle.setTextColor(
            Color.rgb(
                25,
                40,
                55
            )
        )

        cardTitle.typeface =
            Typeface.create(
                "sans",
                Typeface.BOLD
            )

        val cardTitleParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        cardTitleParams.bottomMargin =
            dp(16)

        card.addView(
            cardTitle,
            cardTitleParams
        )

        // ========================================================
        // INFORMATION TEXT
        // ========================================================

        val informationText =
            TextView(this)

        informationText.text =
            "Госавтоинспекция Костромской области информирует, " +
                    "что приложение создано для предупреждения ДТП " +
                    "с участием нетрезвых водителей.\n\n" +

                    "С его помощью можно анонимно сообщать о водителях " +
                    "с признаками опьянения, которые управляют транспортом.\n\n" +

                    "В сообщении можно указать номер, марку, цвет автомобиля, " +
                    "направление движения, отправить геолокацию, фото или видео.\n\n" +

                    "Вся поступившая информация обрабатывается роботом."

        informationText.textSize =
            16f

        informationText.setTextColor(
            Color.rgb(
                55,
                65,
                75
            )
        )

        informationText.setLineSpacing(
            dp(3).toFloat(),
            1.08f
        )

        informationText.includeFontPadding =
            true

        card.addView(
            informationText,
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        )

        // ========================================================
        // ADD CARD
        // ========================================================

        val cardParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        cardParams.bottomMargin =
            dp(24)

        content.addView(
            card,
            cardParams
        )

        // ========================================================
        // NOTE
        // ========================================================

        val note =
            TextView(this)

        note.text =
            "Сообщения принимаются анонимно"

        note.textSize =
            14f

        note.setTextColor(
            Color.rgb(
                105,
                120,
                135
            )
        )

        note.gravity =
            Gravity.CENTER

        val noteParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )

        noteParams.bottomMargin =
            dp(20)

        content.addView(
            note,
            noteParams
        )

        // ========================================================
        // START BUTTON
        // ========================================================

        val startButton =
            TextView(this)

        startButton.text =
            "Начать"

        startButton.textSize =
            17f

        startButton.setTextColor(
            Color.WHITE
        )

        startButton.typeface =
            Typeface.create(
                "sans",
                Typeface.BOLD
            )

        startButton.gravity =
            Gravity.CENTER

        startButton.isClickable =
            true

        startButton.isFocusable =
            true

        startButton.setPadding(
            dp(20),
            0,
            dp(20),
            0
        )

        // ========================================================
        // BUTTON BACKGROUND
        // ========================================================

        val buttonBackground =
            GradientDrawable()

        buttonBackground.setColor(
            Color.rgb(
                35,
                91,
                170
            )
        )

        buttonBackground.cornerRadius =
            dp(20).toFloat()

        startButton.background =
            buttonBackground

        // ========================================================
        // BUTTON CLICK
        // ========================================================

        startButton.setOnClickListener {

            val intent =
                Intent(
                    this@IntroActivity,
                    ChatActivity::class.java
                )

            startActivity(intent)

            finish()
        }

        val buttonParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(58)
            )

        content.addView(
            startButton,
            buttonParams
        )

        // ========================================================
        // SCROLL CONTENT
        // ========================================================

        val scrollParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

        scrollView.addView(
            content,
            scrollParams
        )

        // ========================================================
        // ROOT
        // ========================================================

        val rootParams =
            LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )

        root.addView(
            scrollView,
            rootParams
        )

        setContentView(root)

        root.requestApplyInsets()
    }

    // ============================================================
    // DP
    // ============================================================

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }
}