package jadex.micro.nfservicetags;

import jadex.nfproperty.annotation.Tag;
import jadex.nfproperty.annotation.Tags;
import jadex.providedservice.annotation.Service;

@Service
@Tags({@Tag(value="in", include="true"), @Tag(value="out", include="false")})
public interface ITestService3
{
}
