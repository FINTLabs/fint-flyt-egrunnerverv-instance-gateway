package no.novari.flyt.egrunnerverv.gateway.instance

import no.novari.cache.FintCache
import no.novari.fint.model.felles.kompleksedatatyper.Identifikator
import no.novari.fint.model.felles.kompleksedatatyper.Kontaktinformasjon
import no.novari.fint.model.resource.Link
import no.novari.fint.model.resource.administrasjon.personal.PersonalressursResource
import no.novari.fint.model.resource.arkiv.noark.ArkivressursResource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class ResourceRepositoryTest {
    private val personalressursResourceCache: FintCache<String, PersonalressursResource> = mock()
    private val arkivressursResourceCache: FintCache<String, ArkivressursResource> = mock()

    private lateinit var repo: ResourceRepository

    @BeforeEach
    fun setUp() {
        repo = ResourceRepository(personalressursResourceCache, arkivressursResourceCache)
    }

    @Test
    fun `returns null when personalressurs is missing`() {
        whenever(personalressursResourceCache.getAllDistinct()).thenReturn(emptyList())
        whenever(arkivressursResourceCache.getAllDistinct()).thenReturn(emptyList())

        assertThat(repo.getArkivressursHrefFromPersonEmail("test@novari.no")).isNull()
    }

    @Test
    fun `can resolve arkivressurs self link from normalized person email`() {
        val personalressursHref = "https://api.example/personalressurs/testuser"
        val arkivressursSelfHref = "https://api.example/arkivressurs/42"

        val personalressurs =
            PersonalressursResource().apply {
                val kontaktinformasjon = Kontaktinformasjon().apply { setEpostadresse("Test@Novari.No") }
                val brukernavn = Identifikator().apply { setIdentifikatorverdi("testuser") }
                setKontaktinformasjon(kontaktinformasjon)
                setBrukernavn(brukernavn)
                addSelf(Link(personalressursHref))
            }

        val arkivressurs =
            ArkivressursResource().apply {
                // Repo matches on "personalressurs" link href ending with username (case-insensitive)
                addPersonalressurs(Link(personalressursHref))
                addSelf(Link(arkivressursSelfHref))
            }

        whenever(personalressursResourceCache.getAllDistinct()).thenReturn(listOf(personalressurs))
        whenever(arkivressursResourceCache.getAllDistinct()).thenReturn(listOf(arkivressurs))

        val href = repo.getArkivressursHrefFromPersonEmail("  TEST@NOVARI.NO  ")
        assertThat(href).isEqualTo(arkivressursSelfHref)
    }
}
