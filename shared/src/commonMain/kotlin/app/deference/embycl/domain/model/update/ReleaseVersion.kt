package app.deference.embycl.domain.model.update

/**
 * An app version as the release pipeline tags it: `vMAJOR.MINOR.PATCH` for a release and
 * `vMAJOR.MINOR.PATCH-beta.N` for a pre-release. A beta orders below the release it precedes.
 */
data class ReleaseVersion(
    val major: Int,
    val minor: Int,
    val patch: Int,
    val beta: Int?,
) : Comparable<ReleaseVersion> {
    val isPrerelease: Boolean get() = beta != null

    override fun compareTo(other: ReleaseVersion): Int = compareValuesBy(
        this,
        other,
        { it.major },
        { it.minor },
        { it.patch },
        { it.beta ?: Int.MAX_VALUE },
    )

    companion object {
        private val PATTERN = Regex(pattern = """v?(\d+)\.(\d+)\.(\d+)(?:-beta\.(\d+))?""")

        /** Parses a tag or version name, with or without the `v` prefix; `null` for anything else. */
        fun parse(text: String): ReleaseVersion? {
            val match = PATTERN.matchEntire(input = text.trim()) ?: return null
            val (major, minor, patch, beta) = match.destructured
            return ReleaseVersion(
                major = major.toIntOrNull() ?: return null,
                minor = minor.toIntOrNull() ?: return null,
                patch = patch.toIntOrNull() ?: return null,
                beta = beta.takeIf { it.isNotEmpty() }?.let { it.toIntOrNull() ?: return null },
            )
        }
    }
}
