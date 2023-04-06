package org.embulk.config;

@Deprecated  // https://github.com/embulk/embulk/issues/1304
public class ModelManager {
    public ModelManager() {
        this.delegate = ModelManagerDelegate.of();
    }

    public <T> T readObject(Class<T> valueType, String json) {
        return this.delegate.readObject(valueType, json);
    }

    // The method is removed: public <T> T readObject(Class<T> valueType, JsonParser parser)

    public <T> T readObjectWithConfigSerDe(Class<T> valueType, String json) {
        return this.delegate.readObjectWithConfigSerDe(valueType, json);
    }

    // The method is removed: public <T> T readObjectWithConfigSerDe(Class<T> valueType, JsonParser parser)

    public DataSource readObjectAsDataSource(String json) {
        return this.delegate.readObjectAsDataSource(json);
    }

    public String writeObject(Object object) {
        return this.delegate.writeObject(object);
    }

    public void validate(Object object) {
        this.delegate.validate(object);
    }

    public TaskReport newTaskReport() {
        return this.delegate.newTaskReport();
    }

    public ConfigDiff newConfigDiff() {
        return this.delegate.newConfigDiff();
    }

    public ConfigSource newConfigSource() {
        return this.delegate.newConfigSource();
    }

    public TaskSource newTaskSource() {
        return this.delegate.newTaskSource();
    }

    ModelManagerDelegate getDelegate() {
        return this.delegate;
    }

    private final ModelManagerDelegate delegate;
}
