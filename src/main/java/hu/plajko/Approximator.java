package hu.plajko;

public class Approximator {

	public static interface Function {
		public double calculate(double param0, double param1, double param2);
	}

	public Approximator(Function function, Double targetValue) {
		this.function = function;
		this.targetValue = targetValue;
	}

	private static final double ERR = 0.0001d;
	private Function function = null;
	private Double targetValue = null;

	public double findSolution(Double param0, Double param1, Double param2) {
		if (param0 == null && param1 != null && param2 != null)
			return findSolution(0.0d, param1, param2, 0);
		else if (param0 != null && param1 == null && param2 != null)
			return findSolution(param0, 0.0d, param2, 1);
		else if (param0 != null && param1 != null && param2 == null)
			return findSolution(param0, param1, 0.0d, 2);
		throw new IllegalArgumentException("exactly one parameter must be null");
	}

	private double findSolution(double param0, double param1, double param2, int solutionIndex) {
		double add = 1.0d;
		Double prevDelta = null;
		while (true) {
			if (prevDelta != null) {
				switch (solutionIndex) {
				default:
				case 0:
					param0 += add;
					break;
				case 1:
					param1 += add;
					break;
				case 2:
					param2 += add;
					break;
				}
			}
			double currentValue = function.calculate(param0, param1, param2);
			double delta = Math.abs(currentValue - this.targetValue);

			if (prevDelta != null && prevDelta < delta)
				add *= -0.1d;
			else
				add *= 1.1;

			if (prevDelta != null && prevDelta.equals(delta)) {
				break;
			}

			prevDelta = delta;
		}
		if (prevDelta > ERR)
			return Double.NaN;
		switch (solutionIndex) {
		default:
		case 0:
			return param0;
		case 1:
			return param1;
		case 2:
			return param2;
		}
	}

}