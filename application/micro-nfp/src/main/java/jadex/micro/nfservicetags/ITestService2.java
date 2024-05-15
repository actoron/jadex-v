package jadex.micro.nfservicetags;

import jadex.nfproperty.annotation.Tag;
import jadex.nfproperty.annotation.Tags;
import jadex.providedservice.annotation.Service;

@Service
//@Tags(value={"hello", "$component.getId().toString()", TagProperty.PLATFORM_NAME}, argumentname="tagarg")
@Tags({@Tag("hello"), @Tag("$component.getId().toString()"), @Tag("TagProperty.PLATFORM_NAME"), @Tag("$component.getArguments().get(\"tagarg\")")})
public interface ITestService2
{
}
