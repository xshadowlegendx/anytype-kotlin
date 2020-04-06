package com.agileburo.anytype.core_ui.features.page.pattern

interface Matcher<out T> {
    fun match(text: String): List<T>
}