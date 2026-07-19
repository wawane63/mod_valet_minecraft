package com.wawane.valet.ai;

import com.wawane.valet.ValetAnchor;
import com.wawane.valet.ValetDebug;
import com.wawane.valet.breeding.ValetAnimalArea;
import com.wawane.valet.breeding.ValetAnimalStorage;
import com.wawane.valet.farm.ValetFarmArea;
import com.wawane.valet.farm.ValetFarmStorage;
import com.wawane.valet.order.ValetOrders;
import com.wawane.valet.ai.path.ValetSafeNavigation;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.phys.AABB;

/** Territoire de travail stable d'un valet, sans repli sur une position d'entite. */
public final class ValetWorkZone {
    public static final int AREA_MARGIN = 8;
    public static final int AREA_RADIUS = 40;
    public static final int ANCHOR_RADIUS = 24;
    public static final int RESIDENCE_RADIUS = 32;
    public static final int VERTICAL_RADIUS = 16;
    private static final int SAFE_ANCHOR_SEARCH_RADIUS = 16;

    private ValetWorkZone() {
    }

    /**
     * La zone selectionnee est l'autorite. Son centre remplace l'ancienne ancre
     * persistante des que le joueur choisit un champ ou un enclos.
     */
    public static Zone get(ServerLevel world, Villager villager) {
        Zone orderZone = null;
        int farmAreaId = ValetOrders.getFarmAreaId(villager);
        if (farmAreaId >= 0) {
            ValetFarmArea area = ValetFarmStorage.get(world).getArea(farmAreaId);
            if (area != null) {
                orderZone = fromFarmArea(world, area);
            }
        }

        int animalAreaId = orderZone == null ? ValetOrders.getAnimalAreaId(villager) : -1;
        if (orderZone == null && animalAreaId >= 0) {
            ValetAnimalArea area = ValetAnimalStorage.get(world).getArea(animalAreaId);
            if (area != null) {
                orderZone = fromAnimalArea(world, area);
            }
        }

        if (orderZone != null) {
            BlockPos previous = ValetAnchor.get(world, villager);
            orderZone = withNavigableAnchor(world, previous, orderZone);
            if (!orderZone.anchor().equals(previous)) {
                ValetAnchor.set(villager, orderZone.anchor());
                ValetDebug.record(villager, "anchor moved_to_area source=" + orderZone.source()
                        + " pos=" + ValetDebug.shortPos(orderZone.anchor()));
            }
            return orderZone;
        }
        BlockPos anchor = ValetAnchor.get(world, villager);
        return anchor == null ? null : aroundAnchor(world, anchor);
    }

    /** Zone reservee au trajet explicite vers le HOME, sans elargir les cibles de travail. */
    public static Zone residenceZone(ServerLevel world, Villager villager) {
        Zone workZone = get(world, villager);
        if (workZone == null) {
            return null;
        }
        BlockPos anchor = workZone.anchor();
        return create(
                world,
                anchor,
                anchor.getX() - RESIDENCE_RADIUS,
                anchor.getY() - VERTICAL_RADIUS,
                anchor.getZ() - RESIDENCE_RADIUS,
                anchor.getX() + RESIDENCE_RADIUS,
                anchor.getY() + VERTICAL_RADIUS,
                anchor.getZ() + RESIDENCE_RADIUS,
                Source.RESIDENCE,
                RESIDENCE_RADIUS,
                VERTICAL_RADIUS
        );
    }

    public static boolean contains(ServerLevel world, Villager villager, BlockPos pos) {
        Zone zone = get(world, villager);
        return zone != null && zone.contains(pos);
    }

    public static Optional<Zone> zone(ServerLevel world, Villager villager) {
        return Optional.ofNullable(get(world, villager));
    }

    private static Zone fromFarmArea(ServerLevel world, ValetFarmArea area) {
        BlockPos anchor = center(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ());
        return create(
                world,
                anchor,
                area.minX() - AREA_MARGIN,
                area.minY() - AREA_MARGIN,
                area.minZ() - AREA_MARGIN,
                area.maxX() + AREA_MARGIN,
                area.maxY() + AREA_MARGIN,
                area.maxZ() + AREA_MARGIN,
                Source.FARM_AREA,
                AREA_RADIUS,
                VERTICAL_RADIUS
        );
    }

    private static Zone fromAnimalArea(ServerLevel world, ValetAnimalArea area) {
        BlockPos anchor = center(area.minX(), area.minY(), area.minZ(), area.maxX(), area.maxY(), area.maxZ());
        return create(
                world,
                anchor,
                area.minX() - AREA_MARGIN,
                area.minY() - AREA_MARGIN,
                area.minZ() - AREA_MARGIN,
                area.maxX() + AREA_MARGIN,
                area.maxY() + AREA_MARGIN,
                area.maxZ() + AREA_MARGIN,
                Source.ANIMAL_AREA,
                AREA_RADIUS,
                VERTICAL_RADIUS
        );
    }

