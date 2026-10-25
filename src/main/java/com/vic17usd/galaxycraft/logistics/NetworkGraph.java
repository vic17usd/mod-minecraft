package com.vic17usd.galaxycraft.logistics;

import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.*;

/** Recorrido puro y acotado; una red demasiado grande no transfiere recursos parcialmente. */
public final class NetworkGraph {
  public record Component(Set<BlockPos> nodes, boolean complete) {}

  public static Component scan(BlockPos root, Predicate<BlockPos> cable, int limit) {
    var visited = new LinkedHashSet<BlockPos>();
    var queue = new ArrayDeque<BlockPos>();
    visited.add(root.immutable());
    queue.add(root.immutable());
    while (!queue.isEmpty()) {
      var current = queue.removeFirst();
      for (var direction : Direction.values()) {
        var next = current.relative(direction);
        if (cable.test(next) && visited.add(next)) {
          if (visited.size() > limit) return new Component(Set.copyOf(visited), false);
          queue.addLast(next);
        }
      }
    }
    return new Component(Set.copyOf(visited), true);
  }

  private NetworkGraph() {}
}
