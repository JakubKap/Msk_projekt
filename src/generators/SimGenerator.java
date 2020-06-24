package generators;

import java.util.Random;

public class SimGenerator extends Random {// extends Ranlux

	/**
	 * 
	 */
	private static final long serialVersionUID = -4367175046229354038L;
	static double[] cof = { 76.18009173, -86.50532033, 24.01409822,
			-1.231739516, 0.120858003e-2, -0.536382e-5 };
	public static final double PI = 3.14159265358979323846;

	public SimGenerator(long seed) {
		super(seed);
	}

	public SimGenerator() {
		super();
	}

	public double raw() {
		return nextDouble();
	}

	// ======================================================================
	
	//Clock-based seed
	public static long generateSeed() {
		return RandomSeedable.ClockSeed();
	}

	public double uniform(double a, double b) {
		if (b < a) {
			System.err.println("SimGenerator.uniform: give b>a");
			return (-1);
		}
		return (raw() * (b - a) + a);
	}

    //Returns an integer uniformly between 0 (inclusive) and N (exclusive).
    public int uniformInt(int N) {
        if (N <= 0) throw new IllegalArgumentException("Parameter N must be positive");
        return nextInt(N);
    }

    //Returns an integer uniformly in [a, b).
    public int uniformInt(int a, int b) {
        if (b <= a) throw new IllegalArgumentException("Invalid range");
        if ((long) b - a >= Integer.MAX_VALUE) throw new IllegalArgumentException("Invalid range");
        return a + uniformInt(b - a);
    }
    
	public double exponential(double a) {
		if (a < 0) {
			System.err.println("SimGenerator.exponential: a must be >0");
			return (-1);
		}
		return (a * (-Math.log((raw()))));
	}

	public double hyperExponential(double p, double a, double b) {
		boolean pom = true;
		if (probability(p) == false)
			pom = false;

		if ((a < 0) || (b < 0)) {
			System.err
					.println("SimGenerator.hyperExponential: a and b must be >0");
			return (-1);
		} else if ((p <= 0) || (p >= 1)) {
			System.err
					.println("SimGenerator.hyperExponential: give p from range (0,1)");
			return (-1);
		} else if (pom)
			return (a * (-Math.log(raw())));
		else
			return (b * (-Math.log(raw())));
	}

	public double laplace(double a) {
		boolean pom = true;
		if (probability(0.5) == false)
			pom = false;

		if (pom)
			return (a * (-Math.log(raw())));
		else
			return (a * (Math.log(raw())));
	}

	public double erlang(int n, double a) {
		if (a < 0) {
			System.err.println("SimGenerator.laplace: give a>0");
			return (-1);
		} else {
			// use gamma distribution
			return (gamma((double) n, a));
		}
	}

	public double gamma(double a, double b) {
		// rejection method only for a > 1
		double xx, avg, am, e, s, v1, v2, yy;
		if ((a < 0.0) || (b < 0.0))
			System.err.println("SimGenerator.gamma: a and b be >0 and a<=1");

		if (a < 1.0) {
			do {
				xx = Math.pow(raw(), 1.0 / a);
				yy = Math.pow(raw(), 1.0 / (1.0 - a));
			} while (xx + yy > 1.0);

			xx = xx / (xx + yy);
			yy = exponential(1);
			return xx * yy / b;
		}

		if (a == 1.0)
			return (exponential(1) / b);

		// rejection method
		do {
			do {
				do {
					v1 = 2.0 * raw() - 1.0;
					v2 = 2.0 * raw() - 1.0;
				} while (v1 * v1 + v2 * v2 > 1.0);

				yy = v2 / v1;
				am = a - 1.0;
				s = Math.sqrt(2.0 * am + 1.0);
				avg = s * yy + am;
			} while (avg <= 0.0);
			e = (1.0 + yy * yy) * Math.exp(am * Math.log(avg / am) - s * yy);
		} while (raw() > e);

		return avg / b;
	}

	// ====================================================================================

	public double normal(double a, double b) {
		double x1;
		if (b <= 0.0) {
			System.err.println("SimGenerator.normal: b must be >0");
			return -1;
		}
		/*
		 * do { u = raw(); v1 = 2.0 * u - 1.0; u = raw(); v2 = 2.0 * raw() -
		 * 1.0; s = v1 * v1 + v2 * v2; } while (s >= 1.0 || s == 0.0);
		 * 
		 * x1 = v1 * Math.sqrt((-2.0 * Math.log(s)) / s);
		 */
		x1 = nextGaussian();
		return (a + x1 * b);
	};

	// return a random variable from a chi square distribution with
	// n degrees of freedom
	public double chisquare(int n) {
		return gamma(0.5 * n, 0.5);
	}

	// return random variable from a beta distribution

	public double beta(double a, double b) {
		if (b == 0.0) {
			System.err.println("SimGenerator.beta: b cannot be equal 0");
			return -1;
		}
		double zz = gamma(a, 1.0);
		return (zz / (zz + gamma(b, 1.0)));
	}

	// return it from the student distribution

	public double student(int n) {
		return (normal(0.0, 1.0) / Math.sqrt(chisquare(n) / n));
	}

	// return a random number from a lognormal distribution

	public double lognormal(double a, double b) {
		if (b <= 0.0) {
			System.err.println("SimGenerator:lognormal: b must be >0 ");
			return -1;
		}
		return Math.exp(normal(a, b));
	}

	// return it from a f distribution.

	public double fdistribution(int n, int m) {
		return ((m * chisquare(n)) / (n * chisquare(m)));
	}

	// random number from a weibull distribution

	public double weibull(double a, double b) {
		return Math.pow(exponential(a), (1.0 / b));
	}

