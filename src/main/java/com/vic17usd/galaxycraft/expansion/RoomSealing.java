package com.vic17usd.galaxycraft.expansion;

import java.util.*;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

/**
 * Flood-fill acotado y comprobable sin mundo: desconocido, fuera de rango o demasiado grande
 * implica fuga.
 */
public final class RoomSealing {
  public enum Cell {
    OPEN,
    WALL,
    UNKNOWN
  }

  public record Result(boolean sealed, Set<BlockPos> cells) {}

  public static Result scan(
      BlockPos start, Function<BlockPos, Cell> access, int limit, int radius) {
    if (access.apply(start) != Cell.OPEN) return new Result(false, Set.of());
    var visited = new HashSet<BlockPos>();
    var queue = new ArrayDeque<BlockPos>();
    visited.add(start);
    queue.add(start);
    while (!queue.isEmpty()) {
      var pos = queue.removeFirst();
      for (var direction : Direction.values()) {
        var next = pos.relative(direction);
        var cell = access.apply(next);
        if (cell == Cell.WALL) continue;
        if (cell == Cell.UNKNOWN
            || Math.abs(next.getX() - start.getX()) > radius
            || Math.abs(next.getY() - start.getY()) > radius
            || Math.abs(next.getZ() - start.getZ()) > radius) return new Result(false, Set.of());
        if (visited.add(next)) {
          if (visited.size() > limit) return new Result(false, Set.of());
          queue.add(next);
        }
      }
    }
    return new Result(true, Set.copyOf(visited));
  }

  private RoomSealing() {}
}
