package io.github.fate_grand_automata.scripts.modules

import io.github.fate_grand_automata.scripts.IFgoAutomataApi
import io.github.fate_grand_automata.scripts.ScriptNotify
import io.github.fate_grand_automata.scripts.enums.CardAffinityEnum
import io.github.fate_grand_automata.scripts.enums.CardTypeEnum
import io.github.fate_grand_automata.scripts.models.CommandCard
import io.github.fate_grand_automata.scripts.models.ParsedCard
import io.github.fate_grand_automata.scripts.models.TeamSlot
import io.github.lib_automata.dagger.ScriptScope
import javax.inject.Inject

@ScriptScope
class CardParser @Inject constructor(
    api: IFgoAutomataApi,
    private val servantTracker: ServantTracker
) : IFgoAutomataApi by api {

    private fun CommandCard.Face.affinity(): CardAffinityEnum {
        val region = locations.attack.affinityRegion(this)

        if (images[io.github.fate_grand_automata.scripts.Images.Weak] in region) {
            return io.github.fate_grand_automata.scripts.enums.CardAffinityEnum.Weak
        }

        if (images[io.github.fate_grand_automata.scripts.Images.Resist] in region) {
            return io.github.fate_grand_automata.scripts.enums.CardAffinityEnum.Resist
        }

        return io.github.fate_grand_automata.scripts.enums.CardAffinityEnum.Normal
    }

    private fun CommandCard.Face.isStunned(): Boolean {
        val stunRegion = locations.attack.typeRegion(this).copy(
            y = 930,
            width = 248,
            height = 188
        )

        return images[io.github.fate_grand_automata.scripts.Images.Stun] in stunRegion
    }

    private fun CommandCard.Face.type(): CardTypeEnum {
        val region = locations.attack.typeRegion(this)

        if (images[io.github.fate_grand_automata.scripts.Images.Buster] in region) {
            return io.github.fate_grand_automata.scripts.enums.CardTypeEnum.Buster
        }

        if (images[io.github.fate_grand_automata.scripts.Images.Arts] in region) {
            return io.github.fate_grand_automata.scripts.enums.CardTypeEnum.Arts
        }

        if (images[io.github.fate_grand_automata.scripts.Images.Quick] in region) {
            return io.github.fate_grand_automata.scripts.enums.CardTypeEnum.Quick
        }

        return io.github.fate_grand_automata.scripts.enums.CardTypeEnum.Unknown
    }

    fun parse(): List<ParsedCard> {
        val cardsGroupedByServant = servantTracker.faceCardsGroupedByServant()

        val cards = CommandCard.Face.list
            .map {
                val stunned = it.isStunned()
                val type = if (stunned)
                    CardTypeEnum.Unknown
                else it.type()
                val affinity = if (type == CardTypeEnum.Unknown)
                    CardAffinityEnum.Normal // Couldn't detect card type, so don't care about affinity
                else it.affinity()

                val servant = cardsGroupedByServant
                    .filterValues { cards -> it in cards }
                    .keys
                    .firstOrNull()
                    ?: TeamSlot.Unknown

                val fieldSlot = servantTracker.deployed
                    .entries
                    .firstOrNull { (_, teamSlot) -> teamSlot == servant }
                    ?.key

                ParsedCard(
                    card = it,
                    isStunned = stunned,
                    type = type,
                    affinity = affinity,
                    servant = servant,
                    fieldSlot = fieldSlot
                )
            }

        var unknownCardTypes = false
        var unknownServants = false
        val failedToDetermine = cards
            .filter {
                when {
                    it.isStunned -> false
                    it.type == CardTypeEnum.Unknown -> {
                        unknownCardTypes = true
                        true
                    }
                    it.servant is TeamSlot.Unknown && !prefs.skipServantFaceCardCheck -> {
                        unknownServants = true
                        true
                    }
                    else -> false
                }
            }
            .map { it.card }

        if (failedToDetermine.isNotEmpty()) {
            messages.notify(
                ScriptNotify.FailedToDetermineCards(failedToDetermine, unknownCardTypes, unknownServants)
            )
        }

        return cards
    }
}