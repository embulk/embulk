package org.quickload.record;

import org.junit.Ignore;

import java.util.Map;
import java.util.Random;

@Ignore
public class RandomTestUtils {
    protected long randomSeed;
    protected Random random;

    public RandomTestUtils()
    {
        this(getDefaultSeed());
    }

    public RandomTestUtils(long randomSeed)
    {
        this.randomSeed = randomSeed;
        this.random = new Random(randomSeed);
        System.out.println(" Random seed: 0x"+Long.toHexString(randomSeed)+"L");
    }

    public long getRandomSeed()
    {
        return randomSeed;
    }

    public void setRandomSeed(long seed)
    {
        random.setSeed(seed);
        this.randomSeed = seed;
        System.out.println(" Set random seed: 0x"+Long.toHexString(randomSeed)+"L");
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
        }

        return new Random().nextLong();
    }
}
