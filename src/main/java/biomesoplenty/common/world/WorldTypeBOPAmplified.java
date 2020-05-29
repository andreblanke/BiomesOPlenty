package biomesoplenty.common.world;

public class WorldTypeBOPAmplified extends WorldTypeBOP
{
    public WorldTypeBOPAmplified()
    {
        /* 16 character length restriction prevents us from using 'BIOMESOPAMPLIFIED' here. */
        super("BIOMESOPAMPL");
    }

    @Override
    public boolean showWorldInfoNotice()
    {
        return true;
    }
}
