package com.aeg.core.fiscalizacion.mqtt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.aeg.core.distributor.DistributorRepository;
import com.aeg.core.mqtt.FiscalizacionUsbController;
import com.aeg.core.mqtt.dto.FiscalizacionUsbPrecheckRequest;
import com.aeg.core.printer.Printer;
import com.aeg.core.printer.PrinterRepository;
import com.aeg.core.printermodel.PrinterModelRepository;
import com.aeg.core.seal.Seal;
import com.aeg.core.seal.SealColor;
import com.aeg.core.seal.SealRepository;
import com.aeg.core.seal.SealStatus;
import com.aeg.core.security.Role;
import com.aeg.core.security.User;
import com.aeg.core.tools.mqtt.BrokerMigrationService;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.AfterEach;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

class FiscalizacionPrecheckTest {

    private PrinterRepository printerRepository;
    private SealRepository sealRepository;
    private FiscalizacionPreconditionValidator validator;
    private Seal seal;

    @BeforeEach
    void setUp() {
        printerRepository = mock(PrinterRepository.class);
        sealRepository = mock(SealRepository.class);
        validator = new FiscalizacionPreconditionValidator(
                printerRepository, sealRepository, mock(PrinterModelRepository.class));

        seal = new Seal();
        seal.setSerial("G1B0033");
        seal.setColor(SealColor.AZUL);
        seal.setStatus(SealStatus.DISPONIBLE);
        when(sealRepository.findBySerialIgnoreCase("G1B0033")).thenReturn(Optional.of(seal));
        when(sealRepository.findBySerialIgnoreCase("NOPE")).thenReturn(Optional.empty());
        when(printerRepository.existsByFiscalSerialIgnoreCase(anyString())).thenReturn(false);
        when(printerRepository.findByMacAddressCompact(anyString())).thenReturn(Optional.empty());
    }

    @Test
    void validDataPasses() {
        assertTrue(validator.precheck("GRA0000017", "G1B0033", "Azul", "20:6E:F1:88:4C:68").isEmpty());
    }

    @Test
    void macIsOptional() {
        assertTrue(validator.precheck("GRA0000017", "G1B0033", "azul", null).isEmpty());
    }

    @Test
    void existingSerialIsReported() {
        when(printerRepository.existsByFiscalSerialIgnoreCase("GRA0000017")).thenReturn(true);
        assertEquals(
                List.of(FiscalizacionConstants.MSG_REGISTRO_EXISTE),
                validator.precheck("GRA0000017", "G1B0033", "Azul", null));
    }

    @Test
    void existingMacIsReported() {
        when(printerRepository.findByMacAddressCompact("206EF1884C68")).thenReturn(Optional.of(new Printer()));
        assertEquals(
                List.of(FiscalizacionConstants.MSG_MAC_EXISTE),
                validator.precheck("GRA0000017", "G1B0033", "Azul", "20:6E:F1:88:4C:68"));
    }

    @Test
    void invalidMacIsReported() {
        assertEquals(
                List.of("La dirección MAC no tiene un formato válido."),
                validator.precheck("GRA0000017", "G1B0033", "Azul", "ZZ:00"));
    }

    @Test
    void missingSealIsReported() {
        assertEquals(
                List.of(FiscalizacionConstants.MSG_PRECINTO_NO_EXISTE),
                validator.precheck("GRA0000017", "NOPE", "Azul", null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void colorMismatchAndAssignedSealAreReported() {
        seal.setStatus(SealStatus.EN_IMPRESORA);
        List<String> errors = validator.precheck("GRA0000017", "G1B0033", "Verde", null);
        assertEquals(2, errors.size());
        assertTrue(errors.contains(FiscalizacionConstants.MSG_PRECINTO_ASIGNADO));
    }

    @Test
    void usbPrecheckRegistersPendingDistributorWhenCalledByDistributorUser() {
        FiscalizacionSessionRegistry sessionRegistry = new FiscalizacionSessionRegistry();
        DistributorRepository distributorRepository = mock(DistributorRepository.class);
        when(distributorRepository.existsById(42L)).thenReturn(true);

        FiscalizacionUsbController controller = new FiscalizacionUsbController(
                validator,
                sessionRegistry,
                distributorRepository,
                mock(BrokerMigrationService.class),
                new ObjectMapper());

        User distributorUser = User.builder()
                .role(Role.DISTRIBUTOR)
                .distributorId(42L)
                .build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(distributorUser, null, List.of()));

        var res = controller.precheck(
                new FiscalizacionUsbPrecheckRequest("gra0000017", "G1B0033", "Azul", "20:6E:F1:88:4C:68"));
        assertTrue(res.ok());
        assertEquals(Optional.of(42L), sessionRegistry.resolvePendingDistributorId("GRA0000017"));

        // If an ADMIN subsequently runs precheck for the same serial, the pending distributor is cleared
        User adminUser = User.builder().role(Role.ADMIN).build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(adminUser, null, List.of()));
        var adminRes = controller.precheck(
                new FiscalizacionUsbPrecheckRequest("GRA0000017", "G1B0033", "Azul", null));
        assertTrue(adminRes.ok());
        assertEquals(Optional.empty(), sessionRegistry.resolvePendingDistributorId("GRA0000017"));
    }
}
