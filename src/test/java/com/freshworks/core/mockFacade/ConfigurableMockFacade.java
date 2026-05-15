package com.freshworks.core.mockFacade;

public interface ConfigurableMockFacade {

    public <T> T build(Class<T> clazz);

}
