package dev.ldldevelopers.simplekmock

/**
 * Exception thrown when a mock is called without having previously been set
 */
public class MockNotSetException : RuntimeException("Mock not set")
