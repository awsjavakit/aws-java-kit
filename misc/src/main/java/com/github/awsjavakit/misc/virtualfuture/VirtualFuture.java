package com.github.awsjavakit.misc.virtualfuture;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

public final class VirtualFuture<A> {


  private final Supplier<A> task;
  private  CompletableFuture<A> future;

  private VirtualFuture(Supplier<A> inputTask) {
    this.task=  inputTask;
    execute();
  }

  public VirtualFuture(CompletableFuture<A> future) {
    this.future = future;
    this.task = null;
  }

  public static <A> VirtualFuture<A> supply(Supplier<A> task) {
    return new VirtualFuture<>(task);
  }

    public static VirtualFuture<Void> allOf(VirtualFuture... futures) {
    var completableFutures = Arrays.stream(futures)
      .map(VirtualFuture::execute)
      .map(future -> future.future)
      .toArray(CompletableFuture[]::new);

    var combinedFuture = CompletableFuture.allOf(completableFutures);
    return new VirtualFuture<Void>(combinedFuture);
  }

  public A join() {
    return this.future.join();
  }

  private VirtualFuture<A> execute() {
    this.future = new CompletableFuture<>();
    Thread.startVirtualThread(() -> executeTaskInsideCompletableFuture(future));
    return this;
  }

  public A get() throws ExecutionException, InterruptedException {
    return future.get();
  }

  public <B> VirtualFuture<B> map(Function<A, B> function) {
    var supplier = new Supplier<B>(){
      @Override
      public B get() {
        return function.apply(task.get());
      }
    };
    return VirtualFuture.supply(supplier);
  }

  private void executeTaskInsideCompletableFuture(CompletableFuture<A> future) {
    try {
      future.complete(task.get());
    } catch (Exception e) {
      future.completeExceptionally(e);
    }
  }
}
