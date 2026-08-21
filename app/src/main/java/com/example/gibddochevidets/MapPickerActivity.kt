package com.example.gibddochevidets

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.TextView

class MapPickerActivity : Activity() {

    companion object {

        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"

        private const val DEFAULT_LATITUDE = 57.7679
        private const val DEFAULT_LONGITUDE = 40.9269
    }

    private lateinit var webView: WebView
    private lateinit var selectButton: TextView
    private lateinit var coordinatesText: TextView

    private var selectedLatitude =
        DEFAULT_LATITUDE

    private var selectedLongitude =
        DEFAULT_LONGITUDE

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(
            savedInstanceState
        )

        createScreen()

        setupWebView()
    }

    private fun createScreen() {

        val root =
            android.widget.FrameLayout(this)

        root.setBackgroundColor(
            Color.WHITE
        )

        webView =
            WebView(this)

        val mapParams =
            android.widget.FrameLayout.LayoutParams(
                -1,
                -1
            )

        root.addView(
            webView,
            mapParams
        )

        /*
         * Верхняя панель.
         */
        val topBar =
            android.widget.LinearLayout(this)

        topBar.orientation =
            android.widget.LinearLayout.VERTICAL

        topBar.setBackgroundColor(
            Color.WHITE
        )

        topBar.elevation =
            8f

        val topParams =
            android.widget.FrameLayout.LayoutParams(
                -1,
                dp(105)
            )

        topParams.gravity =
            android.view.Gravity.TOP

        root.addView(
            topBar,
            topParams
        )

        /*
         * Строка заголовка.
         */
        val titleRow =
            android.widget.LinearLayout(this)

        titleRow.orientation =
            android.widget.LinearLayout.HORIZONTAL

        titleRow.gravity =
            android.view.Gravity.CENTER_VERTICAL

        val back =
            TextView(this)

        back.text = "‹"
        back.textSize = 38f
        back.gravity =
            android.view.Gravity.CENTER

        back.setTextColor(
            Color.BLACK
        )

        back.setOnClickListener {
            finish()
        }

        titleRow.addView(
            back,
            android.widget.LinearLayout.LayoutParams(
                dp(52),
                dp(52)
            )
        )

        val title =
            TextView(this)

        title.text =
            "Выберите точку"

        title.textSize =
            18f

        title.setTextColor(
            Color.BLACK
        )

        title.setTypeface(
            null,
            android.graphics.Typeface.BOLD
        )

        title.gravity =
            android.view.Gravity.CENTER_VERTICAL

        titleRow.addView(
            title,
            android.widget.LinearLayout.LayoutParams(
                0,
                dp(52),
                1f
            )
        )

        topBar.addView(
            titleRow
        )

        /*
         * Поиск улицы.
         */
        val search =
            android.widget.EditText(this)

        search.hint =
            "Поиск улицы или места"

        search.setSingleLine(true)

        search.textSize =
            14f

        search.setPadding(
            dp(14),
            0,
            dp(14),
            0
        )

        search.background =
            roundedBackground(
                Color.rgb(
                    245,
                    246,
                    248
                ),
                dp(18).toFloat()
            )

        val searchParams =
            android.widget.LinearLayout.LayoutParams(
                0,
                dp(42),
                1f
            )

        val searchButton =
            TextView(this)

        searchButton.text =
            "Найти"

        searchButton.textSize =
            14f

        searchButton.setTextColor(
            Color.WHITE
        )

        searchButton.gravity =
            android.view.Gravity.CENTER

        searchButton.background =
            roundedBackground(
                Color.rgb(
                    23,
                    59,
                    145
                ),
                dp(18).toFloat()
            )

        searchButton.setPadding(
            dp(16),
            0,
            dp(16),
            0
        )

        val searchRow =
            android.widget.LinearLayout(this)

        searchRow.orientation =
            android.widget.LinearLayout.HORIZONTAL

        searchRow.gravity =
            android.view.Gravity.CENTER_VERTICAL

        searchRow.setPadding(
            dp(10),
            0,
            dp(10),
            dp(8)
        )

        searchRow.addView(
            search,
            searchParams
        )

        val searchButtonParams =
            android.widget.LinearLayout.LayoutParams(
                dp(70),
                dp(42)
            )

        searchButtonParams.leftMargin =
            dp(6)

        searchRow.addView(
            searchButton,
            searchButtonParams
        )

        topBar.addView(
            searchRow
        )

        searchButton.setOnClickListener {

            val query =
                search.text
                    .toString()
                    .trim()

            if (query.isNotEmpty()) {

                webView.evaluateJavascript(
                    "searchPlace(${jsString(query)});",
                    null
                )
            }
        }

        /*
         * Координаты.
         */
        coordinatesText =
            TextView(this)

        coordinatesText.text =
            formatCoordinates()

        coordinatesText.textSize =
            13f

        coordinatesText.setTextColor(
            Color.DKGRAY
        )

        coordinatesText.gravity =
            android.view.Gravity.CENTER

        coordinatesText.background =
            roundedBackground(
                Color.WHITE,
                dp(16).toFloat()
            )

        val coordinatesParams =
            android.widget.FrameLayout.LayoutParams(
                dp(210),
                dp(42)
            )

        coordinatesParams.gravity =
            android.view.Gravity.TOP or
                    android.view.Gravity.END

        coordinatesParams.topMargin =
            dp(115)

        coordinatesParams.rightMargin =
            dp(10)

        root.addView(
            coordinatesText,
            coordinatesParams
        )

        /*
         * Центральная точка.
         */
        val center =
            TextView(this)

        center.text =
            "●"

        center.textSize =
            30f

        center.setTextColor(
            Color.rgb(
                220,
                40,
                40
            )
        )

        center.gravity =
            android.view.Gravity.CENTER

        center.setShadowLayer(
            5f,
            0f,
            2f,
            Color.WHITE
        )

        val centerParams =
            android.widget.FrameLayout.LayoutParams(
                dp(50),
                dp(50)
            )

        centerParams.gravity =
            android.view.Gravity.CENTER

        root.addView(
            center,
            centerParams
        )

        /*
         * Кнопка снизу.
         */
        selectButton =
            TextView(this)

        selectButton.text =
            "Выбрать эту точку"

        selectButton.textSize =
            16f

        selectButton.setTextColor(
            Color.WHITE
        )

        selectButton.gravity =
            android.view.Gravity.CENTER

        selectButton.typeface =
            android.graphics.Typeface.DEFAULT_BOLD

        selectButton.background =
            roundedBackground(
                Color.rgb(
                    23,
                    59,
                    145
                ),
                dp(18).toFloat()
            )

        selectButton.setOnClickListener {

            val result =
                Intent()

            result.putExtra(
                EXTRA_LATITUDE,
                selectedLatitude
            )

            result.putExtra(
                EXTRA_LONGITUDE,
                selectedLongitude
            )

            setResult(
                RESULT_OK,
                result
            )

            finish()
        }

        val buttonParams =
            android.widget.FrameLayout.LayoutParams(
                -1,
                dp(56)
            )

        buttonParams.gravity =
            android.view.Gravity.BOTTOM

        buttonParams.leftMargin =
            dp(16)

        buttonParams.rightMargin =
            dp(16)

        buttonParams.bottomMargin =
            dp(20)

        root.addView(
            selectButton,
            buttonParams
        )

        setContentView(root)
    }

    private fun setupWebView() {

        webView.settings.javaScriptEnabled =
            true

        webView.settings.domStorageEnabled =
            true

        webView.settings.allowFileAccess =
            false

        webView.settings.allowContentAccess =
            true

        webView.webViewClient =
            WebViewClient()

        webView.webChromeClient =
            WebChromeClient()

        webView.addJavascriptInterface(
            MapBridge(),
            "Android"
        )

        webView.loadDataWithBaseURL(
            "https://localhost/",
            createMapHtml(),
            "text/html",
            "UTF-8",
            null
        )
    }

    private fun createMapHtml(): String {

        return """
<!DOCTYPE html>
<html>

<head>

<meta
    name="viewport"
    content="width=device-width,
    initial-scale=1.0,
    maximum-scale=1.0,
    user-scalable=no">

<link
    rel="stylesheet"
    href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css">

<script
    src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js">
</script>

<style>

html,
body,
#map {

    width: 100%;
    height: 100%;
    margin: 0;
    padding: 0;
}

.leaflet-control-attribution {

    font-size: 9px;
}

</style>

</head>

<body>

<div id="map"></div>

<script>

var initialLat =
    $DEFAULT_LATITUDE;

var initialLng =
    $DEFAULT_LONGITUDE;

var map =
    L.map('map', {
        zoomControl: true,
        attributionControl: true
    }).setView(
        [initialLat, initialLng],
        14
    );

L.tileLayer(
    'https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
    {
        maxZoom: 19,
        attribution: '© OpenStreetMap contributors'
    }
).addTo(map);

var marker = null;

function updateCenter() {

    var center =
        map.getCenter();

    Android.onMapMoved(
        center.lat,
        center.lng
    );
}

map.on(
    'moveend',
    updateCenter
);

function searchPlace(query) {

    fetch(
        'https://nominatim.openstreetmap.org/search?format=json&limit=1&q=' +
        encodeURIComponent(query)
    )
    .then(
        function(response) {
            return response.json();
        }
    )
    .then(
        function(data) {

            if (
                data &&
                data.length > 0
            ) {

                var lat =
                    parseFloat(data[0].lat);

                var lon =
                    parseFloat(data[0].lon);

                map.setView(
                    [lat, lon],
                    17,
                    {
                        animate: true
                    }
                );

                Android.onMapMoved(
                    lat,
                    lon
                );

            } else {

                Android.onSearchError(
                    'Место не найдено'
                );
            }
        }
    )
    .catch(
        function(error) {

            Android.onSearchError(
                'Ошибка поиска'
            );
        }
    );
}

</script>

</body>
</html>
        """.trimIndent()
    }

    inner class MapBridge {

        @JavascriptInterface
        fun onMapMoved(
            latitude: Double,
            longitude: Double
        ) {

            runOnUiThread {

                selectedLatitude =
                    latitude

                selectedLongitude =
                    longitude

                coordinatesText.text =
                    formatCoordinates()
            }
        }

        @JavascriptInterface
        fun onSearchError(
            message: String
        ) {

            runOnUiThread {

                android.widget.Toast.makeText(
                    this@MapPickerActivity,
                    message,
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun formatCoordinates(): String {

        return String.format(
            java.util.Locale.US,
            "%.6f, %.6f",
            selectedLatitude,
            selectedLongitude
        )
    }

    private fun jsString(
        value: String
    ): String {

        return "'" +
                value
                    .replace(
                        "\\",
                        "\\\\"
                    )
                    .replace(
                        "'",
                        "\\'"
                    )
                    .replace(
                        "\n",
                        "\\n"
                    ) +
                "'"
    }

    private fun roundedBackground(
        color: Int,
        radius: Float
    ): android.graphics.drawable.GradientDrawable {

        return android.graphics.drawable.GradientDrawable().apply {

            setColor(color)

            cornerRadius =
                radius
        }
    }

    private fun dp(
        value: Int
    ): Int {

        return (
                value *
                        resources.displayMetrics.density
                ).toInt()
    }
}