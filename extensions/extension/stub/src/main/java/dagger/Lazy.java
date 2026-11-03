package dagger;

/**
 * A dependency of Dagger created when first used.
 */
public interface Lazy {
    Object get();
}
