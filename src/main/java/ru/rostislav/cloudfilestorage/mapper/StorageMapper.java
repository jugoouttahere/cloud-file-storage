package ru.rostislav.cloudfilestorage.mapper;

import io.minio.StatObjectResponse;
import io.minio.messages.Item;
import ru.rostislav.cloudfilestorage.dto.resource.ResourceType;
import ru.rostislav.cloudfilestorage.dto.storage.StorageObject;

public class StorageMapper {

    public static StorageObject toStorageObject(Item item) {
        if (item.objectName().endsWith("/")) {
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

    public static StorageObject toStorageObject(StatObjectResponse statObjectResponse) {
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
