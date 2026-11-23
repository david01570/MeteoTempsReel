package com.meteo.app

object WeatherCodes {

    fun describe(code: Int): String = when (code) {
        0 -> "Ciel dégagé"
        1 -> "Globalement dégagé"
        2 -> "Partiellement nuageux"
        3 -> "Couvert"
        45, 48 -> "Brouillard"
        51, 53, 55 -> "Bruine"
        56, 57 -> "Bruine verglaçante"
        61, 63, 65 -> "Pluie"
        66, 67 -> "Pluie verglaçante"
        71, 73, 75 -> "Neige"
        77 -> "Grains de neige"
        80, 81, 82 -> "Averses de pluie"
        85, 86 -> "Averses de neige"
        95 -> "Orage"
        96, 99 -> "Orage avec grêle"
        else -> "Conditions inconnues"
    }

    fun emoji(code: Int, isDay: Boolean): String = when (code) {
        0 -> if (isDay) "☀️" else "🌙"
        1 -> if (isDay) "🌤️" else "🌙"
        2 -> "⛅"
        3 -> "☁️"
        45, 48 -> "🌫️"
        in 51..57 -> "🌦️"
        in 61..67 -> "🌧️"
        in 71..77 -> "🌨️"
        in 80..82 -> "🌧️"
        85, 86 -> "❄️"
        95, 96, 99 -> "⛈️"
        else -> "🌡️"
    }
}
