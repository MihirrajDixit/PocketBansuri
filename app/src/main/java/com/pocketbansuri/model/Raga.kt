package com.pocketbansuri.model

data class Raga(
    val name: String,
    val description: String,
    val aaroh: String,
    val avroh: String,
    val vadi: String,
    val samvadi: String,
    val swarasUsed: List<Swara>
) {
    companion object {
        val dummyRagas = listOf(
            Raga(
                name = "Yaman",
                description = "An evening Raga of Kalyan Thaat. Peaceful, romantic, and devotional. It uses Teevra Ma (sharp 4th) and natural notes for all other Swaras.",
                aaroh = "Ni Re Ga Ma(#) Dha Ni Sa'",
                avroh = "Sa' Ni Dha Pa Ma(#) Ga Re Sa",
                vadi = "Ga",
                samvadi = "Ni",
                swarasUsed = listOf(Swara.RE, Swara.GA, Swara.PA, Swara.DHA, Swara.NI, Swara.HIGH_SA)
            ),
            Raga(
                name = "Bhupali",
                description = "An early evening pentatonic Raga (Audav-Audav) of Kalyan Thaat. Extremely melodic and meditative. It omits Ma and Ni completely.",
                aaroh = "Sa Re Ga Pa Dha Sa'",
                avroh = "Sa' Dha Pa Ga Re Sa",
                vadi = "Ga",
                samvadi = "Dha",
                swarasUsed = listOf(Swara.SA, Swara.RE, Swara.GA, Swara.PA, Swara.DHA, Swara.HIGH_SA)
            ),
            Raga(
                name = "Hansadhwani",
                description = "An auspicious pentatonic Raga originated in Carnatic music but highly popular in Hindustani music. It translates to 'The Song of Swans' and omits Ma and Dha.",
                aaroh = "Sa Re Ga Pa Ni Sa'",
                avroh = "Sa' Ni Pa Ga Re Sa",
                vadi = "Sa",
                samvadi = "Pa",
                swarasUsed = listOf(Swara.SA, Swara.RE, Swara.GA, Swara.PA, Swara.NI, Swara.HIGH_SA)
            )
        )
    }
}
