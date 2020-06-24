package generators;

import java.util.Date;

abstract class RandomSeedable
{

    /**
     * 
     * Return a long integer seed given a date
     * 
     * @param d
     *            a date
     * @return a long integer seed
     *  
     */
    public static long ClockSeed(Date d) {
		return d.getTime();
	}

    /**
	 * 
	 * Return a long integer seed calculated from the date. Equivalent to <CODE>
     * ClockSeed(new Date());
     * 
     * @return a long integer seed
     *
	 */

    public static long ClockSeed() {
		return ClockSeed(new Date());
	}
}
