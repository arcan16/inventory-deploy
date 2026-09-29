package com.inventories.models.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Optional;

/**
 * Presentaciones de un producto. El "label" es el valor exacto guardado en la
 * columna ENUM product_presentations.presentation (y el mismo texto que llega
 * en la columna de unidad de los CSV de inventario, p. ej. "4 LTS").
 */
public enum ProductPresentation {
    EIGHTH_LITER("1/8 LT"),
    QUARTER_LITER("1/4 LT"),
    ONE_LITER("1 LT"),
    GALLON("3.785 LTS"),
    FOUR_LITERS("4 LTS"),
    TWO_AND_A_HALF_GALLONS("9.46 LTS"),
    NINETEEN_LITERS("19 LTS"),
    PIECE("PIEZA");

    private final String label;

    ProductPresentation(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public static ProductPresentation fromLabel(String label) {
        return findByLabel(label)
                .orElseThrow(() -> new IllegalArgumentException("Presentacion desconocida: " + label));
    }

    /** Busca la presentacion por su label ignorando mayusculas y espacios sobrantes. */
    public static Optional<ProductPresentation> findByLabel(String label) {
        if (label == null)
            return Optional.empty();
        String normalized = label.trim().replaceAll("\\s+", " ");
        for (ProductPresentation presentation : values()) {
            if (presentation.label.equalsIgnoreCase(normalized))
                return Optional.of(presentation);
        }
        return Optional.empty();
    }

    /** Guarda el label ("1/8 LT") en lugar del nombre de la constante. */
    @Converter(autoApply = true)
    public static class JpaConverter implements AttributeConverter<ProductPresentation, String> {
        @Override
        public String convertToDatabaseColumn(ProductPresentation presentation) {
            return presentation == null ? null : presentation.getLabel();
        }

        @Override
        public ProductPresentation convertToEntityAttribute(String label) {
            return label == null ? null : ProductPresentation.fromLabel(label);
        }
    }
}
