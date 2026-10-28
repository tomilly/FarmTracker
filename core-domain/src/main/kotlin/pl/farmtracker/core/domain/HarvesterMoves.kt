package pl.farmtracker.core.domain

/** Sieczkarnia wjechała na inne pole – kierowcy dostają powiadomienie, dokąd teraz jechać. */
data class HarvesterMove(val harvesterName: String, val fieldId: String)

/**
 * Wykrywa przejazd sieczkarni na inne pole z kolejnych pozycji (na telefonie kierowcy, bez serwera).
 * Pierwsza pozycja sieczkarni to nie przejazd (start pracy). Pole liczy się ostatnie, na którym była –
 * zjazd na drogę i powrót na to samo pole (nawrót, objazd rowu) nie jest przejazdem.
 */
class HarvesterMoves {

    /** Ostatnie pole każdej sieczkarni (po [LiveLocation.userId]); `null` – jeszcze na żadnym nie była. */
    private val lastField = mutableMapOf<String, String?>()

    fun update(locations: List<LiveLocation>): List<HarvesterMove> {
        val moves = mutableListOf<HarvesterMove>()
        for (harvester in locations) {
            if (harvester.role != Role.HARVESTER || harvester.isMe) continue
            val known = harvester.userId in lastField
            val field = harvester.fieldId
            if (field == null) {
                // Poza polami – pamiętamy dalej ostatnie pole (albo że jeszcze na żadnym nie była).
                if (!known) lastField[harvester.userId] = null
                continue
            }
            if (known && field != lastField[harvester.userId]) moves += HarvesterMove(harvester.name, field)
            lastField[harvester.userId] = field
        }
        return moves
    }
}
