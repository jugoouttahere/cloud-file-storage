package ru.rostislav.cloudfilestorage.mapper;

import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import org.springframework.stereotype.Component;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceType;
import ru.rostislav.cloudfilestorage.dto.storage.StorageObject;

@Component
public class StorageMapper {

    public StorageObject toStorageObject(Item item) {
        if (item.isDir()) {
            return new StorageObject(
                    item.objectName(),
                    null,
                    ResourceType.DIRECTORY
            );
        } else {
            return new StorageObject(
                    item.objectName(),
                    item.size(),
                    ResourceType.FILE
            );
        }
    }

    public StorageObject toStorageObject(StatObjectResponse statObjectResponse) {
        if (statObjectResponse.object().endsWith("/")) {
            return new StorageObject(
                    statObjectResponse.object(),
                    null,
                    ResourceType.DIRECTORY
            );
        } else {
            return new StorageObject(
                    statObjectResponse.object(),
                    statObjectResponse.size(),
                    ResourceType.FILE
            );
        }
    }
}
