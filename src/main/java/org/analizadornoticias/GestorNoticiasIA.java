package org.analizadornoticias;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import org.analizadornoticias.modelo.Noticia;
import org.springframework.stereotype.Service;

@Service
public class GestorNoticiasIA {
    private static final String API_KEY = "AIzaSyDYQRHKHl1qAbXNPoax9rpGj_icr_yduJA";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1/models/gemini-2.0-flash:generateContent";
    private static final String PROMPT_COINCIDENCIAS="Eres un asistente que analiza noticias para detectar cuáles hablan sobre el mismo tema.  \n" +
            "Te daré una lista numerada de noticias.  \n" +
            "Tu tarea es devolver grupos de índices que correspondan a noticias que hablan sobre el mismo suceso o tema (por ejemplo: una dimisión, un accidente, una elección, etc.).\n" +
            "\n" +
            "\uD83D\uDC49 Instrucciones de salida:\n" +
            "- Devuelve SOLO un JSON válido, sin texto adicional ni explicaciones.\n" +
            "- Cada grupo debe ser una lista de números (índices).\n" +
            "- Si una noticia no está relacionada con ninguna otra, no la incluyas.\n" +
            "- No incluyas texto fuera del JSON.\n" +
            "\n" +
            "Ejemplo de formato de salida esperado:\n" +
            "```json\n" +
            "[[1, 2, 5], [3, 7, 9], [10, 12]]";


    public List<List<Integer>> buscaCoincidencias(List<Noticia> noticias){
        List<List<Integer>>coincidencias = new ArrayList<>();
        try {
            coincidencias = analizarNoticias(noticias);
        } catch (IOException e) {
            System.out.println("Error al analizar noticias");
        }
        return coincidencias;
    }
    public static List<List<Integer>> analizarNoticias(List<Noticia> noticias) throws IOException {
        Gson gson = new Gson();


        // Construimos el el mensaje que enviamos a la IA
        StringBuilder sb = new StringBuilder();
        sb.append(PROMPT_COINCIDENCIAS).append("\n\nNoticias:\n");

        for (int i = 0; i < noticias.size(); i++) {
            sb.append(noticias.get(i).getTitular()).append(". ").append(noticias.get(i).getTitular()).append("\n");
        }

        sb.append("\nDevuelve únicamente el JSON con los grupos de índices.");


        String respuesta = llamarGemini(sb.toString());

        // Extraemos el campo "text" del JSON que devuelve Gemini
        JsonObject jsonObject = gson.fromJson(respuesta, JsonObject.class);
        String textoRespuesta = jsonObject
                .getAsJsonArray("candidates")
                .get(0)
                .getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0)
                .getAsJsonObject()
                .get("text")
                .getAsString();

        // Buscar el JSON entre los corchetes [ ]
        int inicio = textoRespuesta.indexOf('[');
        int fin = textoRespuesta.lastIndexOf(']');

        String jsonLimpio = textoRespuesta.substring(inicio, fin + 1).trim();

        // Parsear con Gson
        List<List<Integer>> grupos = gson.fromJson(
                jsonLimpio,
                new TypeToken<List<List<Integer>>>(){}.getType()
        );

        return grupos;

    }


    private static String llamarGemini(String prompt) throws IOException {
        URL url = new URL(API_URL + "?key=" + API_KEY);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        conn.setDoOutput(true);

        String body = "{ \"contents\": [ { \"parts\": [ { \"text\": " + toJson(prompt) + " } ] } ] }";

        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        InputStream is = (conn.getResponseCode() < 400)
                ? conn.getInputStream()
                : conn.getErrorStream();

        BufferedReader br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) response.append(line);
        br.close();
        conn.disconnect();

        return response.toString();
    }


    // Escapa texto para enviarlo en formato JSON.

    private static String toJson(String text) {
        return "\"" + text.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

}
