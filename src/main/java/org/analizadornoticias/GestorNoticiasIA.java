package org.analizadornoticias;


import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import org.analizadornoticias.excepciones.AnalizadorNoticiasException;
import org.analizadornoticias.excepciones.ResumidorNoticiasException;
import org.analizadornoticias.modelo.Noticia;
import org.analizadornoticias.modelo.Resumen;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class GestorNoticiasIA {
    @Value("${gemini.api.key}")
    private String apiKey;
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
            "[[1, 2, 5], [3, 7, 9], [10, 12]]\n"+
            "No puede haber una lista de solo un id";
    private static final String PROMPT_RESUMENES =
            "Analiza las siguientes noticias, que tratan sobre el mismo tema.\n" +
                    "Crea un nuevo titular y un resumen objetivo y laico.\n" +
                    "Devuelve la respuesta ÚNICAMENTE en formato JSON con la siguiente estructura exacta:\n" +
                    "{ \"titular\": \"Nuevo titular\", \"cuerpo\": \"Resumen objetivo y laico.\" }\n" +
                    "No incluyas texto fuera del JSON.\n\n";


    public List<List<Integer>> buscaCoincidencias(List<Noticia> noticias) {
        List<List<Integer>>coincidencias = new ArrayList<>();
        try {
            coincidencias = analizarNoticias(noticias);
        } catch (IOException e) {
            throw new AnalizadorNoticiasException("Error al analizar noticias" +e.getMessage());
        }
        return coincidencias;
    }
    public List<List<Integer>> analizarNoticias(List<Noticia> noticias) throws IOException {
        Gson gson = new Gson();
        String jsonLimpio;
        String respuesta;
        int inicio;
        int fin;

        // Construimos el el mensaje que enviamos a la IA
        StringBuilder response=realizarPromptNoticia(noticias);


        respuesta = llamarGemini(response.toString());

        // Extraemos el campo "text" del JSON que devuelve Gemini
        JsonObject jsonObject = gson.fromJson(respuesta, JsonObject.class);
        System.out.println("Respuesta Gemini: " + respuesta);
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
        inicio = textoRespuesta.indexOf('[');
        fin = textoRespuesta.lastIndexOf(']');

        jsonLimpio = textoRespuesta.substring(inicio, fin + 1).trim();

        // Parsear con Gson
        //TypeToken es una clase de GSON para indicar a Java que tipo de clase cogeremos del JSON
        List<List<Integer>> grupos = gson.fromJson(
                jsonLimpio,
                new TypeToken<List<List<Integer>>>(){}.getType()
        );

        return grupos;
    }

    private static StringBuilder realizarPromptNoticia(List<Noticia> noticias) {
        StringBuilder sb = new StringBuilder();
        sb.append(PROMPT_COINCIDENCIAS).append("\n\nNoticias:\n");

        for (int i = 0; i < noticias.size(); i++) {
            sb.append(noticias.get(i).getTitular()).append(". ").append(noticias.get(i).getTitular()).append("\n");
        }

        sb.append("\nDevuelve únicamente el JSON con los grupos de índices.");
        return sb;
    }

    public List<Resumen> resumidorNoticias(List<List<Integer>> coincidencias, List<Noticia> noticias) {
        return resumirNoticias(coincidencias, noticias);
    }

    private List<Resumen> resumirNoticias(List<List<Integer>> coincidencias, List<Noticia> noticias) {
        List<Resumen> resumenes = new ArrayList<>();
        obtenerResumenes(resumenes,coincidencias,noticias);
        return resumenes;
    }

    private void obtenerResumenes(List<Resumen> resumenes, List<List<Integer>> coincidencias, List<Noticia> noticias) {
        String respuesta;
        for (List<Integer> grupo : coincidencias) {
            StringBuilder prompt = new StringBuilder(PROMPT_RESUMENES);
            realizarPromptResumenes(noticias, grupo, prompt);
            try {
                respuesta = llamarGemini(prompt.toString());
                JsonObject root = JsonParser.parseString(respuesta).getAsJsonObject();
                JsonArray candidates = root.getAsJsonArray("candidates");
                resumenes.add(obtenerResumen(candidates));
            } catch (IOException e) {
                throw new ResumidorNoticiasException("Error al resumir una noticia:" +e.getMessage());
            }
        }
    }

    private void realizarPromptResumenes(List<Noticia> noticias, List<Integer> grupo, StringBuilder prompt) {
        prompt.append("Noticias:\n");
        for (Integer idNoticia : grupo) {
            if (idNoticia <= noticias.size()) {
                Noticia n = noticias.get(idNoticia - 1);
                prompt.append("Titular: ").append(n.getTitular()).append("\n");
                prompt.append("Cuerpo: ").append(n.getContenido()).append("\n\n");
            }
        }
    }

    private Resumen obtenerResumen(JsonArray candidates) {
        Gson gson = new Gson();
        Resumen resumen = new Resumen();
        if (candidates != null && !candidates.isEmpty()) {
            JsonObject content = candidates.get(0).getAsJsonObject().getAsJsonObject("content");
            JsonArray parts = content.getAsJsonArray("parts");
            String texto = parts.get(0).getAsJsonObject().get("text").getAsString();

            // El texto contiene:
            // ```json\n{ "titular": "x", "cuerpo": "y" }\n```
            // Así que limpiamos las comillas y saltos de línea extra
            String jsonLimpio = texto.replaceAll("```json", "")
                    .replaceAll("```", "")
                    .trim();

            // Ahora parseamos el JSON limpio
            resumen = gson.fromJson(jsonLimpio, Resumen.class);

        }
        return resumen;
    }


    private String llamarGemini(String prompt) throws IOException {
        InputStream is;
        BufferedReader br;
        StringBuilder response;
        URL url = new URL(API_URL + "?key=" + apiKey);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        //Le digo que le envio un JSON EN UTF-8
        conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        //Indica al servidor que se le va a enviar datos
        conn.setDoOutput(true);

        String body = "{ \"contents\": [ { \"parts\": [ { \"text\": " + toJson(prompt) + " } ] } ] }";

        //Envia el body al servidor, el metodo getOutputStream te devuelve un flujo preparado para enviar info al servidor
        mandarDatosGemini(body,conn);

        //Verifica que no es un codigo de error si lo es coge otro canal donde ver el error
        is=obtenerCanalRespuestaServidor(conn);

        br = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        response= obtenerRespuestaGemini(br);

        br.close();
        conn.disconnect();

        return response.toString();
    }

    private static StringBuilder obtenerRespuestaGemini(BufferedReader br) throws IOException {
        StringBuilder response= new StringBuilder();
        String linea;
        try{
            while ((linea = br.readLine()) != null) {
                response.append(linea);
            }
        }catch(IOException e){
            throw new IOException("Error al leer respuesta de gemini");
        }
        return response;
    }

    private static InputStream obtenerCanalRespuestaServidor(HttpURLConnection conn) throws IOException {
        try {
            if(conn.getResponseCode() < 400){
                return conn.getInputStream();
            }else {
                return conn.getErrorStream();
            }
        }catch (IOException e){
           throw new IOException("Error al obtener un canal de respuesta del servidor");
        }
    }

    private static void mandarDatosGemini(String body, HttpURLConnection conn) throws IOException {
        try (OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }catch (IOException e) {
            throw new IOException("Error al enviar datos a Gemini");
        }
    }


    // Escapa texto para enviarlo en formato JSON.

    private static String toJson(String text) {
        return "\"" + text.replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

}
