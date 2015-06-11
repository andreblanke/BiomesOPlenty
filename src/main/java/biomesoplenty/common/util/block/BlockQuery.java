/*******************************************************************************
 * Copyright 2014, the Biomes O' Plenty Team
 * 
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 * 
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/

package biomesoplenty.common.util.block;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import biomesoplenty.api.block.ISustainsPlantType;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;

import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.BlockPos;
import net.minecraft.util.EnumFacing;
import net.minecraft.world.World;
import net.minecraftforge.common.EnumPlantType;

public class BlockQuery
{
    
    
    /***** Interfaces *****/
    
    // for queries on a particular block position in the world
    public static interface IBlockPosQuery
    {
        public boolean matches(World world, BlockPos pos);
    }
    
    // for compound queries
    public static interface ICompoundBlockPosQuery extends IBlockPosQuery
    {
        public void add(IBlockPosQuery a);
        public IBlockPosQuery instance();
    }
    
    // for queries which depend only on the block state, and not on it's neighbors or position in the world
    public static interface IBlockQuery extends IBlockPosQuery
    {
        public boolean matches(IBlockState state);
    }
    
    
    
    /***** Builder *****/
    
    public static class CompoundQueryBuilder
    {
        private ICompoundBlockPosQuery query;
        
        public CompoundQueryBuilder(ICompoundBlockPosQuery query)
        {
            this.query = query;
        }
        
        public CompoundQueryBuilder and(IBlockPosQuery a)
        {
            query.add(a);
            return this;
        }
        
        public CompoundQueryBuilder blocks(Block... blocks) {return this.and(new BlockQueryBlock(blocks));}
        public CompoundQueryBuilder states(IBlockState... states) {return this.and(new BlockQueryState(states));}
        public CompoundQueryBuilder blockClass(Class<? extends Block> clazz) {return this.and(new BlockQueryClass(clazz));}
        public CompoundQueryBuilder materials(Material... materials) {return this.and(new BlockQueryMaterial(materials));}
        public CompoundQueryBuilder withProperty(String propName, String... propValues) {return this.and(new BlockQueryProperty(propName, propValues));}
        
        public CompoundQueryBuilder not(IBlockPosQuery query) {return this.and(new BlockPosQueryNot(query));}
        public CompoundQueryBuilder notBlocks(Block... blocks) {return this.not(new BlockQueryBlock(blocks));}
        public CompoundQueryBuilder notStates(IBlockState... states) {return this.not(new BlockQueryState(states));}
        public CompoundQueryBuilder notBlockClass(Class<? extends Block> clazz) {return this.not(new BlockQueryClass(clazz));}
        public CompoundQueryBuilder notMaterial(Material... materials) {return this.not(new BlockQueryMaterial(materials));}
        public CompoundQueryBuilder notWithProperty(String propName, String... propValues) {return this.not(new BlockQueryProperty(propName, propValues));}     

        public CompoundQueryBuilder withAltitudeBetween(int a, int b) {return this.and(new BlockPosQueryAltitude(a,b));}
        public CompoundQueryBuilder byWater() {return this.and(hasWater);}
        public CompoundQueryBuilder withAirAbove() {return this.and(airAbove);}
        public CompoundQueryBuilder withLightAtLeast(int a) {return this.and(new BlockPosQueryLightAtLeast(a));}
        public CompoundQueryBuilder withLightNoMoreThan(int a) {return this.and(new BlockPosQueryLightNoMoreThan(a));}
        public CompoundQueryBuilder sustainsPlant(EnumPlantType plantType) {return this.and(new BlockPosQuerySustainsPlantType(plantType));}

        
        public IBlockPosQuery create() {return this.query.instance();}
        
    }
    
    public static CompoundQueryBuilder buildAnd() {return new CompoundQueryBuilder(new BlockPosQueryAnd());}
    public static CompoundQueryBuilder buildOr() {return new CompoundQueryBuilder(new BlockPosQueryOr());}
    
    
    

    
 
    /***** Some handy reusable queries *****/    
 
    
    // match any position
    public static IBlockPosQuery anything = new IBlockPosQuery()
    {
        @Override
        public boolean matches(World world, BlockPos pos) {
            return true;
        }
    };
    
    // match no positions
    public static IBlockPosQuery nothing = new IBlockPosQuery()
    {
        @Override
        public boolean matches(World world, BlockPos pos) {
            return false;
        }
    };
    
    // Match block positions adjacent to water
    public static IBlockPosQuery hasWater = new IBlockPosQuery()
    {
        @Override
        public boolean matches(World world, BlockPos pos)
        {
            return (world.getBlockState(pos.west()).getBlock().getMaterial() == Material.water || world.getBlockState(pos.east()).getBlock().getMaterial() == Material.water || world.getBlockState(pos.north()).getBlock().getMaterial() == Material.water || world.getBlockState(pos.south()).getBlock().getMaterial() == Material.water);
        }
    };
    
    // Match block positions with air above
    public static IBlockPosQuery airAbove = new IBlockPosQuery()
    {
        @Override
        public boolean matches(World world, BlockPos pos)
        {
            return world.isAirBlock(pos.up());
        }
    };
    
    // Match blocks which are not unbreakable - IE not bedrock, barrier, command blocks
    public static IBlockPosQuery breakable = new IBlockPosQuery()
    {
        // Block.setBlockUnbreakable sets the hardness value to -1.0F
        @Override
        public boolean matches(World world, BlockPos pos)
        {
            return world.getBlockState(pos).getBlock().getBlockHardness(world, pos) >= 0.0F;
        }
    };
    
    public static IBlockPosQuery isAirOrLeaves = new BlockQueryMaterial(Material.air, Material.leaves);
    
    
    
    /***** Compound Queries *****/
    

    // Match a block pos if any of the children match
    public static class BlockPosQueryOr implements ICompoundBlockPosQuery
    {
        private ArrayList<IBlockPosQuery> children;
        public BlockPosQueryOr(IBlockPosQuery... children)
        {
            this.children = new ArrayList<IBlockPosQuery>(Arrays.asList(children));
        }
        @Override
        public boolean matches(World world, BlockPos pos)
        {
            for (IBlockPosQuery child : this.children)
            {
                if (child.matches(world, pos))
                {
                    return true;
                }
            }
            return false;
        }
        @Override
        public void add(IBlockPosQuery child)
        {
            if (child != null) {this.children.add(child);}
        }
        @Override
        public IBlockPosQuery instance()
        {
            return this.children.size() == 0 ? anything : (this.children.size() == 1 ? this.children.get(0) : this);
        }
    }
    
    // Match a block pos if all of the children match
    public static class BlockPosQueryAnd implements ICompoundBlockPosQuery
    {
        private ArrayList<IBlockPosQuery> children;
        public BlockPosQueryAnd(IBlockPosQuery... children)
        {
            this.children = new ArrayList<IBlockPosQuery>(Arrays.asList(children));
        }
        @Override
        public boolean matches(World world, BlockPos pos)
        {
            for (IBlockPosQuery child : this.children)
            {
                if (!child.matches(world, pos))
                {
                    return false;
                }
            }
            return true;
        }
        @Override
        public void add(IBlockPosQuery child)
        {
            if (child != null) {this.children.add(child);}
        }
        @Override
        public IBlockPosQuery instance()
        {
            return this.children.size() == 0 ? anything : (this.children.size() == 1 ? this.children.get(0) : this);
        }
    }
    
    
    
    
    /***** Other queries *****/
    
    
    // Match a block pos if the child does not match
    public static class BlockPosQueryNot implements IBlockPosQuery
    {
        IBlockPosQuery child;
        public BlockPosQueryNot(IBlockPosQuery child)
        {
            this.child = child;
        }
        @Override
        public boolean matches(World world, BlockPos pos)
        {
            return !this.child.matches(world, pos);
        }
    }
    
    
    // Match block positions in a height range
    public static class BlockPosQueryAltitude implements IBlockPosQuery
    {
        public int minHeight;
        public int maxHeight;
        
        public BlockPosQueryAltitude(int minHeight, int maxHeight)
        {
            this.minHeight = minHeight;
            this.maxHeight = maxHeight;
        }
        
        @Override
        public boolean matches(World world, BlockPos pos)
        {
            return pos.getY() >= this.minHeight && pos.getY() <= this.maxHeight;
        }
    }
    
    // Match block positions based on light level
    public static class BlockPosQueryLightAtLeast implements IBlockPosQuery
    {
        public int level;
        
        public BlockPosQueryLightAtLeast(int level)
        {
            this.level = level;
        }
        
        @Override
        public boolean matches(World world, BlockPos pos)
        {
            return world.getLight(pos) >= this.level || world.canSeeSky(pos);
        }
    }
    public static class BlockPosQueryLightNoMoreThan implements IBlockPosQuery
    {
        public int level;
        
        public BlockPosQueryLightNoMoreThan(int level)
        {
            this.level = level;
        }
        
        @Override
        public boolean matches(World world, BlockPos pos)
        {
            return world.getLight(pos) <= this.level;
        }
    }
    
    // Match blocks which can sustain the given forge EnumPlantType plant type
    public static class BlockPosQuerySustainsPlantType implements IBlockPosQuery
    {
        private EnumPlantType plantType;
        public BlockPosQuerySustainsPlantType(EnumPlantType plantType)
        {
            this.plantType = plantType;
        }
        @Override
        public boolean matches(World world, BlockPos pos)
        {
            IBlockState state = world.getBlockState(pos);
            Block block = state.getBlock();
            
            if (block instanceof ISustainsPlantType)
            {
                // If there's a function specifically available for it, then use it
                return ((ISustainsPlantType)block).canSustainPlantType(world, pos, this.plantType);
            }
            else
            {
                // Otherwise fall back to the vanilla code
                switch (this.plantType)
                {
                    case Desert: return block == net.minecraft.init.Blocks.sand || block == net.minecraft.init.Blocks.hardened_clay || block == net.minecraft.init.Blocks.stained_hardened_clay || block == net.minecraft.init.Blocks.dirt;
                    case Nether: return block == net.minecraft.init.Blocks.soul_sand;
                    case Crop:   return block == net.minecraft.init.Blocks.farmland;
                    case Cave:   return block.isSideSolid(world, pos, EnumFacing.UP);
                    case Plains: return block == net.minecraft.init.Blocks.grass || block == net.minecraft.init.Blocks.dirt || block == net.minecraft.init.Blocks.farmland;
                    case Water:  return block.getMaterial() == Material.water && ((Integer)state.getValue(BlockLiquid.LEVEL)) == 0;
                    case Beach:
                        boolean isBeach = block == net.minecraft.init.Blocks.grass || block == net.minecraft.init.Blocks.dirt || block == net.minecraft.init.Blocks.sand;
                        boolean hasWater = (world.getBlockState(pos.east()).getBlock().getMaterial() == Material.water ||
                                            world.getBlockState(pos.west()).getBlock().getMaterial() == Material.water ||
                                            world.getBlockState(pos.north()).getBlock().getMaterial() == Material.water ||
                                            world.getBlockState(pos.south()).getBlock().getMaterial() == Material.water);
                        return isBeach && hasWater;
                }
                return false;  
            }
        }
    }
      
    
    public static abstract class BlockQueryBase implements IBlockQuery
    {
        @Override
        public boolean matches(World world, BlockPos pos)
        {
            return this.matches(world.getBlockState(pos));
        }        
    }
    
    // Match a block if the child does not match
    public static class BlockQueryNot extends BlockQueryBase
    {
        IBlockQuery child;
        public BlockQueryNot(IBlockQuery child)
        {
            this.child = child;
        }
        @Override
        public boolean matches(IBlockState state)
        {
            return !this.child.matches(state);
        }
    }
    
    // Match against a set of block instances
    public static class BlockQueryBlock extends BlockQueryBase
    {
        private Set<Block> blocks;
        
        public BlockQueryBlock(Block... blocks)
        {
            this.blocks = Sets.newHashSet(blocks);
        }
        
        @Override
        public boolean matches(IBlockState state)
        {
            return this.blocks.contains(state.getBlock());
        }
        
        public static IBlockQuery of(String blockName, boolean negated) throws BlockQueryParseException
        {
            Block block = Block.getBlockFromName(blockName);
            if (block == null)
            {
                throw new BlockQueryParseException("No block called "+blockName);
            } else {
                IBlockQuery bm = new BlockQueryBlock(block);
                return negated ? new BlockQueryNot(bm) : bm;
            }
        }
    }
    
    // Match against a set of block state instances
    public static class BlockQueryState extends BlockQueryBase
    {
        private Set<IBlockState> states;
        
        public BlockQueryState(IBlockState... states)
        {
            this.states = Sets.newHashSet(states);
        }
        
        @Override
        public boolean matches(IBlockState state)
        {
            return this.states.contains(state);
        }
    }
    
    // Match against a set of block class
    public static class BlockQueryClass extends BlockQueryBase
    {
        public static String[] packages = {"","biomesoplenty.common.block.","net.minecraft.block."};
        
        private Class<? extends Block> clazz;
        private boolean strict;

        public BlockQueryClass(Class<? extends Block> clazz)
        {
            this(clazz, false);
        }
        
        public BlockQueryClass(Class<? extends Block> clazz, boolean strict)
        {
            this.clazz = clazz;
            this.strict = strict;
        }
        
        @Override
        public boolean matches(IBlockState state)
        {
            return strict ? (state.getBlock().getClass() == this.clazz) : this.clazz.isInstance(state.getBlock());
        }
        
        public static IBlockQuery of(String className, boolean negated, boolean strict) throws BlockQueryParseException
        {
            Class clazz;
            for (String packageName : packages)
            {
                try {
                    clazz = Class.forName(packageName+className);
                } catch (Exception e) {
                    continue;
                }
                if (Block.class.isAssignableFrom(clazz))
                {
                    IBlockQuery bm = new BlockQueryClass(clazz, strict);
                    return negated ? new BlockQueryNot(bm) : bm;
                }
            }
            throw new BlockQueryParseException("No class found extending from Block called "+className);
        }
    }
    
    // Match against a state property value
    public static class BlockQueryProperty extends BlockQueryBase
    {
        private static Pattern propertyNameValueRegex = Pattern.compile("^\\s*((\\w+)\\s*=\\s*)?([\\w\\|]+)\\s*$");
        
        private String propName;
        private String[] propValues;
        
        public BlockQueryProperty(String propName, String... propValues)
        {
            this.propName = propName;
            this.propValues = propValues;
        }
        
        @Override
        public boolean matches(IBlockState state)
        {
            ImmutableMap properties = state.getProperties();
            for (Object property : properties.keySet())
            {
                if (((IProperty)property).getName().equalsIgnoreCase(this.propName))
                {
                    String thisPropValue = ((Comparable)properties.get(property)).toString();
                    for (String value : this.propValues)
                    {
                        if (thisPropValue.equalsIgnoreCase(value))
                        {
                            return true;
                        }
                    }
                    return false;
                }
            }
            return false;
        }
        
        public static IBlockQuery of(String nameValuePair, boolean negated) throws BlockQueryParseException
        {
            Matcher m = propertyNameValueRegex.matcher(nameValuePair);
            if (!m.find())
            {
                throw new BlockQueryParseException("Syntax error in "+nameValuePair);
            }
            String propName = m.group(2);
            String[] propValues = m.group(3).split("\\|");          
            if (propName == null) {propName = "variant";}
            IBlockQuery bm = new BlockQueryProperty(propName, propValues);
            return negated ? new BlockQueryNot(bm) : bm;
        }
    }
    
    // Match against a set of block materials
    public static class BlockQueryMaterial extends BlockQueryBase
    {
        private Set<Material> materials;
        public BlockQueryMaterial(Material... materials)
        {
            this.materials = Sets.newHashSet(materials);
        }
        @Override
        public boolean matches(IBlockState state)
        {
            return this.materials.contains(state.getBlock().getMaterial());
        }
        
        public static IBlockQuery of(String materialName, boolean negated) throws BlockQueryParseException
        {
            try {
                Field f = Material.class.getField(materialName);
                Object mat = f.get(null);
                if (mat instanceof Material)
                {
                    IBlockQuery bm = new BlockQueryMaterial((Material)mat);
                    return negated ? new BlockQueryNot(bm) : bm;
                }
            } catch (Exception e) {;}
            throw new BlockQueryParseException("No block material found called "+materialName);
        }
    }    
    

    
    
    
    /***** Parsing from a string *****/
    
    public static class BlockQueryParseException extends Exception
    {
        public BlockQueryParseException(String message)
        {
            super(message);
        }
    }
    
    public static final Map<String, IBlockPosQuery> predefined = new HashMap<String, IBlockPosQuery>();
    
    // regular expression to match a token in a block query - eg  'sand' '%BlockBOPLeaves' '[facing=up]' etc
    private static Pattern nextTokenRegex = Pattern.compile("^(!?([\\w:]+|\\%\\w+|\\$\\w+|~\\w+|\\[.+\\]|@\\w+))");
    // regular expression for splitting up a comma delimited list
    private static Pattern commaDelimitRegex = Pattern.compile("\\s*,\\s*");
    
    // parse the given block query string and return the equivalent IBlockQuery object
    public static IBlockPosQuery parseQueryString(String spec) throws BlockQueryParseException
    {
        BlockPosQueryOr bmAny = new BlockPosQueryOr();
        String[] subspecs = commaDelimitRegex.split(spec);
        for (String subspec : subspecs)
        {
            bmAny.add( parseQueryStringSingle(subspec) );
        }
        return bmAny.instance();
    }
    
    
    private static IBlockPosQuery parseQueryStringSingle(String spec) throws BlockQueryParseException
    {
        BlockPosQueryAnd bmAll = new BlockPosQueryAnd();
        
        Matcher m = nextTokenRegex.matcher(spec);
        while (spec.length() > 0)
        {
            
            m = nextTokenRegex.matcher(spec);
            if (!m.find())
            {
                throw new BlockQueryParseException("Syntax error in "+spec);
            }
            String token = m.group(0);
            spec = spec.substring(token.length());
            
            boolean negated = false;
            if (token.charAt(0) == '!')
            {
                negated = true;
                token = token.substring(1);
            }
            
            if (token.charAt(0) == '%')
            {
                bmAll.add( BlockQueryClass.of(token.substring(1), negated, false) );
            }
            else if (token.charAt(0) == '$')
            {
                bmAll.add( BlockQueryClass.of(token.substring(1), negated, true) );
            }
            else if (token.charAt(0) == '~')
            {
                bmAll.add( BlockQueryMaterial.of(token.substring(1), negated) );
            }
            else if (token.charAt(0)=='[')
            {
                String[] subtokens = commaDelimitRegex.split(token.substring(1, token.length() - 1));
                for (String subtoken : subtokens)
                {
                    bmAll.add( BlockQueryProperty.of(subtoken, negated) );
                }
            }
            else if (token.charAt(0) == '@')
            {
                IBlockPosQuery bm = predefined.get(token.substring(1));
                if (bm == null)
                {
                    throw new BlockQueryParseException("No predefined query named " + token.substring(1));
                }
                bmAll.add( negated ? new BlockPosQueryNot(bm) : bm );
            }
            else
            {
                bmAll.add( BlockQueryBlock.of(token, negated) );
            }
        }
        
        return bmAll.instance();
        
    } 
    
    
}