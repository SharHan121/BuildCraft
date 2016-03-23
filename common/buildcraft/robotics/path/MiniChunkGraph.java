package buildcraft.robotics.path;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;

import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;

public class MiniChunkGraph {
    public enum ChunkType {
        COMPLETLY_FREE,
        SINGLE_GRAPH,
        MULTIPLE_GRAPHS,
        COMPLETLY_FILLED
    }

    public final BlockPos min;
    public final ChunkType type;
    public final Map<EnumFacing, MiniChunkGraph> neighbours = new EnumMap<>(EnumFacing.class);
    public final ImmutableList<MiniChunkNode> nodes;
    final byte[][][] expenseArray, graphArray;

    public MiniChunkGraph(BlockPos min, ChunkType type, byte[][][] expenseArray, byte[][][] graphArray, int numNodes) {
        this.min = min;
        this.type = type;
        this.expenseArray = expenseArray;
        this.graphArray = graphArray;
        ImmutableList.Builder<MiniChunkNode> nodes = ImmutableList.builder();
        for (int i = 0; i < numNodes; i++) {
            nodes.add(new MiniChunkNode(i));
        }
        this.nodes = nodes.build();
    }

    public class MiniChunkNode {
        final int id;
        final Set<MiniChunkNode> connected = Sets.newIdentityHashSet();

        public MiniChunkNode(int id) {
            this.id = id;
        }

        public Set<MiniChunkNode> getConnected() {
            return Collections.unmodifiableSet(connected);
        }

        /** Checks if this node contains the given position. This will be the world position of the block */
        public boolean contains(BlockPos pos) {
            BlockPos normalised = pos.subtract(min);
            if (!TaskMiniChunkAnalyser.isValid(normalised)) return false;
            return graphArray[normalised.getX()][normalised.getY()][normalised.getZ()] == id;
        }

        public int getExpense(BlockPos pos) {
            BlockPos normalised = pos.subtract(min);
            if (!TaskMiniChunkAnalyser.isValid(normalised)) return Integer.MAX_VALUE;
            int expense = expenseArray[normalised.getX()][normalised.getY()][normalised.getZ()];
            if (expense < 0) return Integer.MAX_VALUE;
            return expense;
        }

        public MiniChunkGraph getParent() {
            return MiniChunkGraph.this;
        }
    }
}
