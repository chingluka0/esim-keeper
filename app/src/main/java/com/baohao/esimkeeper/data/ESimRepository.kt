package com.baohao.esimkeeper.data

class ESimRepository(private val dao: ESimCardDao) {
    val cards = dao.observeCards()

    suspend fun save(card: ESimCard) = dao.upsert(card)

    suspend fun importAsNewCards(cards: List<ESimCard>) = dao.insertAll(cards)

    suspend fun delete(card: ESimCard) = dao.delete(card)
}
