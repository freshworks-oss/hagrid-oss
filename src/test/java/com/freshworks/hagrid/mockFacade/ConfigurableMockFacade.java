package com.freshworks.hagrid.mockFacade;

public interface ConfigurableMockFacade {

    public <T> T build(Class<T> clazz);

}
