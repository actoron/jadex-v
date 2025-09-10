package jadex.llm.workflow;

public class PushEvent
{
    private String projectname;

    private String projecturl;

    public PushEvent() {

    }

    public String getProjectName() {
        return projectname;
    }

    public void setProjectName(String projectname) {
        this.projectname = projectname;
    }

    public String getProjectUrl() {
        return projecturl;
    }

    public void setProjectUrl(String projecturl) {
        this.projecturl = projecturl;
    }

    public String toString()
    {
        return "projectname: "+projectname+", projecturl: "+projecturl;
    }
}
