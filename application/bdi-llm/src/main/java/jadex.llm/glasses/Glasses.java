package jadex.llm.glasses;

import org.json.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

class Glasses
{
    private int articleNumber;
    private String brand;
    private int frameWidth;
    private int frameHeight;
    private String colour;

    public Glasses(int articleNumber, String brand, int frameWidth, int frameHeight, String colour)
    {
        this.articleNumber = articleNumber;
        this.brand = brand;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.colour = colour;
    }

    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        json.put("articleNumber", articleNumber);
        json.put("brand", brand);
        json.put("frameWidth", frameWidth);
        json.put("frameHeight", frameHeight);
        json.put("colour", colour);
        return json;
    }

    public String toString()
    {
        return "Glasses{" +
                "articleNumber=" + articleNumber +
                ", brand='" + brand + '\'' +
                ", frameWidth=" + frameWidth +
                ", frameHeight=" + frameHeight +
                ", colour='" + colour + '\'' +
                '}';
    }
}

class Customer
{
    private int customerId;
    private String firstName;
    private String lastName;
    private String street;
    private int houseNumber;
    private int postalCode;
    private String city;
    private String dayOfBirth;
    private Glasses glasses;

    public Customer(int customerId, String firstName, String lastName, String street, int houseNumber, int postalCode, String city, String dayOfBirth, Glasses glasses)
    {
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.street = street;
        this.houseNumber = houseNumber;
        this.postalCode = postalCode;
        this.city = city;
        this.dayOfBirth = dayOfBirth;
        this.glasses = glasses;
    }


    public JSONObject toJSON()
    {
        JSONObject json = new JSONObject();
        json.put("customerId", customerId);
        json.put("firstName", firstName);
        json.put("lastName", lastName);
        json.put("street", street);
        json.put("houseNumber", houseNumber);
        json.put("postalCode", postalCode);
        json.put("city", city);
        json.put("dayOfBirth", dayOfBirth);
        json.put("glasses", glasses.toJSON());
        return json;
    }

    public String toString()
    {
        return "Customer{" +
                "customerId=" + customerId +
                ", firstName='" + firstName + '\'' +
                ", lastName='" + lastName + '\'' +
                ", street='" + street + '\'' +
                ", houseNumber=" + houseNumber +
                ", postalCode=" + postalCode +
                ", city='" + city + '\'' +
                ", dayOfBirth='" + dayOfBirth + '\'' +
                ", glasses=" + glasses +
                '}';
    }

    static List<Glasses> generateRandomGlassesList()
    {
        ArrayList<Glasses> glassesList = new ArrayList<>();
        Set<Integer> articleNumber = new HashSet<>();

        Random random = new Random();

        String[] brands = {"Brand1", "Brand2", "Brand3"};
        String[] colours = {"red", "black", "yellow"};

        for (int i = 0; i < 20; i++)
        {
            int articleNumberGen;
            do
            {
                articleNumberGen = 100000 + random.nextInt(900000); // Generate unique 6-digit article number
            } while (!articleNumber.add(articleNumberGen));

            Glasses glasses = new Glasses(
                    articleNumberGen,
                    brands[random.nextInt(brands.length)],
                    130 + random.nextInt(50), // Frame width between 130-180
                    40 + random.nextInt(30),  // Frame height between 40-70
                    colours[random.nextInt(colours.length)]
            );

            glassesList.add(glasses);
        }
        return glassesList;
    }

    public static void generateDatasetJSON(List<Glasses> glassesList)
    {
        JSONArray glassesArray = new JSONArray();
        for (Glasses glasses : glassesList)
        {
            glassesArray.put(glasses.toJSON());
        }
        try (FileWriter file = new FileWriter("Dataset.json"))
        {
            try
            {
                file.write(glassesArray.toString());
            } catch (IOException e)
            {
                throw new RuntimeException(e);
            }
            file.flush();
            System.out.println("Dataset.json created");
        } catch (IOException e)
        {
            System.out.println("An error occurred.");
        }
    }
}



