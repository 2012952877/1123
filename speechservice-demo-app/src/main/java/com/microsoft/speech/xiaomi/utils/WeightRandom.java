package com.microsoft.speech.xiaomi.utils;

import com.microsoft.speech.xiaomi.config.AppConfig;
import com.microsoft.speech.xiaomi.entity.CandidateEndpoints;
import com.microsoft.speech.xiaomi.entity.SpeechEndpointBean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

//按权重计算
public class WeightRandom<T> {
    private static final Logger logger = LoggerFactory.getLogger(WeightRandom.class);

    private final List<T> items = new ArrayList<>();
    private double[] weights;

    public WeightRandom(List<WeightItem<T>> weightItems) {
        computeWeights(weightItems);
    }

    /**
     * 计算权重，初始化或者重新定义权重时使用
     *
     */
    public void computeWeights(List<WeightItem<T>> weightItems) {
        items.clear();

        // 计算权重总和
        double originWeightSum = 0;
        for (WeightItem<T> weightItem : weightItems) {
            double weight = weightItem.getWeight();
            if (weight <= 0) {
                continue;
            }

            items.add(weightItem.getItem());
            if (Double.isInfinite(weight)) {
                weight = 10000.0D;
            } else if (Double.isNaN(weight)) {
                weight = 1.0D;
            }
            originWeightSum += weight;
        }

        // 计算每个item的实际权重比例
        double[] actualWeightRatios = new double[weightItems.size()];
        int index = 0;
        for (WeightItem<T> weightItem : weightItems) {
            double weight = weightItem.getWeight();
            if (weight <= 0) {
                continue;
            }
            actualWeightRatios[index] = weight / originWeightSum;

            //logger.info("#index={}. wights={}; ", index, actualWeightRatios[index]);
            index++;
        }

        // 计算每个item的权重范围
        // 权重范围起始位置
        weights = new double[items.size()];
        double weightRangeStartPos = 0;
        for (int i = 0; i < index; i++) {
            weights[i] = weightRangeStartPos + actualWeightRatios[i];
            weightRangeStartPos = weights[i];
            //logger.info("#{}. wights={}; pos={}", i, weights[i], weightRangeStartPos);
        }
    }

    /**
     * 基于权重随机算法选择, 返回对应选中的item key
     *
     */
    public T choose() {
        double random = ThreadLocalRandom.current().nextDouble();
        //binarySearch前提条件是要保证数组的顺序是从小到大排序过的
        int index = Arrays.binarySearch(weights, random);

        //logger.info("#index={}. random={}; ", index, random);
        if (index < 0) {
            index = -index - 1;
        } else {
            return items.get(index);
        }

        if (index < weights.length && random < weights[index]) {
            return items.get(index);
        }

        return items.get(0);
    }

    public static class WeightItem<T> {
        T item;        //item
        double weight; //权重值

        public WeightItem() {
        }

        public WeightItem(T item, double weight) {
            this.item = item;
            this.weight = weight;
        }

        public T getItem() {
            return item;
        }

        public void setItem(T item) {
            this.item = item;
        }

        public double getWeight() {
            return weight;
        }

        public void setWeight(double weight) {
            this.weight = weight;
        }
    }

    public static void main(String[] args) {
        testCase2();
    }

    public static void testCase2() {
        int sampleCount = 10000;//100000;

        AppConfig appConfig = new AppConfig();
        CandidateEndpoints candidateEndpoints = appConfig.getCandidateEndpoints();
        candidateEndpoints.printInfo();
        List<WeightItem<String>> itemList = new ArrayList<>();
        for(SpeechEndpointBean bean : candidateEndpoints.getCandidateEndpointList()) {
            itemList.add( new WeightItem<>(bean.getName(), bean.getWeight() ));
        }

        WeightRandom<String> weightRandom = new WeightRandom<>(itemList);

        // 统计
        Map<String, AtomicInteger> statistics = new HashMap<>();

        for (int i = 0; i < sampleCount; i++) {
            statistics.computeIfAbsent(weightRandom.choose(), (k) -> new AtomicInteger())
                    .incrementAndGet();
        }

        statistics.forEach((k, v) -> {
            double hit = (double) v.get() / sampleCount;
            System.out.println(k + ", hit:" + hit);
        });
    }

    public static void testCase1() {
        // for test
        int sampleCount = 10000;//100000;

        WeightItem<String> server1 = new WeightItem<>("server1", 1.0);
        WeightItem<String> server2 = new WeightItem<>("server2", 3.0);
        WeightItem<String> server3 = new WeightItem<>("server3", 2.0);

        WeightRandom<String> weightRandom = new WeightRandom<>(Arrays.asList(server1, server2, server3));
        // 统计
        Map<String, AtomicInteger> statistics = new HashMap<>();

        for (int i = 0; i < sampleCount; i++) {
            statistics.computeIfAbsent(weightRandom.choose(), (k) -> new AtomicInteger())
                    .incrementAndGet();
        }

        statistics.forEach((k, v) -> {
            double hit = (double) v.get() / sampleCount;
            System.out.println(k + ", hit:" + hit);
        });
    }
}
