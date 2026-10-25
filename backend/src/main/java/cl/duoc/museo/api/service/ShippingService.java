package cl.duoc.museo.api.service;

import org.springframework.stereotype.Service;

import java.util.Set;

/**
 * Reglas FICTICIAS de envío (no se integra ningún courier real).
 * Son las mismas reglas que usa la app (ShippingCalculator).
 */
@Service
public class ShippingService {

    private static final Set<String> LATAM = Set.of(
            "Argentina", "Perú", "Colombia", "México", "Brasil", "Uruguay", "Ecuador", "Costa Rica");

    /** @return costo de envío, o -1 si la modalidad no aplica al destino. */
    public double quote(String country, String type, int physicalUnits) {
        if (physicalUnits <= 0 || "DIGITAL".equals(type)) return 0;
        if ("RETIRO".equals(type)) return "Chile".equals(country) ? 0 : -1;
        double base, perItem, expressFactor;
        if ("Chile".equals(country)) { base = 4; perItem = 1; expressFactor = 1.8; }
        else if (LATAM.contains(country)) { base = 12; perItem = 2; expressFactor = 1.6; }
        else { base = 20; perItem = 3; expressFactor = 1.5; }
        double standard = base + perItem * (physicalUnits - 1);
        return "EXPRESS".equals(type) ? standard * expressFactor : standard;
    }

    public String estimatedDays(String type) {
        return switch (type) {
            case "EXPRESS" -> "2-5 días";
            case "RETIRO" -> "Retiro en sede";
            case "DIGITAL" -> "Inmediato";
            default -> "7-14 días";
        };
    }
}
