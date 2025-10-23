package es.upm.filmrecommender.data;

import java.io.Serializable;
import java.util.List;

public class Movie implements Serializable {
    private static final long serialVersionUID = 1L; 

    private int id;
    private String title;
    private String overview;
    private List<String> genres; 
    private double voteAverage;
    private String posterPath; 
    private List<String> cast;
    private String director;

    
    public Movie(int id, String title, String overview, List<String> genres, double voteAverage, String posterPath,
            List<String> cast, String director) {
   this.id = id;
   this.title = title;
   this.overview = overview;
   this.genres = genres;
   this.voteAverage = voteAverage;
   this.posterPath = posterPath;
   this.cast = cast;
   this.director = director;
}

    
    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getOverview() { return overview; }
    public List<String> getGenres() { return genres; }
    public double getVoteAverage() { return voteAverage; }
    public String getPosterPath() { return posterPath; } 
    public List<String> getCast() { return cast; }
    public String getDirector() { return director; }

    @Override
    public String toString() {
        return "Movie{" +
               "id=" + id +
               ", title='" + title + '\'' +
               ", genres=" + genres +
               ", voteAverage=" + voteAverage +
               '}';
    }
}
