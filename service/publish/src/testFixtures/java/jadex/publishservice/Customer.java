package jadex.publishservice;

public class Customer 
{
    protected String name;

    protected int age;

    protected String address;

    public Customer() 
    {
    }

    public Customer(String name, int age, String address) 
    {
        this.name = name;
        this.age = age;
        this.address = address;
    }

    public String getName() 
    {
        return name;
    }

    public void setName(String name) 
    {
        this.name = name;
    }

    public int getAge() 
    {
        return age;
    }

    public void setAge(int age) 
    {
        this.age = age;
    }

    public String getAddress() 
    {
        return address;
    }

    public void setAddress(String address) 
    {
        this.address = address;
    }
    
    @Override
    public boolean equals(Object obj) 
    {
        if(this == obj)
            return true;
        if(obj == null || getClass() != obj.getClass())
            return false;
        Customer other = (Customer) obj;
        return age == other.age &&
               (name != null ? name.equals(other.name) : other.name == null) &&
               (address != null ? address.equals(other.address) : other.address == null);
    }   

    @Override
    public int hashCode() 
    {
        int result = (name != null) ? name.hashCode() : 0;
        result = 31 * result + age;
        result = 31 * result + ((address != null) ? address.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() 
    {
        return "Customer [name=" + name + ", age=" + age + ", address=" + address + "]";
    }
}
