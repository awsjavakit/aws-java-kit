package com.gtihub.awsjavakit.attempt;

@FunctionalInterface
public interface SupplierWithException<T, E extends Exception> {

  T call() throws E;

}
