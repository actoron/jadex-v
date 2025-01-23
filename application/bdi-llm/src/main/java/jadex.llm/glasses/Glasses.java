package jadex.llm.glasses;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

class Glasses
{
    private int customerId;
    private String firstName;
    private String lastName;
    private String street;
    private int houseNumber;
    private int postalCode;
    private String city;
    private String dayOfBirth;
    private int articleNumber;
    private String brand;
    private int frameWidth;
    private int frameHeight;
    private String colour;

    public Glasses(int customerId, String firstName, String lastName, String street, int houseNumber, int postalCode, String city, String dayOfBirth, int articleNumber, String brand, int frameWidth, int frameHeight, String colour)
    {
        this.customerId = customerId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.street = street;
        this.houseNumber = houseNumber;
        this.postalCode = postalCode;
        this.city = city;
        this.dayOfBirth = dayOfBirth;
        this.articleNumber = articleNumber;
        this.brand = brand;
        this.frameWidth = frameWidth;
        this.frameHeight = frameHeight;
        this.colour = colour;
    }

    public JSONObject toJSONObject()
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
        json.put("articleNumber", articleNumber);
        json.put("brand", brand);
        json.put("frameWidth", frameWidth);
        json.put("frameHeight", frameHeight);
        json.put("colour", colour);
        return json;
    }

    public static List<Glasses> generateOpticanDB()
    {
        ArrayList<Glasses> customerList = new ArrayList<>();
        Set<Integer> articleNumbers = new HashSet<>();
        Set<Integer> customerIds = new HashSet<>();

        Random random = new Random();

        String[] firstNames = {"Alice", "Emma", "Olaf"};
        String[] lastNames = {"Meier", "Schmidt", "Braun"};
        String[] streets = {"Streetlane", "Lanestreet", "Avenue"};
        String[] cities = {"City1", "City2", "City3"};
        String[] brands = {"Brand1", "Brand2", "Brand3"};
        String[] colours = {"red", "black", "yellow"};


        for (int i = 0; i < 10; i++)
        {
            // Generate unique 4-digit customer ID
            int customerId;
            do
            {
                customerId = 1000 + random.nextInt(9000);
            } while (!customerIds.add(customerId));

            // Generate unique 6-digit article number
            int articleNumber;
            do
            {
                articleNumber = 100000 + random.nextInt(900000);
            } while (!articleNumbers.add(articleNumber));

            Glasses glasses = new Glasses(
                    customerId,
                    firstNames[random.nextInt(firstNames.length)],
                    lastNames[random.nextInt(lastNames.length)],
                    streets[random.nextInt(streets.length)],
                    random.nextInt(99) + 1, // Random house number 1-99
                    10000 + random.nextInt(90000), // Random postal code
                    cities[random.nextInt(cities.length)],
                    "199" + (random.nextInt(10) + 1) + "-0" + (random.nextInt(9) + 1) + "-" + (random.nextInt(28) + 1), // Random DOB in 1990s
                    articleNumber,
                    brands[random.nextInt(brands.length)],
                    130 + random.nextInt(50), // Frame width between 130-180
                    40 + random.nextInt(30),  // Frame height between 40-70
                    colours[random.nextInt(colours.length)]
            );

            customerList.add(glasses);
        }
        return customerList;
    }

//    public static void saveDataToJson()
//    {
//        String path = "application/bdi-llm/src/main/java/jadex.llm/glasses/Dataset1.json";
//        JSONArray jsonArray = new JSONArray();
//
//        for (Glasses glasses : customerList) {
//            jsonArray.add(glasses.toJSONObject());
//        }
//
//        try (FileWriter file = new FileWriter(path)) {
//            file.write(jsonArray.toString());
//            System.out.println("Dataset.json file created successfully!");
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }
}






