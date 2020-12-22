package dev.maxneedssnacks.interactio.recipe;

import com.google.gson.JsonObject;
import dev.maxneedssnacks.interactio.event.ExplosionHandler.ExplosionInfo;
import dev.maxneedssnacks.interactio.recipe.ingredient.BlockIngredient;
import dev.maxneedssnacks.interactio.recipe.ingredient.BlockOrItemOutput;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipe;
import dev.maxneedssnacks.interactio.recipe.util.InWorldRecipeType;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.item.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.IRecipeSerializer;
import net.minecraft.item.crafting.IRecipeType;
import net.minecraft.network.PacketBuffer;
import net.minecraft.util.JSONUtils;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.Explosion;
import net.minecraft.world.World;
import net.minecraftforge.registries.ForgeRegistryEntry;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.Random;

public final class BlockExplosionRecipe implements InWorldRecipe<BlockPos, BlockState, ExplosionInfo> {

    public static final Serializer SERIALIZER = new Serializer();

    private final ResourceLocation id;

    private final BlockOrItemOutput output;
    private final BlockIngredient input;

    public BlockExplosionRecipe(ResourceLocation id, BlockOrItemOutput output, BlockIngredient input) {
        this.id = id;
        this.output = output;
        this.input = input;
    }

    @Override
    public boolean canCraft(BlockPos pos, BlockState state) {
        return input.test(state.getBlock());
    }

    @Override
    public void craft(BlockPos pos, ExplosionInfo info) {
        Explosion explosion = info.getExplosion();
        World world = info.getWorld();
        Random rand = world.rand;

        // destroying the block saves me from spawning particles myself AND it doesn't produce drops, woot!!
        world.destroyBlock(pos, false);

        // set it to the default state of our resulting block
        if (output.isBlock()) {
            Block block = output.getBlock();
            if (block != null) world.setBlockState(pos, block.getDefaultState());
        } else if (output.isItem()) {
            Collection<ItemStack> stacks = output.getItems();
            if (stacks != null) {
                double x = pos.getX() + MathHelper.nextDouble(rand, 0.25, 0.75);
                double y = pos.getY() + MathHelper.nextDouble(rand, 0.5, 1);
                double z = pos.getZ() + MathHelper.nextDouble(rand, 0.25, 0.75);

                double vel = MathHelper.nextDouble(rand, 0.1, 0.25);

                stacks.forEach(stack -> {
                    ItemEntity newItem = new ItemEntity(world, x, y, z, stack.copy());
                    newItem.setMotion(0, vel, 0);
                    newItem.setPickupDelay(20);
                    world.addEntity(newItem);
                });
            }
        }

        // don't let the explosion blow up the block we JUST placed
        explosion.getAffectedBlockPositions().remove(pos);

    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public IRecipeSerializer<?> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public IRecipeType<?> getType() {
        return InWorldRecipeType.BLOCK_EXPLODE;
    }

    public BlockOrItemOutput getOutput() {
        return this.output;
    }

    public BlockIngredient getInput() {
        return this.input;
    }

    private static class Serializer extends ForgeRegistryEntry<IRecipeSerializer<?>> implements IRecipeSerializer<BlockExplosionRecipe> {
        @Override
        public BlockExplosionRecipe read(ResourceLocation id, JsonObject json) {
            BlockOrItemOutput output = BlockOrItemOutput.create(JSONUtils.getJsonObject(json, "output"));
            BlockIngredient input = BlockIngredient.deserialize(JSONUtils.getJsonObject(json, "input"));

            return new BlockExplosionRecipe(id, output, input);
        }

        @Nullable
        @Override
        public BlockExplosionRecipe read(ResourceLocation id, PacketBuffer buffer) {
            BlockOrItemOutput output = BlockOrItemOutput.read(buffer);
            BlockIngredient input = BlockIngredient.read(buffer);

            return new BlockExplosionRecipe(id, output, input);
        }

        @Override
        public void write(PacketBuffer buffer, BlockExplosionRecipe recipe) {
            recipe.output.write(buffer);
            recipe.input.write(buffer);
        }
    }

}
