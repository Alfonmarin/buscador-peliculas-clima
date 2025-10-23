package es.upm.filmrecommender.data; 



public class UserPreferences { 

    private String genre;
    private double minimumRating;

    public UserPreferences(String genre, double minimumRating) {
        this.genre = genre;
        this.minimumRating = minimumRating;
    }


    public String getGenre() {
        return genre;
    }

    public double getMinimumRating() {
        return minimumRating;
    }

    public void setGenre(String genre) {
        this.genre = genre;
    }

    public void setMinimumRating(double minimumRating) {
        this.minimumRating = minimumRating;
    }


    public UserPreferences() {
    }

    @Override
    public String toString() {
        return "UserPreferences{" +
               "genre='" + genre + '\'' +
               ", minimumRating=" + minimumRating +
               '}';
    }
}