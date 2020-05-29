package biomesoplenty.common.transformer;

import java.util.Map;

import cpw.mods.fml.relauncher.IFMLLoadingPlugin;

@IFMLLoadingPlugin.MCVersion("1.7.10")
@IFMLLoadingPlugin.TransformerExclusions("biomesoplenty.common.transformer")
public final class BiomesOPlentyCore implements IFMLLoadingPlugin
{
    private static boolean obfuscated;

    // <editor-fold desc="IFMLLoadingPlugin">
    @Override
    public String[] getASMTransformerClass()
    {
        return new String[] { ChunkProviderGenerateClassTransformer.class.getName() };
    }

    @Override
    public String getModContainerClass()
    {
        return null;
    }

    @Override
    public String getSetupClass()
    {
        return null;
    }

    @Override
    public void injectData(Map<String, Object> data)
    {
        obfuscated = (Boolean) data.get("runtimeDeobfuscationEnabled");
    }

    @Override
    public String getAccessTransformerClass()
    {
        return null;
    }
    // </editor-fold>

    static boolean isObfuscated()
    {
        return obfuscated;
    }
}