	// get it from a poisson distribution with the given mean

	public double poisson(double a) {
		// can we use the direct method
		double sq = -1.0;
		double alxm = -1.0;
		double g = -1.0;
		double oldm = -1.0;
		double em, t, yy;
		if (a < 12.0) {
			if (a != oldm) {
				oldm = a;
				g = Math.exp(-a);
			}
			em = -1.0;
			t = 1.0;
			do {
				em += 1.0;
				t *= raw();
			} while (t > g);
		}

		// use the rejection method
		else {
			if (a != oldm) {
				oldm = a;
				sq = Math.sqrt(2.0 * a);
				alxm = Math.log(a);
				g = a * alxm - lngamma(a + 1.0);
			}
			do {
				do {
					// y is a deviate from a Lorentzian comparison function
					yy = Math.tan(PI * raw());
					em = sq * yy + a;
				} while (em < 0.0);
				em = Math.floor(em);
				t = 0.9 * (1.0 + yy * yy)
						* Math.exp(em * alxm - lngamma(em + 1.0) - g);
			} while (raw() > t);
		}
		return em;
	}

	// return a variate from the geometric distribution with event
	// probability p

	public double geometric(double p) {
		if ((p <= 0.0) || (p >= 1.0)) {
			System.err
					.println("SimGenerator.geometric: p must be from, range (0,1)");
			return -1;
		}
		return (Math.ceil(Math.log(raw()) / Math.log(1.0 - p)));
	}

	// return a variate from the hypergeometric distribution with m the
	// population, p the chance on success and n the number of items drawn

	public double hypergeometric(int m, int n, double p) {
		if ((p <= 0.0) || (p >= 1.0)) {
			System.err
					.println("SimGenerator.hypergeometric: p must be from, range (0,1)");
			return -1;
		} else if (m < n) {
			System.err
					.println("SimGenerator.hypergeometric: n must lower than m");
			return -1;
		}
		double g = p * m; // the success items
		double c = m - g; // the non-success items
		int k = 0;
		for (int i = 0; i < n; i++) // n drawings
		{
			if (raw() <= g / m) // if a success
			{
				k++; // success
				g--; // decrease right items
			} else
				c--; // decrease wrong items
			m--; // decrease total
		}
		return k; // return deviate
	}

	// get it from the binomial distribution with event probability p
	// and n the number of trials

	public double binomial(double p, int n) {
		int j;
		int nold = -1;
		double am, em, g, angle, prob, bnl, sq, t, yy;
		double pold = -1.0;
		double pc = 0;
		double en = 0;
		double oldg = 0;
		double plog = 0;
		double pclog = 0;

		if ((p <= 0.0) || (p >= 1.0)) {
			System.err
					.println("SimGenerator.binomial: p must be from range (0,1)");
			return -1;
		}

		if (p <= 0.5)
			prob = p;
		else
			prob = 1.0 - p;
		am = n * prob;

		if (n < 25) {
			bnl = 0.0;
			for (j = 1; j <= n; j++)
				if (raw() < prob)
					bnl += 1.0;
		} else if (am < 10) {
			g = Math.exp(-am);
			t = 1.0;
			for (j = 0; j <= n; j++) {
				t = t * raw();
				if (t < g)
					break;
			}
			if (j <= n)
				bnl = j;
			else
				bnl = n;
		} else {
			if (n != nold) {
				en = n;
				oldg = lngamma(en + 1.0);
				nold = n;
			}
			if (prob != pold) {
				pc = 1.0 - prob;
				plog = Math.log(prob);
				pclog = Math.log(pc);
				pold = prob;
			}
			sq = Math.sqrt(2.0 * am * pc);
			do {
				do {
					angle = PI * raw();
					yy = Math.tan(angle);
					em = sq * yy + am;

				} while (em < 0.0 || em >= (en + 1.0));
				em = Math.floor(em);
				t = 1.2
						* sq
						* (1.0 + yy * yy)
						* Math.exp(oldg - lngamma(em + 1.0)
								- lngamma(en - em + 1.0) + em * plog
								+ (en - em) * pclog);
			} while (raw() > t);
			bnl = em;
		}
		if (prob != p)
			bnl = n - bnl;
		return bnl;
	}

	// return a random variabele with probabilty of success equal to p
	// and n as the number of successes

	public double negativebinomial(double p, int n) {
		if ((p <= 0.0) || (p >= 1.0))
			System.err
					.println("SimGenerator.negativebinomial: give p from range (0,1)");
		return (poisson(gamma(n, p / (1.0 - p))));
	}

	// return a random variabele drawn from the triangular distribution

	public double triangular(double a) {
		if ((a < 0.0) || (a > 1.0))
			System.err
					.println("SimGenerator:triangular: give a from range (0,1)");
		double xx = raw();
		double yy = raw();
		if (xx > a)
			return (1.0 - ((1.0 - a) * Math.sqrt(yy)));
		else
			return (Math.sqrt(yy) * a);
	}

	// return OK with probability p and FALSE with probability 1-p

	public boolean probability(double p) {
		boolean wynik = false;
		if ((p < 0.0) || (p > 1.0)) {
			System.err
					.println("SimGenerator.propability: p must be from range (0,1)");
			return false;
		}
		if (p >= raw())
			wynik = true;
		return wynik;
	}

	// ===============================================================
	private static double lngamma(double xx) {
		double x = xx - 1.0;
		double tmp = x + 5.5;
		tmp -= (x + 0.5) * Math.log(tmp);
		double ser = 1.0;
		for (int j = 0; j < 6; j++) {
			x += 1.0;
			ser += cof[j] / x;
		}
		return (-tmp + Math.log(2.50662827465 * ser));
	}

}
