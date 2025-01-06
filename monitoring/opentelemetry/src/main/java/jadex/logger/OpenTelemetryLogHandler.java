package jadex.logger;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.TimeUnit;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.common.AttributesBuilder;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.common.CompletableResultCode;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import jadex.common.SUtil;
import jadex.core.IComponentManager;
import jadex.core.impl.GlobalProcessIdentifier;

public class OpenTelemetryLogHandler extends Handler 
{
    //private final Logger logger;

	private String loggername;
	
    private static OpenTelemetrySdk otel = null;
    
    private static OpenTelemetrySdk getOrInitializeOpenTelemetry() 
    {
        if (otel == null) 
        {
            synchronized(OpenTelemetryLogHandler.class) 
            {
                if(otel == null) 
                	otel = initializeOpenTelemetry();
            }
        }
        return otel;
    }
    
    private static OpenTelemetrySdk initializeOpenTelemetry() 
    {
        String url = System.getenv(OpenTelemetryLogger.URL)!=null? System.getenv(OpenTelemetryLogger.URL): System.getProperty(OpenTelemetryLogger.URL);
        String key = System.getenv(OpenTelemetryLogger.KEY)!=null? System.getenv(OpenTelemetryLogger.KEY): System.getProperty(OpenTelemetryLogger.KEY);

        OtlpGrpcLogRecordExporter exporter = null;
        OtlpGrpcLogRecordExporterBuilder builder = OtlpGrpcLogRecordExporter.builder();
        if(url != null) 
        	builder.setEndpoint(url);
        if(key != null)
        	builder.addHeader("x-api-key", key);
        exporter = builder.build();
        
        BatchLogRecordProcessor processor = BatchLogRecordProcessor.builder(exporter).build();
        
        // Ressource attributes
        AttributesBuilder	attrs	= Attributes.builder();
        
        // should only use attributes as index labels that do not change for same application
        // cf. Loki label best practices https://grafana.com/docs/loki/latest/get-started/labels/bp-labels/
        //attrs.put(AttributeKey.stringKey("service.name"),  GlobalProcessIdentifier.SELF.toString());
        attrs.put(AttributeKey.stringKey("service.name"),  GlobalProcessIdentifier.SELF.host().toString());
        attrs.put(AttributeKey.booleanKey("runtime.benchmark"), System.getProperty("user.dir").contains("benchmark"));
        attrs.put(AttributeKey.booleanKey("runtime.gradle"),  SUtil.isGradle());
//        attrs.put(AttributeKey.stringKey("runtime.env"), ""+System.getenv());
//        attrs.put(AttributeKey.stringKey("runtime.props"), ""+System.getProperties());

        attrs.put(AttributeKey.stringKey("application.name"), IComponentManager.get().getFirstPojoClassName());

        SdkLoggerProvider provider = SdkLoggerProvider.builder().addLogRecordProcessor(processor)
        	.setResource(Resource.create(attrs.build())).build();
        
        OpenTelemetrySdkBuilder otelb = OpenTelemetrySdk.builder();
        otelb.setLoggerProvider(provider);
        System.out.println("otel init: "+url+" "+key);
        return otelb.buildAndRegisterGlobal();
    }
    
    public OpenTelemetryLogHandler(String loggername) 
    {
        //this.logger = getOrInitializeOpenTelemetry().getLogsBridge().get(loggername);
    	this.loggername = loggername;
    }
    
    public Logger getLogger(String loggername)
    {
    	return getOrInitializeOpenTelemetry().getLogsBridge().get(loggername);
    }

    @Override
    public void publish(LogRecord record) 
    {
    	// todo? return respecting loglevel 
    	
    	if (record == null || record.getMessage() == null) 
    		return;
    	
    	Instant instant = Instant.ofEpochMilli(record.getMillis());
    	String isotime = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(instant);

        LogRecordBuilder builder = getLogger(loggername).logRecordBuilder()
            .setBody(record.getMessage())
        	.setSeverity(mapJulLevelToOtelSeverity(record.getLevel())) 
           	.setSeverityText(record.getLevel().getName())
        	.setAttribute(AttributeKey.stringKey("logger.name"), record.getLoggerName())
        	// scope/log attributes can't be used as index labels, grrr.
        	//.setAttribute(AttributeKey.stringKey("logger.host"), GlobalProcessIdentifier.SELF.host().toString())
            //.setAttribute(AttributeKey.stringKey("thread.name"), Thread.currentThread().getName())
            //.setAttribute(AttributeKey.stringKey("service.name"), "my_java_service")
            .setAttribute(AttributeKey.stringKey("service.instance.id"), GlobalProcessIdentifier.SELF.toString())
            .setTimestamp(record.getMillis(), java.util.concurrent.TimeUnit.MILLISECONDS)
            .setAttribute(AttributeKey.stringKey("iso.timestamp"), isotime);
        
        builder.emit();
    }
    
    private Severity mapJulLevelToOtelSeverity(Level level) 
    {
        if (level == Level.SEVERE) 
        {
            return Severity.ERROR;
        } 
        else if (level == Level.WARNING) 
        {
            return Severity.WARN;
        } 
        else if (level == Level.INFO) 
        {
            return Severity.INFO;
        } 
        else if (level == Level.CONFIG) 
        {
            return Severity.DEBUG;
        } 
        else if (level == Level.FINE || level == Level.FINER || level == Level.FINEST) 
        {
            return Severity.TRACE;
        }
        return Severity.UNDEFINED_SEVERITY_NUMBER; 
    }
    
    @Override
    public void flush() 
    {
    	forceFlush();
    }
    
	@Override
    public void close() throws SecurityException 
    {
    	// TODO close this logger!?
//    	otel.close();
    }

	/**
	 *  Flush all otel stuff.
	 */
	// Public to allow flushing from outside (e.g. at end of benchmark). hack!?
    public static void forceFlush()
	{
    	if(otel!=null)
    	{
	    	// TODO: flush only this logegr!?
	    	CompletableResultCode	c1	= otel.getSdkLoggerProvider().forceFlush();
	    	CompletableResultCode	c2	= otel.getSdkMeterProvider().forceFlush();
	    	CompletableResultCode	c3	= otel.getSdkTracerProvider().forceFlush();
	    	c1.join(10, TimeUnit.SECONDS);
	    	c2.join(10, TimeUnit.SECONDS);
	    	c3.join(10, TimeUnit.SECONDS);
    	}
    }
}