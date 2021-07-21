package com.anytypeio.anytype.presentation.navigation

import com.anytypeio.anytype.core_models.Id
import com.anytypeio.anytype.core_models.ObjectType
import com.anytypeio.anytype.presentation.`object`.ObjectIcon

data class DefaultObjectView(
    val id: Id,
    val name: String? = null,
    val typeName: String? = null,
    val typeLayout: ObjectType.Layout? = null,
    val icon: ObjectIcon = ObjectIcon.None
)

data class ObjectView(
    val id: String,
    val title: String,
    val subtitle: String,
    val image: String? = null,
    val emoji: String? = null,
    val layout: ObjectType.Layout? = null
)

fun ObjectView.isContainsText(text: String): Boolean = title.contains(text, true) ||
        subtitle.contains(text, true)

fun List<ObjectView>.filterBy(text: String): List<ObjectView> =
    if (text.isNotEmpty()) this.filter { it.isContainsText(text) } else this