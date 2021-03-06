package toughasnails.init;

import static toughasnails.api.TANBlocks.campfire;
import static toughasnails.api.TANBlocks.purified_water;
import static toughasnails.api.TANBlocks.purified_water_fluid;
import static toughasnails.api.TANBlocks.rain_collector;
import static toughasnails.api.TANBlocks.temperature_coil;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;

import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.registry.ForgeRegistries;
import net.minecraftforge.fml.common.registry.GameRegistry;
import toughasnails.api.ITANBlock;
import toughasnails.block.BlockRainCollector;
import toughasnails.block.BlockTANCampfire;
import toughasnails.block.BlockTANTemperatureCoil;
import toughasnails.core.ToughAsNails;
import toughasnails.fluids.PurifiedWaterFluid;
import toughasnails.fluids.blocks.BlockPurifiedWaterFluid;
import toughasnails.tileentity.TileEntityTemperatureSpread;
import toughasnails.util.BlockStateUtils;
import toughasnails.util.inventory.CreativeTabTAN;

public class ModBlocks
{
    public static void init()
    {
        campfire = registerBlock( new BlockTANCampfire(), "campfire" );
        rain_collector = registerBlock( new BlockRainCollector(), "rain_collector" );
        //gas = registerBlock( new BlockTANGas(), "gas" );
        //gas.setCreativeTab(null);
        temperature_coil = registerBlock(new BlockTANTemperatureCoil(), "temperature_coil");
        
        purified_water_fluid = PurifiedWaterFluid.instance;
        FluidRegistry.addBucketForFluid(purified_water_fluid);
        purified_water = registerFluidBlock(purified_water_fluid, new BlockPurifiedWaterFluid(purified_water_fluid), "purified_water");

        GameRegistry.registerTileEntity(TileEntityTemperatureSpread.class, "temperature_spread");
    }
    
    public static Block registerFluidBlock(Fluid fluid, Block fluidBlock, String name)
    {
        fluidBlock.setRegistryName(new ResourceLocation(ToughAsNails.MOD_ID, name));
        ForgeRegistries.BLOCKS.register(fluidBlock);
        ToughAsNails.proxy.registerFluidBlockRendering(fluidBlock, name);
        fluid.setBlock(fluidBlock);
        return fluidBlock;
    }

    public static void registerBlockItemModel(Block block, String stateName, int stateMeta)
    {
        Item item = Item.getItemFromBlock(block);
        ToughAsNails.proxy.registerItemVariantModel(item, stateName, stateMeta);
    }

    public static Block registerBlock(Block block, String blockName)
    {
        // by default, set the creative tab for all blocks added in BOP to CreativeTabBOP.instance
        return registerBlock(block, blockName, CreativeTabTAN.instance);
    }

    public static Block registerBlock(Block block, String blockName,CreativeTabs tab)
    {
        return registerBlock(block, blockName, tab, true);
    }

    public static Block registerBlock(Block block, String blockName, CreativeTabs tab, boolean registerItemModels)
    {
        Preconditions.checkNotNull(block, "Cannot register a null block");
        block.setUnlocalizedName(blockName);
        block.setCreativeTab(tab);

        if (block instanceof ITANBlock)
        {
            // if this block supports the IBOPBlock interface then we can determine the item block class, and sub-blocks automatically
            ITANBlock bopBlock = (ITANBlock)block;

            registerBlockWithItem(block, blockName, bopBlock.getItemClass());
            ToughAsNails.proxy.registerBlockSided(block);

            // check for missing default states
            IBlockState defaultState = block.getDefaultState();
            if (defaultState == null)
            {
                defaultState = block.getBlockState().getBaseState();
                ToughAsNails.logger.error("Missing default state for " + block.getUnlocalizedName());
            }

            // Some blocks such as doors and slabs register their items after the blocks (getItemClass returns null)
            if (registerItemModels)
            {
                // get the preset blocks variants
                ImmutableSet<IBlockState> presets = BlockStateUtils.getBlockPresets(block);
                if (presets.isEmpty())
                {
                    // block has no sub-blocks to register
                    registerBlockItemModel(block, blockName, 0);
                } else
                {
                    // register all the sub-blocks
                    for (IBlockState state : presets)
                    {
                        String stateName = bopBlock.getStateName(state);
                        int stateMeta = block.getMetaFromState(state);
                        registerBlockItemModel(block, stateName, stateMeta);
                    }
                }
            }
        }
        else
        {
            // for vanilla blocks, just register a single variant with meta=0 and assume ItemBlock for the item class
            registerBlockWithItem(block, blockName, ItemBlock.class);
            registerBlockItemModel(block, blockName, 0);
        }

        return block;
    }

    private static void registerBlockWithItem(Block block, String blockName, Class<? extends ItemBlock> clazz)
    {
        try
        {
            Item itemBlock = clazz != null ? (Item)clazz.getConstructor(Block.class).newInstance(block) : null;
            ResourceLocation location = new ResourceLocation(ToughAsNails.MOD_ID, blockName);

            block.setRegistryName(new ResourceLocation(ToughAsNails.MOD_ID, blockName));

            ForgeRegistries.BLOCKS.register(block);
            if (itemBlock != null)
            {
                itemBlock.setRegistryName(new ResourceLocation(ToughAsNails.MOD_ID, blockName));
                ForgeRegistries.ITEMS.register(itemBlock);
            }
        }
        catch (Exception e)
        {
            throw new RuntimeException("An error occurred associating an item block during registration of " + blockName, e);
        }
    }
}
