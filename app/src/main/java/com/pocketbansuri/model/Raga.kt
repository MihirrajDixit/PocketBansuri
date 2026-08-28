package com.pocketbansuri.model

data class Raga(
    val name: String,
    val category: String, // e.g. Thaat name (tag)
    val vadi: String,
    val samvadi: String,
    val aarohNotes: List<String>,
    val avrohNotes: List<String>
) {
    companion object {
        val dummyRagas = listOf(
            Raga(
                name = "Yaman",
                category = "Kalyan",
                vadi = "Ga",
                samvadi = "Ni",
                aarohNotes = listOf("Ni_", "Re", "Ga", "Ma#", "Dha", "Ni", "Sa'"),
                avrohNotes = listOf("Sa'", "Ni", "Dha", "Pa", "Ma#", "Ga", "Re", "Sa")
            ),
            Raga(
                name = "Bhupali",
                category = "Kalyan",
                vadi = "Ga",
                samvadi = "Dha",
                aarohNotes = listOf("Sa", "Re", "Ga", "Pa", "Dha", "Sa'"),
                avrohNotes = listOf("Sa'", "Dha", "Pa", "Ga", "Re", "Sa")
            ),
            Raga(
                name = "Hansadhwani",
                category = "Bilawal",
                vadi = "Sa",
                samvadi = "Pa",
                aarohNotes = listOf("Sa", "Re", "Ga", "Pa", "Ni", "Sa'"),
                avrohNotes = listOf("Sa'", "Ni", "Pa", "Ga", "Re", "Sa")
            ),
            Raga(
                name = "Alhaiya Bilawal",
                category = "Bilawal",
                vadi = "Dha",
                samvadi = "Ga",
                aarohNotes = listOf("Sa", "Re", "Ga", "Pa", "Dha", "Ni", "Sa'"),
                avrohNotes = listOf("Sa'", "Ni", "Dha", "Pa", "Ma", "Ga", "Re", "Sa")
            ),
            Raga(
                name = "Bhairav",
                category = "Bhairav",
                vadi = "dha",
                samvadi = "re",
                aarohNotes = listOf("Sa", "re", "Ga", "Ma", "Pa", "dha", "Ni", "Sa'"),
                avrohNotes = listOf("Sa'", "Ni", "dha", "Pa", "Ma", "Ga", "re", "Sa")
            ),
            Raga(
                name = "Kafi",
                category = "Kafi",
                vadi = "Pa",
                samvadi = "Sa",
                aarohNotes = listOf("Sa", "Re", "ga", "Ma", "Pa", "Dha", "ni", "Sa'"),
                avrohNotes = listOf("Sa'", "ni", "Dha", "Pa", "Ma", "ga", "Re", "Sa")
            ),
            Raga(
                name = "Bhairavi",
                category = "Bhairavi",
                vadi = "Ma",
                samvadi = "Sa",
                aarohNotes = listOf("Sa", "re", "ga", "Ma", "Pa", "dha", "ni", "Sa'"),
                avrohNotes = listOf("Sa'", "ni", "dha", "Pa", "Ma", "ga", "re", "Sa")
            ),
            Raga(
                name = "Asavari",
                category = "Asavari",
                vadi = "dha",
                samvadi = "ga",
                aarohNotes = listOf("Sa", "Re", "Ma", "Pa", "dha", "Sa'"),
                avrohNotes = listOf("Sa'", "ni", "dha", "Pa", "Ma", "ga", "Re", "Sa")
            ),
            Raga(
                name = "Khamaj",
                category = "Khamaj",
                vadi = "Ga",
                samvadi = "Ni",
                aarohNotes = listOf("Sa", "Ga", "Ma", "Pa", "Dha", "Ni", "Sa'"),
                avrohNotes = listOf("Sa'", "ni", "Dha", "Pa", "Ma", "Ga", "Re", "Sa")
            ),
            Raga(
                name = "Todi",
                category = "Todi",
                vadi = "dha",
                samvadi = "ga",
                aarohNotes = listOf("Sa", "re", "ga", "Ma#", "Pa", "dha", "Ni", "Sa'"),
                avrohNotes = listOf("Sa'", "Ni", "dha", "Pa", "Ma#", "ga", "re", "Sa")
            ),
            Raga(
                name = "Purvi",
                category = "Purvi",
                vadi = "Ga",
                samvadi = "Ni",
                aarohNotes = listOf("Sa", "re", "Ga", "Ma#", "Pa", "dha", "Ni", "Sa'"),
                avrohNotes = listOf("Sa'", "Ni", "dha", "Pa", "Ma#", "Ga", "re", "Sa")
            ),
            Raga(
                name = "Marwa",
                category = "Marwa",
                vadi = "Dha",
                samvadi = "re",
                aarohNotes = listOf("Sa", "re", "Ga", "Ma#", "Dha", "Ni", "Sa'"),
                avrohNotes = listOf("Sa'", "Ni", "Dha", "Ma#", "Ga", "re", "Sa")
            ),
            Raga(
                name = "Desh",
                category = "Khamaj",
                vadi = "Re",
                samvadi = "Pa",
                aarohNotes = listOf("Sa", "Re", "Ma", "Pa", "Ni", "Sa'"),
                avrohNotes = listOf("Sa'", "ni", "Dha", "Pa", "Ma", "Ga", "Re", "Sa")
            ),
            Raga(
                name = "Bhimpalasi",
                category = "Kafi",
                vadi = "Ma",
                samvadi = "Sa",
                aarohNotes = listOf("Ni_", "Sa", "ga", "Ma", "Pa", "ni", "Sa'"),
                avrohNotes = listOf("Sa'", "ni", "Dha", "Pa", "Ma", "ga", "Re", "Sa")
            ),
            Raga(
                name = "Dhani",
                category = "Kafi",
                vadi = "ga",
                samvadi = "ni",
                aarohNotes = listOf("Sa", "ga", "Ma", "Pa", "ni", "Sa'"),
                avrohNotes = listOf("Sa'", "ni", "Pa", "Ma", "ga", "Sa")
            ),
            Raga(
                name = "Bageshri",
                category = "Kafi",
                vadi = "Ma",
                samvadi = "Sa",
                aarohNotes = listOf("Sa", "Re", "ga", "Ma", "Dha", "ni", "Sa'"),
                avrohNotes = listOf("Sa'", "ni", "Dha", "Ma", "ga", "Re", "Sa")
            ),
            Raga(
                name = "Durga",
                category = "Bilawal",
                vadi = "Dha",
                samvadi = "Re",
                aarohNotes = listOf("Sa", "Re", "Ma", "Pa", "Dha", "Sa'"),
                avrohNotes = listOf("Sa'", "Dha", "Pa", "Ma", "Re", "Sa")
            ),
            Raga(
                name = "Kalavati",
                category = "Khamaj",
                vadi = "Pa",
                samvadi = "Sa",
                aarohNotes = listOf("Sa", "Ga", "Pa", "Dha", "ni", "Sa'"),
                avrohNotes = listOf("Sa'", "ni", "Dha", "Pa", "Ga", "Sa")
            ),
            Raga(
                name = "Malkauns",
                category = "Bhairavi",
                vadi = "Ma",
                samvadi = "Sa",
                aarohNotes = listOf("Sa", "ga", "Ma", "dha", "ni", "Sa'"),
                avrohNotes = listOf("Sa'", "ni", "dha", "Ma", "ga", "Sa")
            ),
            Raga(
                name = "Brindabani Sarang",
                category = "Kafi",
                vadi = "Re",
                samvadi = "Pa",
                aarohNotes = listOf("Sa", "Re", "Ma", "Pa", "Ni", "Sa'"),
                avrohNotes = listOf("Sa'", "ni", "Pa", "Ma", "Re", "Sa")
            )
        )
    }
}
