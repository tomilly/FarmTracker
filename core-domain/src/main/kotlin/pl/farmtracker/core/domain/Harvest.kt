package pl.farmtracker.core.domain

import kotlin.random.Random

/** Zbiór, np. „Kukurydza 2026" – wspólne pola, baza i ludzie. */
data class Harvest(val id: String, val name: String)

/**
 * Osoba w zbiorze. Jedna rola na zbiór (BRIEF §3).
 *
 * @param userId identyfikator konta: z logowania numerem telefonu albo z kodu zaproszenia (konto na tym telefonie)
 * @param phone numer z logowania; przy dołączeniu kodem – numer, który admin wpisał w zaproszeniu (może być pusty)
 */
data class Member(
    val userId: String,
    val name: String,
    val phone: String,
    val role: Role,
)

/** Ostatniego admina nie można usunąć ani zmienić mu roli – zbiór zostałby bez nikogo, kto nim zarządza. */
fun List<Member>.canChangeOrRemove(member: Member): Boolean =
    member.role != Role.ADMIN || count { it.role == Role.ADMIN } > 1

/**
 * Zaproszenie konkretnej osoby na rolę („Zaproś kierowcę" → imię, numer). Kod przychodzi SMS-em; po wpisaniu
 * osoba – bez logowania numerem i bez podawania imienia – trafia od razu na ekran swojej roli. Kod działa raz.
 *
 * @param name imię zaproszonego – tak go zobaczą inni
 * @param phone numer, na który admin wysyła kod (może być pusty – kod podyktowany)
 */
data class Invite(
    val code: InviteCode,
    val harvestId: String,
    val role: Role,
    val expiresAtMillis: Long,
    val name: String = "",
    val phone: String = "",
) {
    fun isValidAt(nowMillis: Long): Boolean = nowMillis < expiresAtMillis

    companion object {
        /** Tydzień – wystarczy, żeby zaprosić ludzi przed zbiorem, a stary kod sam przestaje działać. */
        const val VALID_FOR_MILLIS: Long = 7L * 24 * 60 * 60 * 1000
    }
}

/** Kod zaproszenia: 6 cyfr – na telefonie wpisuje się je z klawiatury numerycznej, bez pomyłek O/0. */
@JvmInline
value class InviteCode(val digits: String) {
    init {
        require(digits.length == LENGTH && digits.all { it.isDigit() }) { "Kod zaproszenia to $LENGTH cyfr" }
    }

    /** „482 913" – łatwiej przeczytać i przepisać. */
    val display: String get() = digits.chunked(LENGTH / 2).joinToString(" ")

    companion object {
        const val LENGTH = 6

        /** Kod z tego, co wpisał człowiek (spacje i myślniki pomija); `null`, gdy to nie 6 cyfr. */
        fun parse(text: String): InviteCode? =
            text.filterNot { it.isWhitespace() || it == '-' }
                .takeIf { it.length == LENGTH && it.all { char -> char.isDigit() } }
                ?.let(::InviteCode)

        fun random(random: Random = Random.Default): InviteCode =
            InviteCode(buildString { repeat(LENGTH) { append(random.nextInt(10)) } })
    }
}
