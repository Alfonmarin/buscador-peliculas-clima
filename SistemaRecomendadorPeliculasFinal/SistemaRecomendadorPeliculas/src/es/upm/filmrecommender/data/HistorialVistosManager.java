package es.upm.filmrecommender.data;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class HistorialVistosManager {

    private static final String FILENAME = "vistos.json";
    private static final Gson gson = new Gson();


    public static void guardarPeliculaVista(Movie pelicula) {
        List<Movie> actuales = cargarPeliculasVistas();
        if (!actuales.stream().anyMatch(p -> p.getId() == pelicula.getId())) {
            actuales.add(pelicula);
            try (Writer writer = new FileWriter(FILENAME)) {
                gson.toJson(actuales, writer);
                System.out.println("Historial: Película guardada: " + pelicula.getTitle());
            } catch (IOException e) {
                System.err.println("Error guardando historial de películas vistas: " + e.getMessage());
            }
        }
    }

    public static List<Movie> cargarPeliculasVistas() {
        File archivo = new File(FILENAME);
        if (!archivo.exists()) return new ArrayList<>();

        try (Reader reader = new FileReader(FILENAME)) {
            Type listType = new TypeToken<ArrayList<Movie>>() {}.getType();
            List<Movie> vistas = gson.fromJson(reader, listType);
            return vistas != null ? vistas : new ArrayList<>();

        } catch (IOException e) {
            System.err.println("Error leyendo historial de películas vistas: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
