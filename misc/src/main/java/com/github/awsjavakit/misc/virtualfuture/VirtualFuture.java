package com.github.awsjavakit.misc.virtualfuture;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

public final class VirtualFuture<A> {


  private final Supplier<A> task;
  private final CompletableFuture<A> future;

  private VirtualFuture(Supplier<A> task) {
    this.task = task;
    this.future = new CompletableFuture<>();
    Thread.startVirtualThread(() -> executeTaskInsideCompletableFuture(future));

  }

  public VirtualFuture(CompletableFuture<A> future) {
    this.future = future;
    this.task = null;
  }

  public static <A> VirtualFuture<A> supply(Supplier<A> task) {
    return new VirtualFuture<>(task);
  }

  public static VirtualFuture<Void> allOf(VirtualFuture... futures) {
    var completableFutures = Arrays.stream(futures).map(future -> future.future)
      .toArray(CompletableFuture[]::new);
    var combinedFuture = CompletableFuture.allOf(completableFutures);
    return new VirtualFuture<Void>(combinedFuture);
  }

  public A join() {
    return this.future.join();
  }

  public A get() throws ExecutionException, InterruptedException {
    return future.get();
  }

  private void executeTaskInsideCompletableFuture(CompletableFuture<A> future) {
    try {
      future.complete(task.get());
    } catch (Exception e) {
      future.completeExceptionally(e);
    }
  }
}