    private static Zone aroundAnchor(ServerLevel world, BlockPos anchor) {
        return create(
                world,
                anchor.immutable(),
                anchor.getX() - ANCHOR_RADIUS,
                anchor.getY() - VERTICAL_RADIUS,
                anchor.getZ() - ANCHOR_RADIUS,
                anchor.getX() + ANCHOR_RADIUS,
                anchor.getY() + VERTICAL_RADIUS,
                anchor.getZ() + ANCHOR_RADIUS,
                Source.ANCHOR,
                ANCHOR_RADIUS,
                VERTICAL_RADIUS
        );
    }

    private static Zone withNavigableAnchor(ServerLevel world, BlockPos previous, Zone zone) {
        BlockPos center = zone.anchor();
        if (previous != null
                && zone.contains(previous)
                && center.distSqr(previous) <= SAFE_ANCHOR_SEARCH_RADIUS * SAFE_ANCHOR_SEARCH_RADIUS
                && ValetSafeNavigation.isSafeStand(world, previous, 2)) {
            return zone.withAnchor(previous);
        }
        if (ValetSafeNavigation.isSafeStand(world, center, 2)) {
            return zone;
        }
        for (BlockPos candidate : BlockPos.withinManhattan(
                center,
                SAFE_ANCHOR_SEARCH_RADIUS,
                VERTICAL_RADIUS,
                SAFE_ANCHOR_SEARCH_RADIUS
        )) {
            BlockPos stand = candidate.immutable();
            if (zone.contains(stand) && ValetSafeNavigation.isSafeStand(world, stand, 2)) {
                return zone.withAnchor(stand);
            }
        }
        return zone;
    }

    private static Zone create(
            ServerLevel world,
            BlockPos anchor,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            Source source,
            int horizontalRadius,
            int verticalRadius
    ) {
        minX = Math.max(minX, anchor.getX() - horizontalRadius);
        minZ = Math.max(minZ, anchor.getZ() - horizontalRadius);
        maxX = Math.min(maxX, anchor.getX() + horizontalRadius);
        maxZ = Math.min(maxZ, anchor.getZ() + horizontalRadius);
        int boundedMinY = Math.max(world.getMinY(), Math.max(minY, anchor.getY() - verticalRadius));
        int boundedMaxY = Math.min(world.getMaxY() - 1, Math.min(maxY, anchor.getY() + verticalRadius));
        if (boundedMinY > boundedMaxY) {
            return null;
        }
        int anchorY = Math.max(boundedMinY, Math.min(boundedMaxY, anchor.getY()));
        return new Zone(
                new BlockPos(anchor.getX(), anchorY, anchor.getZ()),
                minX,
                boundedMinY,
                minZ,
                maxX,
                boundedMaxY,
                maxZ,
                source
        );
    }

    private static BlockPos center(int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        return new BlockPos(midpoint(minX, maxX), midpoint(minY, maxY), midpoint(minZ, maxZ));
    }

    private static int midpoint(int min, int max) {
        return (int) ((long) min + ((long) max - min) / 2L);
    }

    public enum Source {
        FARM_AREA,
        ANIMAL_AREA,
        ANCHOR,
        RESIDENCE
    }

    public record Zone(
            BlockPos anchor,
            int minX,
            int minY,
            int minZ,
            int maxX,
            int maxY,
            int maxZ,
            Source source
    ) {
        public Zone {
            anchor = anchor.immutable();
            if (minX > maxX || minY > maxY || minZ > maxZ) {
                throw new IllegalArgumentException("Invalid valet work zone bounds");
            }
        }

        public boolean contains(BlockPos pos) {
            if (pos == null) {
                return false;
            }
            return pos.getX() >= minX
                    && pos.getX() <= maxX
                    && pos.getY() >= minY
                    && pos.getY() <= maxY
                    && pos.getZ() >= minZ
                    && pos.getZ() <= maxZ;
        }

        public BlockPos clamp(BlockPos pos) {
            if (contains(pos)) {
                return pos.immutable();
            }
            return new BlockPos(
                    Math.max(minX, Math.min(maxX, pos.getX())),
                    Math.max(minY, Math.min(maxY, pos.getY())),
                    Math.max(minZ, Math.min(maxZ, pos.getZ()))
            );
        }

        public AABB bounds() {
            return new AABB(minX, minY, minZ, maxX + 1.0D, maxY + 1.0D, maxZ + 1.0D);
        }

        private Zone withAnchor(BlockPos newAnchor) {
            return new Zone(newAnchor, minX, minY, minZ, maxX, maxY, maxZ, source);
        }
    }
}
