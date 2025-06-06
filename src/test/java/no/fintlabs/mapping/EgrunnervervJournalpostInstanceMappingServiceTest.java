package no.fintlabs.mapping;

import no.fintlabs.ResourceRepository;
import no.fintlabs.exceptions.ArchiveResourceNotFoundException;
import no.fintlabs.gateway.instance.model.File;
import no.fintlabs.gateway.instance.model.instance.InstanceObject;
import no.fintlabs.models.EgrunnervervJournalpostDocument;
import no.fintlabs.models.EgrunnervervJournalpostInstance;
import no.fintlabs.models.EgrunnervervJournalpostInstanceBody;
import no.fintlabs.models.EgrunnervervJournalpostReceiver;
import no.fintlabs.slack.SlackAlertService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.function.Function;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EgrunnervervJournalpostInstanceMappingServiceTest {
    public static final long egrunnervervSourceApplicationId = 2;

    EgrunnervervJournalpostInstanceMappingService egrunnervervJournalpostInstanceMappingService;

    ArgumentMatcher<File> argumentMatcherHoveddokument;
    ArgumentMatcher<File> argumentMatcherVedlegg1;
    ArgumentMatcher<File> argumentMatcherVedlegg2;

    @Mock
    FormattingUtilsService formattingUtilsService;

    @Mock
    ResourceRepository resourceRepository;
    @Mock
    SlackAlertService slackAlertService;

    @Mock
    Function<File, Mono<UUID>> persistFile;

    @BeforeEach
    public void setUp() {
        when(formattingUtilsService.formatEmail(" testSaksansvarligEpost@fintlabs.no ")).thenReturn("testsaksansvarligepost@fintlabs.no");
    }

    private EgrunnervervJournalpostInstance createTestJournalpostInstance(String organisasjonsnummer) {
        return EgrunnervervJournalpostInstance
                .builder()
                .saksnummer("testSaksnummer")
                .egrunnervervJournalpostInstanceBody(
                        EgrunnervervJournalpostInstanceBody
                                .builder()
                                .tittel("testTittel")
                                .dokumentNavn("testDokumentNavn")
                                .dokumentDato("testDokumentDato")
                                .forsendelsesMate("testForsendelsesmaate")
                                .kommunenavn("TESTKOMMUNENAVN")
                                .knr("testKnr")
                                .gnr("testGnr")
                                .bnr("testBnr")
                                .fnr("testFnr")
                                .snr("testSnr")
                                .eierforhold("testEierforhold")
                                .sysId("testSysId")
                                .id("testId")
                                .maltittel("testMaltittel")
                                .prosjektnavn("testProsjektnavn")
                                .saksbehandlerEpost(" testSaksansvarligEpost@fintlabs.no ")
                                .mottakere(List.of(
                                        EgrunnervervJournalpostReceiver
                                                .builder()
                                                .navn("testNavn")
                                                .organisasjonsnummer(organisasjonsnummer)
                                                .epost("testEpost")
                                                .telefon("testTelefon")
                                                .postadresse("testPostadresse")
                                                .poststed("testPoststed")
                                                .postnummer("testPostnummer")
                                                .build())
                                )
                                .dokumenter(List.of(
                                        EgrunnervervJournalpostDocument
                                                .builder()
                                                .tittel("testHoveddokumentTittel")
                                                .hoveddokument(true)
                                                .filnavn("testHoveddokumentFilnavn.pdf")
                                                .dokumentBase64("SG92ZWRkb2t1bWVudA==")
                                                .build(),
                                        EgrunnervervJournalpostDocument
                                                .builder()
                                                .tittel("testVedlegg1Tittel")
                                                .hoveddokument(false)
                                                .filnavn("testVedlegg1Filnavn.pdf")
                                                .dokumentBase64("VmVkbGVnZzE=")
                                                .build(),
                                        EgrunnervervJournalpostDocument
                                                .builder()
                                                .tittel("testVedlegg2Tittel")
                                                .hoveddokument(false)
                                                .filnavn("testVedlegg2Filnavn.pdf")
                                                .dokumentBase64("VmVkbGVnZzI=")
                                                .build())
                                )
                                .build()
                )
                .build();
    }

    private InstanceObject createInstance(String organisasjonsnummer, String fodselsnummer) {
        HashMap<String, String> expectedInstanceValuePerKey = getStringStringHashMap();
        return InstanceObject
                .builder()
                .valuePerKey(expectedInstanceValuePerKey)
                .objectCollectionPerKey(
                        Map.of(
                                "mottakere", List.of(
                                        InstanceObject
                                                .builder()
                                                .valuePerKey(Map.of(
                                                        "navn", "testNavn",
                                                        "organisasjonsnummer", organisasjonsnummer,
                                                        "fodselsnummer", fodselsnummer,
                                                        "epost", "testEpost",
                                                        "telefon", "testTelefon",
                                                        "postadresse", "testPostadresse",
                                                        "poststed", "testPoststed",
                                                        "postnummer", "testPostnummer"
                                                ))
                                                .build()
                                ),
                                "vedlegg",
                                List.of(
                                        InstanceObject
                                                .builder()
                                                .valuePerKey(Map.of(
                                                        "tittel", "testVedlegg1Tittel",
                                                        "filnavn", "testVedlegg1Filnavn.pdf",
                                                        "mediatype", "application/pdf",
                                                        "fil", "251bfa61-6c0e-47d0-a479-643c40c3e767"
                                                ))
                                                .build(),
                                        InstanceObject
                                                .builder()
                                                .valuePerKey(Map.of(
                                                        "tittel", "testVedlegg2Tittel",
                                                        "filnavn", "testVedlegg2Filnavn.pdf",
                                                        "mediatype", "application/pdf",
                                                        "fil", "251bfa61-6c0e-47d0-a479-643c40c3e768"
                                                ))
                                                .build()
                                )
                        )
                )
                .build();
    }

    private static HashMap<String, String> getStringStringHashMap() {
        HashMap<String, String> expectedInstanceValuePerKey = new HashMap<>();
        expectedInstanceValuePerKey.put("tittel", "testTittel");
        expectedInstanceValuePerKey.put("dokumentNavn", "testDokumentNavn");
        expectedInstanceValuePerKey.put("dokumentDato", "testDokumentDato");
        expectedInstanceValuePerKey.put("forsendelsesmaate", "testForsendelsesmaate");
        expectedInstanceValuePerKey.put("kommunenavn", "Testkommunenavn");
        expectedInstanceValuePerKey.put("knr", "testKnr");
        expectedInstanceValuePerKey.put("gnr", "testGnr");
        expectedInstanceValuePerKey.put("bnr", "testBnr");
        expectedInstanceValuePerKey.put("fnr", "testFnr");
        expectedInstanceValuePerKey.put("snr", "testSnr");
        expectedInstanceValuePerKey.put("eierforhold", "testEierforhold");
        expectedInstanceValuePerKey.put("id", "testId");
        expectedInstanceValuePerKey.put("maltittel", "testMaltittel");
        expectedInstanceValuePerKey.put("prosjektnavn", "testProsjektnavn");
        expectedInstanceValuePerKey.put("saksbehandlerEpost", "testsaksansvarligepost@fintlabs.no");
        expectedInstanceValuePerKey.put("saksnummer", "testSaksnummer");
        expectedInstanceValuePerKey.put("hoveddokumentFil", "251bfa61-6c0e-47d0-a479-643c40c3e766");
        expectedInstanceValuePerKey.put("hoveddokumentTittel", "testHoveddokumentTittel");
        expectedInstanceValuePerKey.put("hoveddokumentFilnavn", "testHoveddokumentFilnavn.pdf");
        expectedInstanceValuePerKey.put("hoveddokumentMediatype", "application/pdf");
        expectedInstanceValuePerKey.put("saksbehandler", "testSaksansvarlig");
        return expectedInstanceValuePerKey;
    }

    public void setUpFileFunctionMock() {
        argumentMatcherHoveddokument = file ->
                "testHoveddokumentFilnavn.pdf".equals(file.getName()) &&
                        "UTF-8".equals(file.getEncoding()) &&
                        "application/pdf".equals(String.valueOf(file.getType())) &&
                        "2".equals(String.valueOf(file.getSourceApplicationId())) &&
                        "testSysId".equals(file.getSourceApplicationInstanceId()) &&
                        "SG92ZWRkb2t1bWVudA==".equals(file.getBase64Contents());
        doReturn(Mono.just(UUID.fromString("251bfa61-6c0e-47d0-a479-643c40c3e766")))
                .when(persistFile).apply(argThat(argumentMatcherHoveddokument));

        argumentMatcherVedlegg1 = file ->
                "testVedlegg1Filnavn.pdf".equals(file.getName()) &&
                        "UTF-8".equals(file.getEncoding()) &&
                        "application/pdf".equals(String.valueOf(file.getType())) &&
                        "2".equals(String.valueOf(file.getSourceApplicationId())) &&
                        "testSysId".equals(file.getSourceApplicationInstanceId()) &&
                        "VmVkbGVnZzE=".equals(file.getBase64Contents());
        doReturn(Mono.just(UUID.fromString("251bfa61-6c0e-47d0-a479-643c40c3e767")))
                .when(persistFile).apply(argThat(argumentMatcherVedlegg1));

        argumentMatcherVedlegg2 = file ->
                "testVedlegg2Filnavn.pdf".equals(file.getName()) &&
                        "UTF-8".equals(file.getEncoding()) &&
                        "application/pdf".equals(String.valueOf(file.getType())) &&
                        "2".equals(String.valueOf(file.getSourceApplicationId())) &&
                        "testSysId".equals(file.getSourceApplicationInstanceId()) &&
                        "VmVkbGVnZzI=".equals(file.getBase64Contents());
        doReturn(Mono.just(UUID.fromString("251bfa61-6c0e-47d0-a479-643c40c3e768")))
                .when(persistFile).apply(argThat(argumentMatcherVedlegg2));
    }

    @Test
    public void givenJournalpostWithOrganisasjonsnummerWithNineDigits_shouldReturnMappedInstanceWithOrganisasjonsnummer() {
        EgrunnervervJournalpostInstance egrunnervervJournalpostInstance = createTestJournalpostInstance(
                "123456789"
        );
        InstanceObject expectedInstance = createInstance(
                "123456789",
                ""
        );

        when(formattingUtilsService.formatEmail(" testSaksansvarligEpost@fintlabs.no "))
                .thenReturn("testsaksansvarligepost@fintlabs.no");

        when(resourceRepository.getArkivressursHrefFromPersonEmail("testsaksansvarligepost@fintlabs.no"))
                .thenReturn(Optional.of("testSaksansvarlig"));
        when(formattingUtilsService.formatKommunenavn("TESTKOMMUNENAVN")).thenReturn("Testkommunenavn");

        setUpFileFunctionMock();

        egrunnervervJournalpostInstanceMappingService = new EgrunnervervJournalpostInstanceMappingService(
                resourceRepository,
                formattingUtilsService,
                slackAlertService
        );
        egrunnervervJournalpostInstanceMappingService.checkSaksbehandler = true;

        InstanceObject instanceObject = egrunnervervJournalpostInstanceMappingService.map(
                egrunnervervSourceApplicationId,
                egrunnervervJournalpostInstance,
                persistFile
        ).block();
        assertThat(instanceObject).isEqualTo(expectedInstance);
    }

    @Test
    public void givenJournalpostWithOrganisasjonsnummerWithElevenDigits_shouldReturnMappedInstanceWithFodselsnummer() {
        EgrunnervervJournalpostInstance egrunnervervJournalpostInstance = createTestJournalpostInstance(
                "01234567890"
        );
        InstanceObject expectedInstance = createInstance(
                "",
                "01234567890"
        );

        when(resourceRepository.getArkivressursHrefFromPersonEmail("testsaksansvarligepost@fintlabs.no"))
                .thenReturn(Optional.of("testSaksansvarlig"));
        when(formattingUtilsService.formatKommunenavn("TESTKOMMUNENAVN")).thenReturn("Testkommunenavn");

        setUpFileFunctionMock();

        egrunnervervJournalpostInstanceMappingService = new EgrunnervervJournalpostInstanceMappingService(
                resourceRepository,
                formattingUtilsService,
                slackAlertService
        );
        egrunnervervJournalpostInstanceMappingService.checkSaksbehandler = true;

        InstanceObject instanceObject = egrunnervervJournalpostInstanceMappingService.map(
                egrunnervervSourceApplicationId,
                egrunnervervJournalpostInstance,
                persistFile
        ).block();
        assertThat(instanceObject).isEqualTo(expectedInstance);
    }

    @Test
    public void givenJournalpostWithHoveddokumentAndVedlegg_shouldReturnMappedInstanceAsExpected() {
        EgrunnervervJournalpostInstance egrunnervervJournalpostInstance = createTestJournalpostInstance(
                "123456789"
        );
        InstanceObject expectedInstance = createInstance(
                "123456789",
                ""
        );

        when(resourceRepository.getArkivressursHrefFromPersonEmail("testsaksansvarligepost@fintlabs.no"))
                .thenReturn(Optional.of("testSaksansvarlig"));
        when(formattingUtilsService.formatKommunenavn("TESTKOMMUNENAVN")).thenReturn("Testkommunenavn");

        setUpFileFunctionMock();

        egrunnervervJournalpostInstanceMappingService = new EgrunnervervJournalpostInstanceMappingService(
                resourceRepository,
                formattingUtilsService,
                slackAlertService
        );
        egrunnervervJournalpostInstanceMappingService.checkSaksbehandler = true;

        InstanceObject instanceObject = egrunnervervJournalpostInstanceMappingService.map(
                egrunnervervSourceApplicationId,
                egrunnervervJournalpostInstance,
                persistFile
        ).block();
        assertThat(instanceObject).isEqualTo(expectedInstance);

        verify(persistFile, times(1)).apply(argThat(argumentMatcherHoveddokument));
        verify(persistFile, times(1)).apply(argThat(argumentMatcherVedlegg1));
        verify(persistFile, times(1)).apply(argThat(argumentMatcherVedlegg2));
        verifyNoMoreInteractions(persistFile);
    }

    @Test
    public void givenNoArkivressursHrefForSaksansvarlig_shouldThrowArchiveResourceNotFoundException() {
        EgrunnervervJournalpostInstance egrunnervervJournalpostInstance = createTestJournalpostInstance(
                "123456789"
        );

        when(resourceRepository.getArkivressursHrefFromPersonEmail("testsaksansvarligepost@fintlabs.no"))
                .thenReturn(Optional.empty());

        egrunnervervJournalpostInstanceMappingService = new EgrunnervervJournalpostInstanceMappingService(
                resourceRepository,
                formattingUtilsService,
                slackAlertService
        );
        egrunnervervJournalpostInstanceMappingService.checkSaksbehandler = true;

        assertThrows(
                ArchiveResourceNotFoundException.class,
                () -> egrunnervervJournalpostInstanceMappingService.map(
                        egrunnervervSourceApplicationId,
                        egrunnervervJournalpostInstance,
                        persistFile
                ).block());
    }

}