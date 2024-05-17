package jadex.micro.nfservicetags;

import jadex.providedservice.annotation.Service;
import jadex.providedservice.annotation.Tag;
import jadex.providedservice.annotation.Tags;

@Service
//@Tags(value={"hello", "$component.getId().toString()", TagProperty.PLATFORM_NAME}, argumentname="tagarg")
@Tags({@Tag("hello"), @Tag("$component.getId().toString()"), @Tag("TagProperty.HOST_NAME")}) // , @Tag("$component.getArguments().get(\"tagarg\")")
public interface ITestService2
{
}
