package org.embulk;

import java.util.Map;
import java.util.Random;

public class RandomManager
{
    protected long seed;
    protected Random random;

    public RandomManager()
    {
        this(getDefaultSeed());
    }

    public RandomManager(long seed)
    {
        this.seed = seed;
        this.random = new Random(seed);
        System.out.println(" Random seed: 0x"+Long.toHexString(seed)+"L");
    }

    public long getRandomSeed()
    {
        return seed;
    }

    public void setRandomSeed(long seed)
    {
        random.setSeed(seed);
        this.seed = seed;
        System.out.println(" Set random seed: 0x"+Long.toHexString(this.seed)+"L");
    }

    public Random getRandom()
    {
        return random;
    }

    private static long getDefaultSeed() {
        Map<String, String> env = System.getenv();
        String s = env.get("RANDOM_SEED");
        try {
            if(s != null) {
                return Long.parseLong(s);
            }
        } catch (NumberFormatException e) {
            System.out.println("RANDOM_SEED variable is wrong: "+e);
        }

        return new Random().nextLong();
    }
}
