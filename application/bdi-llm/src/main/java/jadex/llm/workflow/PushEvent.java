package jadex.llm.workflow;

public class PushEvent
{
    private String projectname;

    private String projecturl;

    public String getProjectname() {
        return projectname;
    }

    public void setProjectname(String projectname) {
        this.projectname = projectname;
    }

    public String getProjecturl() {
        return projecturl;
    }

    public void setProjecturl(String projecturl) {
        this.projecturl = projecturl;
    }

    public String toString()
    {
        return "projectname: "+projectname+", projecturl: "+projecturl;
    }
}
