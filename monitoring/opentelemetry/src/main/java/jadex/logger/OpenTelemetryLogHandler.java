package jadex.logger;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.logs.Logger;
import io.opentelemetry.api.logs.Severity;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporter;
import io.opentelemetry.exporter.otlp.logs.OtlpGrpcLogRecordExporterBuilder;
import io.opentelemetry.sdk.OpenTelemetrySdk;
import io.opentelemetry.sdk.OpenTelemetrySdkBuilder;
import io.opentelemetry.sdk.logs.SdkLoggerProvider;
import io.opentelemetry.sdk.logs.export.BatchLogRecordProcessor;
import io.opentelemetry.sdk.resources.Resource;
import jadex.core.impl.GlobalProcessIdentifier;

public class OpenTelemetryLogHandler extends Handler 
{
    private final Logger logger;

    public static OpenTelemetry otel;
    
    static
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
        
        SdkLoggerProvider provider = SdkLoggerProvider.builder().addLogRecordProcessor(processor)
        	.setResource(Resource.create(Attributes.of(AttributeKey.stringKey("service.name"),  GlobalProcessIdentifier.SELF.toString()))).build();
        
        OpenTelemetrySdkBuilder otelb = OpenTelemetrySdk.builder();
        otelb.setLoggerProvider(provider);
        otel = otelb.buildAndRegisterGlobal();
        
       	System.out.println("otel init: "+url+" "+key);
    }
    
    public OpenTelemetryLogHandler() 
    {
        this.logger = otel.getLogsBridge().get("java-util-logging");
    }

    @Override
    public void publish(LogRecord record) 
    {
    	// todo? return respecting loglevel 
    	
    	if (record == null || record.getMessage() == null) 
    		return;
    	
    	Instant instant = Instant.ofEpochMilli(record.getMillis());
    	String isotime = DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC).format(instant);

        LogRecordBuilder builder = logger.logRecordBuilder()
            .setBody(record.getMessage())
        	.setSeverity(mapJulLevelToOtelSeverity(record.getLevel())) 
           	.setSeverityText(record.getLevel().getName())
        	.setAttribute(AttributeKey.stringKey("logger.name"), record.getLoggerName())
            //.setAttribute(AttributeKey.stringKey("thread.name"), Thread.currentThread().getName())
            //.setAttribute(AttributeKey.stringKey("service.name"), "my_java_service")
            .setAttribute(AttributeKey.stringKey("service.instance.id"), record.getLoggerName())
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
    }
    
    @Override
    public void close() throws SecurityException 
    {
    }
}