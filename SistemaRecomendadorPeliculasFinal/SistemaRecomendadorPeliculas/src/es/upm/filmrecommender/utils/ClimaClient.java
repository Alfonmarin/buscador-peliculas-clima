package es.upm.filmrecommender.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class ClimaClient {

    private static final String API_KEY = "4431bc2a71507645eddb568a26b9f260";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/weather";

    public static String obtenerTipoClimaMadrid() {
        String ciudad = "Madrid";
        String endpoint = BASE_URL + "?q=" + ciudad + "&appid=" + API_KEY + "&units=metric&lang=es";
        try {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int codigoRespuesta = conn.getResponseCode();
            if (codigoRespuesta != 200) {
                return "unknown";
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder respuesta = new StringBuilder();
                String linea;
                while ((linea = reader.readLine()) != null) {
                    respuesta.append(linea);
                }

                String json = respuesta.toString();
                return extraerCampo(json, "\"main\":\"", "\"");

            }
        } catch (Exception e) {
            return "unknown";
        }
    }
    
    public static String obtenerFraseClimaMadrid() {
        String ciudad = "Madrid";
        String endpoint = BASE_URL + "?q=" + ciudad + "&appid=" + API_KEY + "&units=metric&lang=es";
        try {
            URL url = new URL(endpoint);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            int codigoRespuesta = conn.getResponseCode();
            if (codigoRespuesta != 200) {
                return "Error al obtener el clima: código " + codigoRespuesta;
            }

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                StringBuilder respuesta = new StringBuilder();
                String linea;
                while ((linea = reader.readLine()) != null) {
                    respuesta.append(linea);
                }

                String json = respuesta.toString();

                String descripcion = extraerCampo(json, "\"description\":\"", "\"");
                String temperatura = extraerCampo(json, "\"temp\":", ",");

                if (descripcion != null && temperatura != null) {
                    return "El clima actual en Madrid es: " + descripcion + " (" + temperatura + "°C)"+"\n"+"\n"+"--- Películas Según el clima actual ---";
                } else {
                    return "No se pudo interpretar la respuesta del clima.";
                }
            }
        } catch (Exception e) {
            return "Error al conectar con el servicio de clima: " + e.getMessage();
        }
    }
    
    private static String extraerCampo(String json, String inicio, String fin) {
        int indexInicio = json.indexOf(inicio);
        if (indexInicio == -1) return null;
        indexInicio += inicio.length();
        int indexFin = json.indexOf(fin, indexInicio);
        if (indexFin == -1) return null;
        return json.substring(indexInicio, indexFin);
    }
}