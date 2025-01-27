package jadex.llm.glasses;

import com.github.javafaker.Faker;
import jadex.common.IdGenerator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;

import java.time.LocalDateTime;

public class OpticiansDataGenerator
{
    private JSONObject dataset = new JSONObject();

    public OpticiansDataGenerator(int datasetLength)
    {
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String formattedNow = now.format(formatter);

        Random random = new Random();
        Faker faker = new Faker();
        IdGenerator articleNumber = new IdGenerator(6, true, null);

        String[] targetGroup = {"Men", "Women", "Children", "Athletic", "Gaming", "Unisex", "Sunglasses"};
        String[] shapes = {"Round", "Oval", "Square", "Rectangle", "Cat-eye", "Wayfarer",
                "Aviator", "Hexagon", "Browline", "Geometric", "Oversized",
                "Rimless", "Semi-Rimless", "Horn-Rimmed", "Diamond", "Triangle",
                "Clubmaster", "Wraparound", "Flat-top", "Gradient", "Retro",
                "Vintage", "Modern", "Sporty", "Classic", "Abstract", "Flat",
                "Deep", "Wide", "Narrow", "Tapered", "Elliptical", "Butterfly",
                "Circular", "Angular", "Minimalist", "Bold", "Soft", "Sharp",
                "Hybrid", "Fusion", "Dual-tone", "Half-moon", "D-frame",
                "Inverted Triangle", "Trapezoid", "Panto", "Tear-drop",
                "Artistic"};
        String[] faceShapes = {"Round", "Oval", "Square", "Heart", "Diamond", "Rectangle", "Triangle"};

        JSONArray opticansData = new JSONArray();

        for (int i = 0; i < datasetLength; i++)
        {
            JSONObject json = new JSONObject();
            json.put("articleNumber", articleNumber.generateId());
            json.put("modelName", faker.name().firstName());
            json.put("brand", faker.funnyName().name());
            json.put("material", faker.commerce().material());
            json.put("targetGroup", targetGroup[random.nextInt(targetGroup.length)]);
            json.put("frameWidth", 130 + random.nextInt(50)); // Frame width between 130-180
            json.put("frameHeight", 40 + random.nextInt(30)); // Frame height between 40-70
            json.put("colour", faker.color().name());
            json.put("shape", shapes[random.nextInt(shapes.length)]);
            json.put("faceShape", faceShapes[random.nextInt(faceShapes.length)]);
            json.put("price", faker.commerce().price());

            // Add the JSON object to the JSONArray
            opticansData.add(json);
        }
        // Add the JSONArray to the parent JSONObject
        dataset.put("GenerationDate",formattedNow);
        dataset.put("OpticansData", opticansData);
    }

    public void saveToJson(String savePath) {
        // Write to a JSON file
        try (FileWriter file = new FileWriter(savePath)) {
            file.write(dataset.toJSONString());
            System.out.println("Dataset created successfully at " + savePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String toJSONString() {
        return dataset.toJSONString();
    }

    public static void main(String[] args) throws IOException {
        if (Files.exists(Paths.get("application/bdi-llm/src/main/java/jadex.llm/glasses/Dataset.json")))
        {
            OpticiansDataGenerator dataGenerator = new OpticiansDataGenerator(10000);
            dataGenerator.saveToJson("application/bdi-llm/src/main/java/jadex.llm/glasses/Dataset.json");
        }
    }
}
