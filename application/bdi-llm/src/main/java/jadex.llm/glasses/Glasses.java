package jadex.llm.glasses;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Glasses
{
    private String model;
    private String shape;
    private String material;
    private String brand;
    private String colour;

    public Glasses(String model, String shape, String material, String brand, String colour)
    {
        this.model = model;
        this.shape = shape;
        this.material = material;
        this.brand = brand;
        this.colour = colour;
    }

    static List<Glasses> generateRandomGlassesList()
    {
        ArrayList<Glasses> glassesList = new ArrayList<>();
        Random random = new Random();

        String[] shapes = {"Round", "Square", "Oval"};
        String[] materials = {"plastic", "metal", "no"};
        String[] brands = {"Brand1", "Brand2", "Brand3"};
        String[] colours = {"red", "black", "yellow"};

        for (int i = 0; i < 20; i++)
        {
            String model = "Model" + (i + 1);
            String shape = shapes[random.nextInt(shapes.length)];
            String material = materials[random.nextInt(materials.length)];
            String brand = brands[random.nextInt(brands.length)];
            String colour = colours[random.nextInt(colours.length)];

            glassesList.add(new Glasses(model, shape, material, brand, colour));
        }

        for (Glasses glasses : glassesList)
        {
            //System.out.println(glasses);
        }
        return glassesList;
    }

    // Getter methods
    public String getModel()
    {
        return model;
    }

    public String getShape()
    {
        return shape;
    }

    public String getMaterial()
    {
        return material;
    }

    public String getBrand()
    {
        return brand;
    }

    public String getColour()
    {
        return colour;
    }

    @Override
    public String toString()
    {
        return "Glasses{" +
                "model= " + model +
                ", shape= " + shape +
                ", material= " + material +
                ", brand= " + brand +
                ", colour= " + colour +
                "}";
    }
}
