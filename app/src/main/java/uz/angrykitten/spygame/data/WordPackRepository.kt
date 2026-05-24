package uz.angrykitten.spygame.data

import uz.angrykitten.spygame.model.WordPack
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WordPackRepository @Inject constructor() {

    fun getAvailablePacks(): List<WordPack> = listOf(
        getLocationsPack(),
        getFoodDrinkPack(),
        getSportsPack(),
        getMoviesPack()
    )

    fun getLocationsPack() = WordPack(
        name = "Locations",
        words = listOf(
            "Airport", "Bank", "Beach", "Casino", "Church",
            "Circus", "Corporate Party", "Crusader Army", "Day Spa", "Embassy",
            "Hospital", "Hotel", "Military Base", "Movie Studio", "Ocean Liner",
            "Passenger Train", "Pirate Ship", "Polar Station", "Police Station", "Restaurant",
            "School", "Service Station", "Space Station", "Submarine", "Supermarket",
            "Theater", "University", "World Cup", "Zoo", "Amusement Park"
        )
    )

    fun getFoodDrinkPack() = WordPack(
        name = "Food & Drink",
        words = listOf(
            "Pizza", "Sushi", "Burger", "Tacos", "Pasta",
            "Ice Cream", "Coffee", "Chocolate", "Steak", "Salad",
            "Smoothie", "Donut", "Sandwich", "Curry", "Ramen",
            "Pancakes", "Waffles", "Cheesecake", "Milkshake", "Barbeque"
        )
    )

    fun getSportsPack() = WordPack(
        name = "Sports",
        words = listOf(
            "Soccer", "Basketball", "Tennis", "Swimming", "Boxing",
            "Cricket", "Golf", "Hockey", "Baseball", "Volleyball",
            "Rugby", "Skiing", "Surfing", "Wrestling", "Fencing",
            "Archery", "Gymnastics", "Cycling", "Marathon", "Formula 1"
        )
    )

    fun getMoviesPack() = WordPack(
        name = "Movies",
        words = listOf(
            "Titanic", "Avatar", "Star Wars", "The Matrix", "Inception",
            "Jurassic Park", "The Godfather", "Forrest Gump", "The Lion King", "Gladiator",
            "Harry Potter", "Lord of the Rings", "Spider-Man", "Batman", "James Bond",
            "Mission Impossible", "Fast and Furious", "Toy Story", "Frozen", "Avengers"
        )
    )

    fun createCustomPack(name: String, words: List<String>) = WordPack(
        name = name,
        words = words,
        isCustom = true
    )
}
