package com.roberthj.project.healthcare.collection.processor;

import com.roberthj.project.healthcare.collection.enums.HealthDataSource;
import com.roberthj.project.healthcare.collection.enums.HealthDataType;
import com.roberthj.project.healthcare.collection.exception.CollectionException;
import com.roberthj.project.healthcare.collection.model.HealthDataFormat;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.PROCESSOR_ALREADY_REGISTERED;
import static com.roberthj.project.healthcare.collection.exception.CollectionErrorCode.PROCESSOR_NOT_FOUND;

@Component
public class HealthDataProcessorRegistry {

    private final Map<HealthDataFormat, HealthDataProcessor> processors;

    public HealthDataProcessorRegistry(List<HealthDataProcessor> processors) {
        Map<HealthDataFormat, HealthDataProcessor> processorMap = new HashMap<>();

        for (HealthDataProcessor processor : processors) {
            HealthDataProcessor previousProcessor = processorMap.putIfAbsent(
                processor.format(),
                processor
            );
            if (previousProcessor != null) {
                throw new CollectionException(
                    PROCESSOR_ALREADY_REGISTERED,
                    processor.format().toString()
                );
            }
        }

        this.processors = Map.copyOf(processorMap);
    }

    public HealthDataProcessor getProcessor(
        HealthDataSource source,
        HealthDataType dataType
    ) {
        HealthDataFormat format = new HealthDataFormat(source, dataType);
        HealthDataProcessor processor = processors.get(format);

        if (processor == null) {
            throw new CollectionException(PROCESSOR_NOT_FOUND, format.toString());
        }

        return processor;
    }
}
