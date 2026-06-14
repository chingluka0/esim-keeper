package com.baohao.esimkeeper.data

import java.util.Locale

object CardSorter {
    fun sort(cards: List<ESimCard>, order: CardSortOrder): List<ESimCard> =
        when (order) {
            CardSortOrder.ExpiryDate -> cards.sortedWith(compareBy<ESimCard> { it.expiryDate }.thenByDescending { it.updatedAt })
            CardSortOrder.CreatedAt -> cards.sortedByDescending { it.createdAt }
            CardSortOrder.Name -> cards.sortedBy { it.name.lowercase(Locale.ROOT) }
        }
}
