package kz.hapyl.spigotutils.module.chat.gradient;

@FunctionalInterface
public interface Interpolator {

	double[] interpolate(double from, double to, int max);

}
