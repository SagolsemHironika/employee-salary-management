package com.acme.salary.seed;

import java.util.List;
import java.util.Random;
import java.util.function.ToDoubleFunction;

final class WeightedPicker {

    private WeightedPicker() {}

    static <T> T pick(List<T> options, ToDoubleFunction<T> weightOf, Random random) {
        double totalWeight = options.stream().mapToDouble(weightOf).sum();
        double target = random.nextDouble() * totalWeight;
        double cumulative = 0;
        for (T option : options) {
            cumulative += weightOf.applyAsDouble(option);
            if (target <= cumulative) {
                return option;
            }
        }
        return options.get(options.size() - 1);
    }
}
