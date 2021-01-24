package com.example.hakonsreader.interfaces

/**
 * Listener for when the language for the application should be changed
 */
fun interface LanguageListener {

    /**
     * Called when the language should be changed
     *
     * @param languageCode The language code to change to (eg. "en")
     */
    fun updateLanguage(languageCode: String)
}