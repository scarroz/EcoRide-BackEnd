package co.edu.unbosque.notificationservice.util;

import java.util.Map;

public class TemplateProcessor {

    /**
     * Reemplaza los placeholders del tipo {{variable}} en la plantilla
     * por los valores proporcionados en el mapa de datos.
     *
     * @param template contenido HTML con placeholders
     * @param variables mapa con claves y valores a reemplazar
     * @return plantilla procesada con los valores sustituidos
     */
    public static String processTemplate(String template, Map<String, String> variables) {
        if (template == null || variables == null) return template;

        String processed = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            String key = "{{" + entry.getKey() + "}}";
            processed = processed.replace(key, entry.getValue() != null ? entry.getValue() : "");
        }
        return processed;
    }
}
