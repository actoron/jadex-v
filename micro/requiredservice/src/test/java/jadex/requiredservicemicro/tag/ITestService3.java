package jadex.requiredservicemicro.tag;

import jadex.providedservice.annotation.Service;
import jadex.providedservice.annotation.Tag;

@Service
//@Tags({@Tag(value="in", include="true"), @Tag(value="out", include="false")})

@Tag("$args.testarg") // for public field
//@Tag("$args.getTestarg()") // for public getter
@Tag("testarg")
public interface ITestService3
{
}
