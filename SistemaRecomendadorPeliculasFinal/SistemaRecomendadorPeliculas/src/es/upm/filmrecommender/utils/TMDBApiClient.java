package es.upm.filmrecommender.utils;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import es.upm.filmrecommender.data.Movie;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TMDBApiClient {
    private static final String API_KEY = "f380475a86804381f15a39e16a086e8f";
    private static final String BASE_URL = "https://api.themoviedb.org/3";
    private static final String IMAGE_BASE_URL = "https://image.tmdb.org/t/p/w500";
    private static final String LANGUAGE_PARAM = "&language=es-ES";

    private Gson gson;
    private Map<String, Integer> genreNameToIdMap;
    private Map<Integer, String> genreIdToNameMap;
    private DecimalFormat ratingFormatter;

    private static class GenreApiResponse { List<Genre> genres; }
    private static class Genre { int id; String name; }

    public TMDBApiClient() {
        this.gson = new Gson();
        this.genreNameToIdMap = new HashMap<>();
        this.genreIdToNameMap = new HashMap<>();
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.US);
        this.ratingFormatter = new DecimalFormat("#0.0", symbols);
        fetchAndCacheGenres();
    }

    private String normalizeGenreName(String name) {
        if (name == null) return ""; 
        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        normalized = normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return normalized.toLowerCase().trim();
    }

    private void fetchAndCacheGenres() {
        String endpoint = BASE_URL + "/genre/movie/list?api_key=" + API_KEY + LANGUAGE_PARAM;
        System.out.println("TMDBApiClient: Solicitando lista de géneros desde: " + endpoint);
        try {
            HttpURLConnection conn = createConnection(endpoint);
            if (conn.getResponseCode() != 200) {
                System.err.println("Error fetching genres from TMDB: " + conn.getResponseCode() + " " + conn.getResponseMessage());
                logApiErrorDetails(conn);
                return;
            }
            String response = readResponse(conn);
            GenreApiResponse genreResponse = gson.fromJson(response, GenreApiResponse.class);
            if (genreResponse != null && genreResponse.genres != null) {
                System.out.println("TMDBApiClient: === GÉNEROS CARGADOS Y CACHEADOS ===");
                for (Genre genre : genreResponse.genres) {
                    String normalizedName = normalizeGenreName(genre.name);
                    genreNameToIdMap.put(normalizedName, genre.id);
                    genreIdToNameMap.put(genre.id, genre.name); 
                    System.out.println("ID: " + genre.id + ", Nombre Original: \"" + genre.name + "\", Normalizado para Mapa: \"" + normalizedName + "\"");
                }
                System.out.println("TMDBApiClient: Total de géneros cargados: " + genreIdToNameMap.size() + "\n====================================");
            } else {
                 System.err.println("TMDBApiClient: No se recibieron géneros o la respuesta fue malformada.");
            }
        } catch (Exception e) {
            System.err.println("TMDBApiClient: Excepción al cargar géneros.");
            e.printStackTrace();
        }
    }

    public List<Movie> searchMoviesByGenre(String genreNameInput, double minRating, int maxResults) {
        List<Movie> movies = new ArrayList<>();
        if (genreNameInput == null || genreNameInput.trim().isEmpty()) {
            System.out.println("TMDBApiClient: Nombre de género vacío, obteniendo populares con rating >= " + minRating);
            return getPopularMovies(minRating, maxResults);
        }

        String normalizedQueryGenreName = normalizeGenreName(genreNameInput);
        Integer genreId = genreNameToIdMap.get(normalizedQueryGenreName);

        if (genreId == null) {
            System.out.println("TMDBApiClient: Género Normalizado '" + normalizedQueryGenreName + "' (Original: '"+genreNameInput+"') no encontrado. Usando búsqueda por palabra clave.");
            return searchMoviesByKeyword(genreNameInput, minRating, maxResults);
        }

        String endpoint = BASE_URL + "/discover/movie?api_key=" + API_KEY + LANGUAGE_PARAM +
                          "&with_genres=" + genreId + "&sort_by=popularity.desc";
        if (minRating > 0.0) {
            endpoint += "&vote_average.gte=" + ratingFormatter.format(minRating);
        }
        
        System.out.println("TMDBApiClient: Buscando por Género. Endpoint: " + endpoint);
        try {
            HttpURLConnection conn = createConnection(endpoint);
            if (conn.getResponseCode() != 200) {
                System.err.println("Error fetching movies by genre from TMDB: " + conn.getResponseCode());
                logApiErrorDetails(conn); return movies;
            }
            String response = readResponse(conn);
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            JsonArray results = jsonResponse.getAsJsonArray("results");
            movies = parseMovieResults(results);
            if (movies.size() > maxResults && maxResults > 0) {
                return movies.subList(0, maxResults);
            }
        } catch (Exception e) {
            System.err.println("TMDBApiClient: Excepción al buscar películas por género: " + e.getMessage());
            e.printStackTrace();
        }
        return movies;
    }

    public List<Movie> searchMoviesByKeyword(String keyword, double minRating, int maxResults) {
        List<Movie> movies = new ArrayList<>();
         if (keyword == null || keyword.trim().isEmpty()) {
            System.out.println("TMDBApiClient: Palabra clave vacía, obteniendo populares con rating >= " + minRating);
            return getPopularMovies(minRating, maxResults);
        }
        try {
            String queryParam = java.net.URLEncoder.encode(keyword, StandardCharsets.UTF_8.toString());
            String endpoint = BASE_URL + "/search/movie?api_key=" + API_KEY + LANGUAGE_PARAM + "&query=" + queryParam;
            
            System.out.println("TMDBApiClient: Buscando por Palabra Clave. Endpoint: " + endpoint);
            HttpURLConnection conn = createConnection(endpoint);
            if (conn.getResponseCode() != 200) {
                System.err.println("Error fetching movies by keyword from TMDB: " + conn.getResponseCode());
                logApiErrorDetails(conn); return movies;
            }
            String response = readResponse(conn);
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            JsonArray results = jsonResponse.getAsJsonArray("results");
            List<Movie> fetchedMovies = parseMovieResults(results);

            if (minRating > 0.0) {
                List<Movie> postFilteredMovies = new ArrayList<>();
                for (Movie movie : fetchedMovies) {
                    if (movie.getVoteAverage() >= minRating) {
                        postFilteredMovies.add(movie);
                    }
                }
                movies = postFilteredMovies;
                System.out.println("TMDBApiClient: Post-filtro por rating (>= " + minRating + ") aplicado a búsqueda por palabra clave. Originales: " + (results != null ? results.size() : 0) + ", Filtradas: " + movies.size());
            } else {
                movies = fetchedMovies;
            }

            if (movies.size() > maxResults && maxResults > 0) {
                return movies.subList(0, maxResults);
            }
        } catch (Exception e) {
            System.err.println("TMDBApiClient: Excepción al buscar películas por palabra clave: " + e.getMessage());
            e.printStackTrace();
        }
        return movies;
    }
    
    public List<Movie> getPopularMovies(double minRating, int maxResults) {
        List<Movie> movies = new ArrayList<>();
        String endpoint = BASE_URL + "/movie/popular?api_key=" + API_KEY + LANGUAGE_PARAM;
        if (minRating > 0.0) {
            endpoint += "&vote_average.gte=" + ratingFormatter.format(minRating);
        }
        System.out.println("TMDBApiClient: Obteniendo Populares. Endpoint: " + endpoint);
        try {
            HttpURLConnection conn = createConnection(endpoint);
            if (conn.getResponseCode() != 200) {
                System.err.println("Error fetching popular movies from TMDB: " + conn.getResponseCode());
                logApiErrorDetails(conn); return movies;
            }
            String response = readResponse(conn);
            JsonObject jsonResponse = gson.fromJson(response, JsonObject.class);
            JsonArray results = jsonResponse.getAsJsonArray("results");
            movies = parseMovieResults(results);
            if (movies.size() > maxResults && maxResults > 0) {
                return movies.subList(0, maxResults);
            }
        } catch (Exception e) {
            System.err.println("TMDBApiClient: Excepción al obtener películas populares: " + e.getMessage());
            e.printStackTrace();
        }
        return movies;
    }

    private HttpURLConnection createConnection(String endpoint) throws Exception {
        URL url = new URL(endpoint);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    private String readResponse(HttpURLConnection conn) throws Exception {
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }
        return response.toString();
    }
    
    private void logApiErrorDetails(HttpURLConnection conn) {
        if (conn == null) return;
        try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
             String errorLine;
             System.err.println("TMDB API Error Body:");
             while ((errorLine = br.readLine()) != null) {
                 System.err.println(errorLine);
             }
        } catch (Exception e) {
            System.err.println("TMDBApiClient: No se pudo leer el cuerpo del error de la API. Detalle: " + e.getMessage());
        }
    }

    private List<Movie> parseMovieResults(JsonArray results) {
        List<Movie> movies = new ArrayList<>();
        if (results == null) {
            System.out.println("TMDBApiClient: No se encontraron resultados (JsonArray nulo) para parsear.");
            return movies;
        }
        for (JsonElement element : results) {
            JsonObject movieJson = element.getAsJsonObject();
            if (!movieJson.has("id") || !movieJson.has("title") || !movieJson.has("overview") || !movieJson.has("vote_average")) {
                System.err.println("TMDBApiClient: Película con datos incompletos omitida: " + movieJson.toString());
                continue;
            }

            int id = movieJson.get("id").getAsInt();
            String title = movieJson.get("title").getAsString();
            String overview = movieJson.get("overview").getAsString();
            double voteAverage = movieJson.get("vote_average").getAsDouble();
            String posterPath = movieJson.has("poster_path") && !movieJson.get("poster_path").isJsonNull() ?
                                IMAGE_BASE_URL + movieJson.get("poster_path").getAsString() : null;

            List<String> movieGenreNames = new ArrayList<>();
            if (movieJson.has("genre_ids") && movieJson.get("genre_ids").isJsonArray()) {
                JsonArray genreIdsJson = movieJson.getAsJsonArray("genre_ids");
                for (JsonElement genreIdEl : genreIdsJson) {
                    if (genreIdEl.isJsonPrimitive() && genreIdEl.getAsJsonPrimitive().isNumber()){
                        int genreId = genreIdEl.getAsInt();
                        String genreName = genreIdToNameMap.get(genreId);
                        if (genreName != null) {
                            movieGenreNames.add(genreName);
                        }
                    }
                }
            }
            movies.add(new Movie(id, title, overview, movieGenreNames, voteAverage, posterPath, new ArrayList<>(), ""));
        }
        return movies;
    }
}